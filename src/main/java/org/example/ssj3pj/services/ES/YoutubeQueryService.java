package org.example.ssj3pj.services.ES;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.youtube.YoutubeSummaryDto;
import org.example.ssj3pj.dto.youtube.YoutubeCommentDto;
import org.example.ssj3pj.dto.youtube.YoutubeThumbnailDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.Collections;
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
            return YoutubeSummaryDto.builder()
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

    public Object getRawYoutubeByEsDocId(String esDocId) {
        try {
            GetResponse<JsonData> response = elasticsearchClient.get(
                    new GetRequest.Builder().index(INDEX).id(esDocId).build(),
                    JsonData.class
            );
            if (!response.found()) throw new RuntimeException("❌ ES 문서 없음: " + esDocId);

            JsonNode root = objectMapper.readTree(response.source().toJson().toString());
            return objectMapper.treeToValue(root, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("❌ YouTube ES Object 조회 실패: " + e.getMessage(), e);
        }
    }

    public JsonNode findAllCommentsForVideo(String esDocId, String videoId) throws IOException {
        log.info("Fetching comments for esDocId: {} and videoId: {}", esDocId, videoId);

        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return objectMapper.createObjectNode();
        }

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode videosNode = source.path("videos");

        if (!videosNode.isArray()) {
            log.warn("The 'videos' field is not an array in document id: {}", esDocId);
            return objectMapper.createObjectNode();
        }

        for (JsonNode videoNode : videosNode) {
            if (videoId.equals(videoNode.path("video_id").asText())) {
                JsonNode commentsNode = videoNode.path("comments");
                log.info("commentsNode raw: {}", commentsNode.toPrettyString());

                if (commentsNode.isArray()) {
                    ArrayNode commentsArray = objectMapper.createArrayNode();

                    for (JsonNode commentNode : commentsNode) {
                        ObjectNode newComment = objectMapper.createObjectNode();
                        newComment.put("comment_id", commentNode.path("comment_id").asText());
                        newComment.put("author", commentNode.path("author").asText());
                        newComment.put("comment", commentNode.path("text").asText()); // ✅ text → comment
                        newComment.put("like_count", commentNode.path("like_count").asInt());
                        newComment.put("total_reply_count", commentNode.path("reply_count").asInt());
                        newComment.put("published_at", commentNode.path("published_at").asText());

                        commentsArray.add(newComment);
                    }

                    ObjectNode youtubeNode = objectMapper.createObjectNode();
                    youtubeNode.put("videoId", videoId);
                    youtubeNode.set("comments", commentsArray);

                    ObjectNode result = objectMapper.createObjectNode();
                    result.set("youtube", youtubeNode);

                    return result; // ✅ 원하는 JSON 구조 반환
                }
            }
        }

        log.warn("No video found with videoId: {} in document id: {}", videoId, esDocId);
        return objectMapper.createObjectNode();
    }


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
