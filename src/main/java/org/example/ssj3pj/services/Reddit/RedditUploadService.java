package org.example.ssj3pj.services.Reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditUploadService {

    private final RedditTokenGuard tokenGuard;

    @Qualifier("redditRestTemplate") // ✅ config에서 만든 redditRestTemplate bean 사용
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reddit에 미디어 업로드 (이미지/비디오)
     * @param userId   Reddit 토큰 관리용 유저 ID
     * @param subreddit 게시할 서브레딧
     * @param title    게시글 제목
     * @param mediaFile 업로드할 파일 (이미지 or 비디오)
     * @param kind     "image" 또는 "video"
     * @return Reddit postId
     */
    public String uploadMediaPost(Long userId, String subreddit, String title, File mediaFile, String kind) {
        if (!"image".equalsIgnoreCase(kind) && !"video".equalsIgnoreCase(kind)) {
            throw new IllegalArgumentException("지원하지 않는 kind 값: " + kind);
        }

        String accessToken = tokenGuard.getValidAccessToken(userId);

        // 1. 파일 업로드 (asset.json)
        String assetId = uploadAsset(accessToken, mediaFile);

        // 2. 포스트 등록 (submit)
        String response = submitPost(accessToken, subreddit, title, assetId, kind);

        // 3. 응답에서 postId 추출
        return extractPostIdFromResponse(response);
    }

    /** Reddit asset 업로드 */
    private String uploadAsset(String accessToken, File mediaFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(mediaFile));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/media/asset.json",
                request,
                Map.class
        );

        log.info("Reddit asset upload response={}", response);

        return (String) ((Map<?, ?>) response.get("args")).get("asset_id");
    }

    /** Reddit submit API 호출 */
    private String submitPost(String accessToken, String subreddit, String title, String assetId, String kind) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("sr", subreddit);
        body.add("kind", kind); // image or video
        body.add("title", title);
        body.add("url", "https://preview.redd.it/" + assetId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/submit",
                request,
                String.class
        );

        log.info("Reddit submit response={}", response);
        return response;
    }

    /** Reddit API 응답에서 postId 추출 */
    private String extractPostIdFromResponse(String redditResponse) {
        try {
            JsonNode jsonNode = objectMapper.readTree(redditResponse);
            String postId = jsonNode.path("json").path("data").path("id").asText();

            if (postId == null || postId.isEmpty()) {
                throw new RuntimeException("Reddit 응답에서 postId를 찾을 수 없습니다: " + redditResponse);
            }

            return postId;
        } catch (Exception e) {
            throw new RuntimeException("Reddit 응답 파싱 실패", e);
        }
    }
}
