package org.example.ssj3pj.services.Reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.User.RedditToken;
import org.example.ssj3pj.repository.RedditTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditUploadService {

    private final RedditTokenGuard tokenGuard;
    private final RedditTokenRepository redditTokenRepository;

    @Qualifier("redditRestTemplate")
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reddit에 미디어 업로드 (이미지 or 비디오)
     */
    public String uploadMediaPost(Long userId, String subreddit, String title, File mediaFile, String kind, String thumbnailUrl) {
        if (!"image".equalsIgnoreCase(kind) && !"video".equalsIgnoreCase(kind)) {
            throw new IllegalArgumentException("지원하지 않는 kind 값: " + kind);
        }

        String accessToken = tokenGuard.getValidAccessToken(userId);

        String redditUsername = redditTokenRepository.findByUserId(userId)
                .map(RedditToken::getRedditUsername)
                .orElseThrow(() -> new IllegalStateException("해당 사용자 Reddit 계정 연동 정보가 없습니다."));

        String response;
        if ("image".equalsIgnoreCase(kind)) {
            String assetUrl = uploadImage(accessToken, mediaFile, redditUsername);
            response = submitPost(accessToken, subreddit, title, "image", redditUsername, assetUrl, null, null);
        } else {
            String videoUrl = uploadVideo(accessToken, mediaFile, redditUsername);
            response = submitPost(accessToken, subreddit, title, "video", redditUsername, videoUrl, null, thumbnailUrl);
        }
        // 우선 일반 경로로 postId 추출 시도
        try {
            return extractPostIdFromResponse(response);
        } catch (RuntimeException ex) {
            // 비디오 게시물의 경우 Reddit이 즉시 id를 주지 않을 수 있어 보조 탐색 시도
            if ("video".equalsIgnoreCase(kind)) {
                try {
                    String found = pollRecentSubmittedPostId(accessToken, redditUsername, subreddit, title, 12, 1500);
                    if (found != null && !found.isEmpty()) return found;
                } catch (Exception ignore) { }
            }
            throw ex;
        }
    }

    /** ✅ 이미지 업로드 */
    private String uploadImage(String accessToken, File mediaFile, String redditUsername) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(mediaFile));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/media/asset.json",
                request,
                Map.class
        );

        log.info("Reddit image asset upload response={}", response);

        Map<String, Object> args = (Map<String, Object>) response.get("args");
        return (String) args.get("asset_url"); // ✅ image는 url
    }

    /** ✅ 비디오 업로드 */
    private String uploadVideo(String accessToken, File videoFile, String redditUsername) {
        // 1. Reddit 업로드 세션 생성
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("filepath", videoFile.getName());
        body.add("mimetype", "video/mp4");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/media/asset.json",
                request,
                Map.class
        );

        log.info("Reddit video asset init response={}", response);

        Map<String, Object> args = (Map<String, Object>) response.get("args");
        Map<String, Object> asset = (Map<String, Object>) response.get("asset");

        String assetId = (String) asset.get("asset_id");
        String uploadUrl = (String) args.get("action");

        if (uploadUrl.startsWith("//")) {
            uploadUrl = "https:" + uploadUrl;
        }

        // S3 key 찾기 (fields에서 key 값 추출)
        String s3Key = null;
        java.util.List<Map<String, String>> fields = (java.util.List<Map<String, String>>) args.get("fields");
        for (Map<String, String> field : fields) {
            if ("key".equals(field.get("name"))) {
                s3Key = field.get("value");
                break;
            }
        }

        // 2. Presigned URL로 S3 업로드 (POST) - HttpURLConnection 사용
        try {
            uploadToS3WithHttpConnection(uploadUrl, videoFile, fields);
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 중 오류 발생", e);
        }

        // 3. 제출에 사용할 v.redd.it URL로 변환 (레딧이 호스팅 비디오로 인식)
        String videoUrl = "https://v.redd.it/" + assetId;
        log.info("업로드된 비디오 URL(v.redd.it): {}", videoUrl);
        return videoUrl;
    }

    /** ✅ HttpURLConnection을 사용한 S3 업로드 */
    private void uploadToS3WithHttpConnection(String uploadUrl, File videoFile, java.util.List<Map<String, String>> fields) throws IOException {
        URL url = new URL(uploadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String boundary = "----formdata-" + System.currentTimeMillis();

        // POST 연결 설정
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        // multipart form data 생성
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"));

        // 모든 form fields 추가
        for (Map<String, String> field : fields) {
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"").append(field.get("name")).append("\"").append("\r\n");
            writer.append("\r\n");
            writer.append(field.get("value")).append("\r\n");
        }

        // 파일 field 추가
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(videoFile.getName()).append("\"").append("\r\n");
        writer.append("Content-Type: video/mp4").append("\r\n");
        writer.append("\r\n");
        writer.flush();

        // 파일 내용 추가
        baos.write(Files.readAllBytes(videoFile.toPath()));

        writer = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"));
        writer.append("\r\n");
        writer.append("--").append(boundary).append("--").append("\r\n");
        writer.flush();
        writer.close();

        byte[] postData = baos.toByteArray();

        // Content-Length 설정 (사용자 제안 방식)
        connection.setFixedLengthStreamingMode(postData.length);
        connection.connect();

        // 데이터 전송
        try (OutputStream out = connection.getOutputStream()) {
            out.write(postData);
            out.flush();
        }

        // 응답 확인
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            String errorResponse = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                errorResponse = sb.toString();
            } catch (Exception e) {
                // ignore
            }
            throw new RuntimeException("S3 업로드 실패: HTTP " + responseCode + " - " + errorResponse);
        }

        log.info("S3 업로드 성공: HTTP {}", responseCode);
    }

    /** ✅ Submit API 호출 */
    private String submitPost(String accessToken,
                              String subreddit,
                              String title,
                              String kind,
                              String redditUsername,
                              String assetUrl,   // 이미지 또는 비디오 URL
                              String mediaId,    // 사용 안함
                              String thumbnailUrl) { // 비디오 썸네일
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");
        // JSON 응답을 강제하도록 Accept 지정
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("sr", subreddit);
        body.add("kind", kind);
        body.add("title", title);
        // Reddit 비디오 제출은 url 대신 video_url을 사용해야 안정적
        if ("video".equalsIgnoreCase(kind)) {
            body.add("video_url", assetUrl);
        } else {
            body.add("url", assetUrl);
        }
        // Reddit이 JSON을 반환하도록 강제
        body.add("api_type", "json");
        // JSON 이스케이프 문제 방지용 옵션(무해)
        body.add("raw_json", "1");

        // 비디오인 경우, 썸네일 URL 추가
        if ("video".equalsIgnoreCase(kind) && thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            body.add("video_poster_url", thumbnailUrl);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/submit",
                request,
                String.class
        );

        log.info("Reddit submit response={}", response);
        return response;
    }

    /** ✅ postId 추출 */
    private String extractPostIdFromResponse(String redditResponse) {
        // 1차: JSON 경로에서 추출
        try {
            JsonNode root = objectMapper.readTree(redditResponse);
            JsonNode data = root.path("json").path("data");
            String postId = data.path("id").asText(null);
            if (postId != null && !postId.isEmpty()) {
                return postId;
            }

            // 일부 경우 name: "t3_<id>" 만 내려올 수 있음
            String name = data.path("name").asText(null);
            if (name != null && name.startsWith("t3_")) {
                return name.substring(3);
            }

            // 혹시 redirect/permalink에 comments/<id>/ 형태가 있을 수 있음
            String redirect = data.path("redirect").asText(null);
            if (redirect != null) {
                String fromRedirect = extractIdFromCommentsUrl(redirect);
                if (fromRedirect != null) return fromRedirect;
            }
            String urlField = data.path("url").asText(null);
            if (urlField != null) {
                String fromUrl = extractIdFromCommentsUrl(urlField);
                if (fromUrl != null) return fromUrl;
            }

            // JSON 파싱은 됐지만 id를 못 찾음
            throw new RuntimeException("Reddit 응답에서 postId를 찾을 수 없습니다: " + truncate(redditResponse, 500));
        } catch (Exception jsonEx) {
            // 2차: 비JSON(jquery) 응답에 대한 폴백 정규식
            try {
                java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("\\bt3_([a-z0-9]+)\\b").matcher(redditResponse);
                if (m1.find()) return m1.group(1);

                java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("/comments/([a-z0-9]+)/").matcher(redditResponse);
                if (m2.find()) return m2.group(1);
            } catch (Exception ignore) { }
            throw new RuntimeException("Reddit 응답 파싱 실패", jsonEx);
        }
    }

    private String extractIdFromCommentsUrl(String url) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("/comments/([a-z0-9]+)/").matcher(url);
            if (m.find()) return m.group(1);
        } catch (Exception ignore) { }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * 비디오 업로드 직후 id가 응답에 없는 경우, 사용자의 최근 제출 목록에서 제목/서브레딧으로 역추적
     */
    private String findRecentSubmittedPostId(String accessToken, String redditUsername, String subreddit, String title) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = "https://oauth.reddit.com/user/" + redditUsername + "/submitted.json?limit=10";
        try {
            String body = restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody();
            if (body == null) return null;

            JsonNode root = objectMapper.readTree(body);
            JsonNode children = root.path("data").path("children");
            if (children.isArray()) {
                for (JsonNode child : children) {
                    JsonNode d = child.path("data");
                    String t = d.path("title").asText("");
                    String sr = d.path("subreddit").asText("");
                    boolean isVideo = d.path("is_video").asBoolean(false);
                    if (isVideo && title.equals(t) && subreddit.equalsIgnoreCase(sr)) {
                        String id = d.path("id").asText("");
                        if (!id.isEmpty()) return id;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("최근 제출 목록 조회 실패: {}", e.getMessage());
        }
        return null;
    }

    private String pollRecentSubmittedPostId(String accessToken, String redditUsername, String subreddit, String title, int attempts, long intervalMs) {
        for (int i = 0; i < attempts; i++) {
            String id = findRecentSubmittedPostId(accessToken, redditUsername, subreddit, title);
            if (id != null && !id.isEmpty()) return id;
            try { Thread.sleep(intervalMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        return null;
    }
}
