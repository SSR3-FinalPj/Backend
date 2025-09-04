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
import org.example.ssj3pj.dto.dashboard.DashboardRDDayStats;
import org.example.ssj3pj.dto.dashboard.DashboardRDTotalStats;
import org.example.ssj3pj.dto.reddit.*;
import org.example.ssj3pj.dto.youtube.ChannelInfoDto;
import org.example.ssj3pj.dto.youtube.YTUploadRangeDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.example.ssj3pj.util.JsonNodeUtils.*;
import static org.example.ssj3pj.util.RedditJsonParsers.parseComments;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedditQueryService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    private static final String INDEX = "redditdata";

    // ✅ 공통 유틸 메서드 추가
    private String safeGetVideoUrl(JsonNode postNode) {
        JsonNode urlNode = postNode.path("media").path("reddit_video").path("fallback_url");
        return urlNode.isMissingNode() || urlNode.isNull() ? null : urlNode.asText(null);
    }
    public DashboardRDTotalStats findAllStat(String esDocId) throws IOException{
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return null;
        }

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode postsNode = source.path("posts");
        long ups_count = 0;
        double upvote_ratio = 0.0;
        long comment_count = 0;
        for (JsonNode videoNode : postsNode) {
            upvote_ratio += videoNode.path("upvote_ratio").asDouble(1);
            comment_count += videoNode.path("num_comments").asLong(0);
            ups_count += videoNode.path("ups").asLong(0);
        }
        double ratio = upvote_ratio / source.path("post_count").asLong();
        double roundedRatio = Math.round(ratio * 100.0) / 100.0; // 소수점 둘째자리까지
        return DashboardRDTotalStats.builder()
                .totalPostCount(source.path("post_count").asLong())
                .totalUpvoteRatio(roundedRatio)
                .totalUpvoteCount(ups_count)
                .totalCommentCount(comment_count)
                .build();
    }

    public DashboardRDDayStats findDayStatForChannel(String esDocId, LocalDate date) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return null;
        }

        // source를 JsonNode로 파싱
        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode postsNode = source.path("posts");
        long ups_count = 0;
        double upvote_ratio = 0.0;
        long comment_count = 0;
        for (JsonNode videoNode : postsNode) {
            upvote_ratio += videoNode.path("upvote_ratio").asDouble(1);
            comment_count += videoNode.path("num_comments").asLong(0);
            ups_count += videoNode.path("ups").asLong(0);
        }
        double ratio = upvote_ratio / source.path("post_count").asLong();
        double roundedRatio = Math.round(ratio * 100.0) / 100.0; // 소수점 둘째자리까지

        return DashboardRDDayStats.builder()
                .date(date)  // Service에서 받은 날짜 문자열
                .upvoteRatio(roundedRatio)
                .commentCount(comment_count)
                .postCount(source.path("post_count").asLong())
                .upvoteCount(ups_count)
                .build();
    }
    public ChannelInfoDto findChannel(String esDocId) throws IOException{
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        String channelTitle = source.path("reddit_username").asText();
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return null;
        }
        return ChannelInfoDto.builder()
                .channelId(channelTitle)
                .channelTitle(channelTitle)
                .build();
    }
    public RDUploadRangeDto findAllPostRangeDate(String esDocId, String channelId, LocalDate start, LocalDate end) throws IOException {
        GetRequest request = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(request, JsonData.class);
        if (!response.found()) {
            throw new RuntimeException("❌ ES 문서 없음 (youtube): " + esDocId);
        }

        JsonNode src = objectMapper.readTree(response.source().toJson().toString());
        JsonNode postsNode = src.path("posts");
        List<RedditContentDetailDto> videoItemList = new ArrayList<>();
        long ups_count = 0;
        double upvote_ratio = 0.0;
        long comment_count = 0;

        for (JsonNode postNode : postsNode) {
            long createdEpoch = postNode.path("created").asLong(0);
            if (createdEpoch == 0) continue;

            LocalDateTime createdDateTime = Instant.ofEpochSecond(createdEpoch)
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDateTime();

            LocalDate uploadDate = createdDateTime.toLocalDate();

            if ((uploadDate.isEqual(start) || uploadDate.isAfter(start)) &&
                    (uploadDate.isEqual(end)   || uploadDate.isBefore(end))) {

                log.info("✅ 기간 포함 영상: {} ({})", postNode.path("title").asText(), uploadDate);

                Double upvoteRatio = postNode.path("upvote_ratio").asDouble(1);
                int commentCount = postNode.path("num_comments").asInt();
                int upvoteCount = postNode.path("ups").asInt(0);

                RedditContentDetailDto videoItem = RedditContentDetailDto.builder()
                        .postId(postNode.path("id").asText())
                        .uploadDate(String.valueOf(uploadDate))
                        .title(postNode.path("title").asText(null))
                        .upvote(postNode.path("ups").asInt(0))
                        .commentCount(postNode.path("num_comments").asInt(0))
                        .upvoteRatio(postNode.path("upvote_ratio").asDouble(0))
                        .score(postNode.path("score").asInt(0))
                        .subReddit(postNode.path("subreddit").asText(null))
                        .text(postNode.path("selftext").asText(null))
                        .url(postNode.path("url").asText(null))
                        .userName(postNode.path("author").asText(null))
                        .RDvideoUrl(safeGetVideoUrl(postNode))   // ✅ 수정
                        .build();
                videoItemList.add(videoItem);

                upvote_ratio += upvoteRatio;
                comment_count += commentCount;
                ups_count += upvoteCount;
            }
        }

        double ratio = upvote_ratio / src.path("post_count").asLong();
        double roundedRatio = Math.round(ratio * 100.0) / 100.0;

        DashboardRDTotalStats totalStats = DashboardRDTotalStats.builder()
                .totalPostCount(src.path("post_count").asLong())
                .totalUpvoteRatio(roundedRatio)
                .totalUpvoteCount(ups_count)
                .totalCommentCount(comment_count)
                .build();

        return RDUploadRangeDto.builder()
                .total(totalStats)
                .posts(videoItemList)
                .build();
    }

    public PostListDto findAllPostForChannel(String esDocId, String channelId) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode postsNode = source.path("posts");

        List<RedditContentDetailDto> postItemList = new ArrayList<>();
        for (JsonNode postNode : postsNode) {
            long createdEpoch = postNode.path("created").asLong(0);
            if (createdEpoch == 0) continue;

            LocalDateTime createdDateTime = Instant.ofEpochSecond(createdEpoch)
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDateTime();

            LocalDate uploadDate = createdDateTime.toLocalDate();

            RedditContentDetailDto videoItem = RedditContentDetailDto.builder()
                    .postId(postNode.path("id").asText())
                    .uploadDate(String.valueOf(uploadDate))
                    .title(postNode.path("title").asText(null))
                    .upvote(postNode.path("ups").asInt(0))
                    .commentCount(postNode.path("num_comments").asInt(0))
                    .upvoteRatio(postNode.path("upvote_ratio").asDouble(0))
                    .score(postNode.path("score").asInt(0))
                    .subReddit(postNode.path("subreddit").asText(null))
                    .text(postNode.path("selftext").asText(null))
                    .url(postNode.path("url").asText(null))
                    .userName(postNode.path("author").asText(null))
                    .RDvideoUrl(safeGetVideoUrl(postNode))   // ✅ 수정
                    .build();
            postItemList.add(videoItem);
        }

        return PostListDto.builder()
                .channelId(channelId)
                .posts(postItemList)
                .build();
    }
    public JsonNode findAllCommentsForPost(String esDocId, String postId) throws IOException {
        log.info("Fetching comments for esDocId: {} and postId: {}", esDocId, postId);

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
        JsonNode postsNode = source.path("posts");

        if (!postsNode.isArray()) {
            log.warn("The 'posts' field is not an array in document id: {}", esDocId);
            return objectMapper.createObjectNode();
        }

        for (JsonNode postNode : postsNode) {
            if (postId.equals(postNode.path("id").asText())) {
                JsonNode commentsNode = postNode.path("parsed_comments");
                log.info("commentsNode raw: {}", commentsNode.toPrettyString());

                if (commentsNode.isArray()) {
                    ArrayNode commentsArray = objectMapper.createArrayNode();

                    for (JsonNode commentNode : commentsNode) {
                        double createdValue = commentNode.path("created").asDouble(); // 1754636569.0
                        long epochSeconds = (long) createdValue;

                        LocalDate date = Instant.ofEpochSecond(epochSeconds)
                                .atZone(ZoneId.systemDefault())  // 원하는 타임존 적용
                                .toLocalDate();
                        ObjectNode newComment = objectMapper.createObjectNode();
                        newComment.put("comment_id", commentNode.path("id").asText());
                        newComment.put("author", commentNode.path("author").asText());
                        newComment.put("comment", commentNode.path("body").asText()); // ✅ text → comment
                        newComment.put("like_count", commentNode.path("score").asInt());
                        newComment.put("total_reply_count", commentNode.path("parsed_replies").size());
                        newComment.put("published_at", String.valueOf(date));

                        commentsArray.add(newComment);
                    }

                    ObjectNode redditNode = objectMapper.createObjectNode();
                    redditNode.put("postId", postId);
                    redditNode.set("comments", commentsArray);

                    ObjectNode result = objectMapper.createObjectNode();
                    result.set("reddit", redditNode);

                    return result; // ✅ 원하는 JSON 구조 반환
                }
            }
        }

        log.warn("No post found with postId: {} in document id: {}", postId, esDocId);
        return objectMapper.createObjectNode();
    }
    public RedditContentDetailDto findAllDetailForPost(String esDocId, String videoId) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return null;
        }

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode postsNode = source.path("posts");

        for (JsonNode postNode : postsNode) {
            if (videoId.equals(postNode.path("id").asText())) {
                long createdEpoch = postNode.path("created").asLong(0);
                if (createdEpoch == 0) continue;

                LocalDateTime createdDateTime = Instant.ofEpochSecond(createdEpoch)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDateTime();

                LocalDate uploadDate = createdDateTime.toLocalDate();

                return RedditContentDetailDto.builder()
                        .postId(postNode.path("id").asText())
                        .uploadDate(String.valueOf(uploadDate))
                        .title(postNode.path("title").asText(null))
                        .upvote(postNode.path("ups").asInt(0))
                        .commentCount(postNode.path("num_comments").asInt(0))
                        .upvoteRatio(postNode.path("upvote_ratio").asDouble(0))
                        .score(postNode.path("score").asInt(0))
                        .subReddit(postNode.path("subreddit").asText(null))
                        .text(postNode.path("selftext").asText(null))
                        .url(postNode.path("url").asText(null))
                        .userName(postNode.path("author").asText(null))
                        .RDvideoUrl(safeGetVideoUrl(postNode))   // ✅ 수정
                        .build();
            }
        }
        return null;
    }
}
