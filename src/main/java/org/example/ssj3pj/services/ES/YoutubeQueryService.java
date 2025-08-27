package org.example.ssj3pj.services.ES;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.YoutubeSummaryDto;
import org.example.ssj3pj.dto.youtube.YoutubeCommentDto;
import org.example.ssj3pj.dto.youtube.YoutubeThumbnailDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import static org.example.ssj3pj.util.JsonNodeUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeQueryService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    private static final String INDEX = "youtubedata";  // YouTube 인덱스명

    public YoutubeSummaryDto getSummaryByDocId(String esDocId) {
        try {
            // 1. ES에서 문서 가져오기
            GetRequest request = new GetRequest.Builder()
                    .index(INDEX)
                    .id(esDocId)
                    .build();

            GetResponse<JsonData> response = elasticsearchClient.get(request, JsonData.class);
            if (!response.found()) {
                throw new RuntimeException("❌ ES 문서 없음 (youtube): " + esDocId);
            }

            // 2. JsonNode 변환
            JsonNode src = objectMapper.readTree(response.source().toJson().toString());

            // 3. tags 배열 처리
            String[] tags = null;
            JsonNode tagsNode = src.path("tags");
            if (tagsNode.isArray()) {
                tags = new String[tagsNode.size()];
                for (int i = 0; i < tagsNode.size(); i++) {
                    tags[i] = tagsNode.get(i).asText();
                }
            }

            // 4. 썸네일 처리
            YoutubeThumbnailDto thumbnails = parseThumbnails(src.path("thumbnail_url"));

            // 5. 댓글 처리
            List<YoutubeCommentDto> comments = parseComments(src.path("comments"));

            // 6. DTO 빌드
            YoutubeSummaryDto dto = YoutubeSummaryDto.builder()
                    .videoId(getText(src, "video_id"))
                    .title(getText(src, "title"))
                    .description(getText(src, "description"))
                    .channelTitle(getText(src, "channel_title"))
                    .channelId(getText(src, "channel_id"))
                    .uploadDate(getText(src, "upload_date"))
                    .viewCount(getIntFromString(src, "view_count"))
                    .likeCount(getIntFromString(src, "like_count"))
                    .commentCount(getIntFromString(src, "comment_count"))
                    .categoryId(getText(src, "category_id"))
                    .tags(tags)
                    .thumbnailUrl(thumbnails)
                    .videoPlayer(getText(src, "video_player"))
                    .comments(comments)
                    .build();

            if (log.isDebugEnabled()) {
                log.debug("📦 YouTube DTO READY: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dto));
            }
            return dto;

        } catch (Exception e) {
            throw new RuntimeException("❌ YouTube ES 조회 또는 파싱 실패: " + e.getMessage(), e);
        }
    }

    public String getRawSourceByDocId(String esDocId) {
        try {
            GetResponse<JsonData> response = elasticsearchClient.get(
                    new GetRequest.Builder().index(INDEX).id(esDocId).build(),
                    JsonData.class
            );
            if (!response.found()) throw new RuntimeException("❌ ES 문서 없음: " + esDocId);
            JsonNode root = objectMapper.readTree(response.source().toJson().toString());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("❌ YouTube ES 원본 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * ES 원본 데이터를 Object(배열/객체) 형태로 반환 (실행 중인 동작과 일치)
     */
    public Object getRawYoutubeByEsDocId(String esDocId) {
        try {
            GetResponse<JsonData> response = elasticsearchClient.get(
                    new GetRequest.Builder().index(INDEX).id(esDocId).build(),
                    JsonData.class
            );
            if (!response.found()) throw new RuntimeException("❌ ES 문서 없음: " + esDocId);
            
            // JSON 객체로 직접 반환 (실행 중인 컴트롤러와 일치)
            JsonNode root = objectMapper.readTree(response.source().toJson().toString());
            return objectMapper.treeToValue(root, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("❌ YouTube ES Object 조회 실패: " + e.getMessage(), e);
        }
    }

    // 썸네일 파싱 헬퍼 메서드
    private YoutubeThumbnailDto parseThumbnails(JsonNode thumbnailNode) {
        if (thumbnailNode == null || thumbnailNode.isMissingNode()) {
            return null;
        }
        
        return YoutubeThumbnailDto.builder()
                .defaultThumbnail(parseThumbnailDetail(thumbnailNode.path("default")))
                .medium(parseThumbnailDetail(thumbnailNode.path("medium")))
                .high(parseThumbnailDetail(thumbnailNode.path("high")))
                .standard(parseThumbnailDetail(thumbnailNode.path("standard")))
                .maxres(parseThumbnailDetail(thumbnailNode.path("maxres")))
                .build();
    }

    private YoutubeThumbnailDto.ThumbnailDetailDto parseThumbnailDetail(JsonNode detailNode) {
        if (detailNode == null || detailNode.isMissingNode()) {
            return null;
        }
        
        return YoutubeThumbnailDto.ThumbnailDetailDto.builder()
                .url(getText(detailNode, "url"))
                .width(getInt(detailNode, "width"))
                .height(getInt(detailNode, "height"))
                .build();
    }

    // 댓글 파싱 헬퍼 메서드
    private List<YoutubeCommentDto> parseComments(JsonNode commentsNode) {
        List<YoutubeCommentDto> comments = new ArrayList<>();
        
        if (commentsNode == null || !commentsNode.isArray()) {
            return comments;
        }
        
        for (JsonNode commentNode : commentsNode) {
            YoutubeCommentDto comment = YoutubeCommentDto.builder()
                    .commentId(getText(commentNode, "comment_id"))
                    .videoId(getText(commentNode, "video_id"))
                    .author(getText(commentNode, "author"))
                    .comment(getText(commentNode, "comment"))
                    .likeCount(getInt(commentNode, "like_count"))
                    .publishedAt(getText(commentNode, "published_at"))
                    .updatedAt(getText(commentNode, "updated_at"))
                    .totalReplyCount(getInt(commentNode, "total_reply_count"))
                    .build();
            comments.add(comment);
        }
        
        return comments;
    }

    // 문자열로 된 숫자를 Integer로 변환하는 헬퍼 메서드
    private Integer getIntFromString(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode()) {
            return null;
        }
        
        if (fieldNode.isInt()) {
            return fieldNode.asInt();
        }
        
        try {
            return Integer.parseInt(fieldNode.asText());
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {} = {}", fieldName, fieldNode.asText());
            return null;
        }
    }
}
