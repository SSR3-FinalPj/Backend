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
import java.util.Collections;
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
    public String uploadMediaPost(Long userId, String subreddit, String title,
                                  File mediaFile, String kind, String posterUrl) {
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
            response = submitPost(accessToken, subreddit, title, "image", redditUsername, assetUrl, null);
        } else {
            String assetId = uploadVideo(accessToken, mediaFile, redditUsername);

            // ✅ Reddit 백엔드가 asset 준비할 시간을 조금 줌
            try {
                Thread.sleep(500_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // kind=link 로 제출
            response = submitPost(accessToken, subreddit, title, "link", redditUsername, assetId, posterUrl);
        }

        return extractPostIdFromResponse(response);
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

        java.util.List<Map<String, String>> fields =
                (java.util.List<Map<String, String>>) args.get("fields");

        try {
            uploadToS3(uploadUrl, videoFile, fields);
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 중 오류 발생", e);
        }

        log.info("업로드된 비디오 assetId: {}", assetId);
        return assetId;
    }

    /** ✅ Presigned URL로 업로드 */
    private void uploadToS3(String uploadUrl, File videoFile, java.util.List<Map<String, String>> fields) throws IOException {
        URL url = new URL(uploadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String boundary = "----formdata-" + System.currentTimeMillis();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"));

        for (Map<String, String> field : fields) {
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"").append(field.get("name")).append("\"").append("\r\n");
            writer.append("\r\n").append(field.get("value")).append("\r\n");
        }

        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(videoFile.getName()).append("\"").append("\r\n");
        writer.append("Content-Type: video/mp4\r\n\r\n");
        writer.flush();

        baos.write(Files.readAllBytes(videoFile.toPath()));
        writer.append("\r\n--").append(boundary).append("--\r\n");
        writer.close();

        byte[] postData = baos.toByteArray();
        connection.setFixedLengthStreamingMode(postData.length);
        connection.connect();

        try (OutputStream out = connection.getOutputStream()) {
            out.write(postData);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 201 && (responseCode < 200 || responseCode >= 300)) {
            throw new RuntimeException("S3 업로드 실패: HTTP " + responseCode);
        }

        log.info("S3 업로드 성공: HTTP {}", responseCode);
    }

    /** ✅ Submit */
    private String submitPost(String accessToken,
                              String subreddit,
                              String title,
                              String kind,  // ← 이제 비디오는 link로 호출
                              String redditUsername,
                              String assetOrUrl,
                              String posterUrl) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("sr", subreddit);
        body.add("kind", kind);
        body.add("title", title);
        body.add("api_type", "json");
        body.add("raw_json", "1");

        if ("link".equalsIgnoreCase(kind)) { // ✅ 비디오 업로드는 kind=link
            body.add("media_asset_id", assetOrUrl);
            body.add("url", "https://v.redd.it/" + assetOrUrl);
            if (posterUrl != null && !posterUrl.isBlank()) {
                body.add("video_poster_url", posterUrl);
            }
        } else {
            body.add("url", assetOrUrl);
        }

        // ✅ 최종 body 로그 출력
        log.info("📤 Reddit submit body={}", body);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/submit",
                request,
                String.class
        );

        log.info("✅ Reddit submit response={}", response);
        return response;
    }

    /** ✅ postId 추출 */
    private String extractPostIdFromResponse(String redditResponse) {
        try {
            JsonNode root = objectMapper.readTree(redditResponse);
            JsonNode data = root.path("json").path("data");
            String postId = data.path("id").asText(null);
            if (postId != null && !postId.isEmpty()) return postId;

            String name = data.path("name").asText(null);
            if (name != null && name.startsWith("t3_")) return name.substring(3);

            throw new RuntimeException("Reddit 응답에서 postId 추출 실패: " + redditResponse);
        } catch (Exception e) {
            throw new RuntimeException("Reddit 응답 파싱 실패", e);
        }
    }
}
