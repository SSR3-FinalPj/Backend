package org.example.ssj3pj.services.Reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.User.RedditToken;
import org.example.ssj3pj.repository.RedditTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditUploadService {

    private final RedditTokenGuard tokenGuard;
    private final RedditTokenRepository redditTokenRepository;

    @Qualifier("redditRestTemplate")
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Client s3Client;

    /**
     * Python 코드 흐름 + postId 보장 (submitted.json 조회 fallback)
     */
    public String uploadMediaPost(Long userId,
                                  String subreddit,
                                  String title,
                                  String bucket,
                                  String key,
                                  String kind,
                                  String posterKey) {

        if (!"video".equalsIgnoreCase(kind)) {
            throw new IllegalArgumentException("현재 Python 흐름은 video 업로드 기준입니다.");
        }

        String accessToken = tokenGuard.getValidAccessToken(userId);

        String redditUsername = redditTokenRepository.findByUserId(userId)
                .map(RedditToken::getRedditUsername)
                .orElseThrow(() -> new IllegalStateException("해당 사용자 Reddit 계정 연동 정보가 없습니다."));

        // === 1) 업로드 lease 요청
        Map<String, Object> lease = obtainUploadLease(accessToken, key, "video/mp4", redditUsername);
        String uploadUrl = normalizeUrl((String) lease.get("action"));
        List<Map<String, String>> fields = (List<Map<String, String>>) lease.get("fields");
        log.info("key : {}", key);
        // === 2) Reddit presigned로 실제 업로드
        try (ResponseInputStream<?> s3obj = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key("video/" +key)
                .build())) {
            uploadToRedditS3(uploadUrl, s3obj, key, "video/mp4", fields);
        } catch (IOException e) {
            throw new RuntimeException("비디오 업로드 실패", e);
        }

        // === 3) video_url 만들기
        String fieldKey = fields.stream()
                .filter(f -> "key".equals(f.get("name")))
                .map(f -> f.get("value"))
                .findFirst()
                .orElseThrow();
        String videoUrl = joinUrl(uploadUrl, fieldKey);

        // === 4) 썸네일(옵션)
        String posterUrl = null;
        if (posterKey != null && !posterKey.isBlank()) {
            Map<String, Object> posterLease = obtainUploadLease(accessToken, posterKey, "image/jpeg", redditUsername);
            String pUploadUrl = normalizeUrl((String) posterLease.get("action"));
            List<Map<String, String>> pFields = (List<Map<String, String>>) posterLease.get("fields");

            try (ResponseInputStream<?> s3obj = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key("images/"+ posterKey)
                    .build())) {
                uploadToRedditS3(pUploadUrl, s3obj, posterKey, "image/jpeg", pFields);
            } catch (IOException e) {
                throw new RuntimeException("포스터 업로드 실패", e);
            }

            String posterFieldKey = pFields.stream()
                    .filter(f -> "key".equals(f.get("name")))
                    .map(f -> f.get("value"))
                    .findFirst()
                    .orElseThrow();
            posterUrl = joinUrl(pUploadUrl, posterFieldKey);
        }

        // === 5) submit 호출 (kind=video + url=video_url)
        String response = submitVideoPost(accessToken, subreddit, title, videoUrl, posterUrl, redditUsername);

        // === 6) postId 추출 (없으면 submitted.json 조회)
        return extractPostIdFromResponse(response, redditUsername, accessToken);
    }

    /** asset.json 요청 */
    private Map<String, Object> obtainUploadLease(String accessToken, String filename, String mimetype, String redditUsername) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("filepath", filename);
        body.add("mimetype", mimetype);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/media/asset.json",
                request,
                Map.class
        );

        log.info("Reddit lease response={}", response);
        Map<String, Object> args = (Map<String, Object>) response.get("args");

        Map<String, Object> result = new HashMap<>();
        result.put("action", args.get("action"));
        result.put("fields", args.get("fields"));
        return result;
    }

    /** S3 → Reddit 업로드 */
    private void uploadToRedditS3(String uploadUrl,
                                  InputStream s3obj,
                                  String key,
                                  String mimeType,
                                  List<Map<String, String>> fields) throws IOException {

        URL url = new URL(uploadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String boundary = "----redditS3" + UUID.randomUUID().toString().replace("-", "");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = connection.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {

            for (Map<String, String> field : fields) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"").append(field.get("name")).append("\"\r\n\r\n");
                writer.append(field.get("value")).append("\r\n");
            }

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(key).append("\"\r\n");
            writer.append("Content-Type: ").append(mimeType).append("\r\n\r\n");
            writer.flush();

            byte[] buffer = new byte[8192];
            int len;
            while ((len = s3obj.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();

            writer.append("\r\n--").append(boundary).append("--\r\n");
            writer.flush();
        }

        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("Reddit S3 업로드 실패: HTTP " + code);
        }
        log.info("✅ Reddit S3 업로드 성공: HTTP {}", code);
    }

    /** submit 호출 */
    private String submitVideoPost(String accessToken,
                                   String subreddit,
                                   String title,
                                   String videoUrl,
                                   String posterUrl,
                                   String redditUsername) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("api_type", "json");
        body.add("sr", subreddit);
        body.add("title", title);
        body.add("kind", "video");
        body.add("url", videoUrl);
        body.add("resubmit", "true");
        if (posterUrl != null) {
            body.add("video_poster_url", posterUrl);
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

    /** video_url 합성 */
    private String joinUrl(String base, String key) {
        if (!base.endsWith("/")) base = base + "/";
        return base + key;
    }

    private String normalizeUrl(String u) {
        if (u.startsWith("//")) return "https:" + u;
        return u;
    }

    /** postId 추출 (없으면 submitted.json fallback) */
    private String extractPostIdFromResponse(String redditResponse,
                                             String redditUsername,
                                             String accessToken) {
        try {
            JsonNode root = objectMapper.readTree(redditResponse);
            JsonNode data = root.path("json").path("data");

            // 1) id/name 바로 있으면 반환
            String postId = data.path("id").asText(null);
            if (postId != null && !postId.isEmpty()) return postId;

            String name = data.path("name").asText(null);
            if (name != null && name.startsWith("t3_")) return name.substring(3);

            // 2) 없으면 최신 글 조회
            String url = "https://oauth.reddit.com/user/" + redditUsername + "/submitted.json?limit=1";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode listing = objectMapper.readTree(resp.getBody());
            JsonNode children = listing.path("data").path("children");

            if (children.isArray() && children.size() > 0) {
                String fullName = children.get(0).path("data").path("name").asText(null); // t3_xxx
                if (fullName != null && fullName.startsWith("t3_")) {
                    return fullName.substring(3);
                }
            }

            throw new RuntimeException("postId 추출 실패 (submitted.json도 없음)");

        } catch (Exception e) {
            throw new RuntimeException("Reddit 응답 파싱 실패", e);
        }
    }
}
