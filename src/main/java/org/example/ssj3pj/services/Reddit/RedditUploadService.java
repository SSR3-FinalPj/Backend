package org.example.ssj3pj.services.Reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.entity.User.RedditToken;
import org.example.ssj3pj.repository.UsersRepository;
import org.example.ssj3pj.repository.RedditTokenRepository;
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

    private final RedditTokenRepository redditTokenRepository;
    private final UsersRepository usersRepository;

    @Qualifier("redditRestTemplate")
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reddit에 미디어 업로드 (이미지/비디오)
     */
    public String uploadMediaPost(Long userId, String subreddit, String title, File mediaFile, String kind) {
        if (!"image".equalsIgnoreCase(kind) && !"video".equalsIgnoreCase(kind)) {
            throw new IllegalArgumentException("지원하지 않는 kind 값: " + kind);
        }

        String accessToken = tokenGuard.getValidAccessToken(userId);

        String redditUsername = redditTokenRepository.findByUserId(userId)
                .map(RedditToken::getRedditUsername)
                .orElseThrow(() -> new IllegalStateException("해당 사용자 Reddit 계정 연동 정보가 없습니다."));

        // 1. 파일 업로드 (asset.json)
        String assetUrl = uploadAsset(accessToken, mediaFile, redditUsername);

        // 2. 포스트 등록 (submit)
        String response = submitPost(accessToken, subreddit, title, assetUrl, kind, redditUsername);

        // 3. 응답에서 postId 추출
        return extractPostIdFromResponse(response);
    }

    /** Reddit asset 업로드 */
    private String uploadAsset(String accessToken, File mediaFile, String redditUsername) {
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

        log.info("Reddit asset upload response={}", response);

        Map<String, Object> args = (Map<String, Object>) response.get("args");
        return (String) args.get("asset_url");
    }

    /** Reddit submit API 호출 */
    private String submitPost(String accessToken, String subreddit, String title, String assetUrl, String kind, String redditUsername) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "ssj3pj:backend:1.0 (by /u/" + redditUsername + ")"); // ✅ 동적 User-Agent

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("sr", subreddit);
        body.add("kind", kind);
        body.add("title", title);
        body.add("url", assetUrl);

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
