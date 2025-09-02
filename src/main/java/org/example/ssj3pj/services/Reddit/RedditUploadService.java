package org.example.ssj3pj.services.Reddit;

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

    /**
     * Reddit에 이미지 업로드 + 포스트 등록
     * @param userId Reddit 토큰 관리용 유저 ID
     * @param subreddit 게시할 서브레딧
     * @param title 게시글 제목
     * @param imageFile 업로드할 이미지 파일
     * @return Reddit API 응답 JSON (문자열)
     */
    public String uploadImagePost(Long userId, String subreddit, String title, File imageFile) {
        String accessToken = tokenGuard.getValidAccessToken(userId);

        // 1. 이미지 업로드 (asset.json)
        String assetId = uploadImage(accessToken, imageFile);

        // 2. 포스트 등록 (submit)
        return submitPost(accessToken, subreddit, title, assetId);
    }

    private String uploadImage(String accessToken, File imageFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imageFile));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/media/asset.json",
                request,
                Map.class
        );

        log.info("Reddit image upload response={}", response);

        return (String) ((Map<?, ?>) response.get("args")).get("asset_id");
    }

    private String submitPost(String accessToken, String subreddit, String title, String assetId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("sr", subreddit);
        body.add("kind", "image");
        body.add("title", title);
        body.add("url", "https://preview.redd.it/" + assetId + ".png");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String response = restTemplate.postForObject(
                "https://oauth.reddit.com/api/submit",
                request,
                String.class
        );

        log.info("Reddit post submit response={}", response);
        return response;
    }
}
