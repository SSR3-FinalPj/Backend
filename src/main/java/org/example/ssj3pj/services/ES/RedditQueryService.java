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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
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

    public RedditSummaryDto getSummaryByDocId(String esDocId) {
        try {
            // 1. ES에서 문서 가져오기
            GetRequest req = new GetRequest.Builder()
                    .index(INDEX)
                    .id(esDocId)
                    .build();

            GetResponse<JsonData> res = elasticsearchClient.get(req, JsonData.class);
            if (!res.found()) {
                throw new RuntimeException("❌ ES 문서 없음 (reddit): " + esDocId);
            }

            // 2. JsonNode 변환
            JsonNode src = objectMapper.readTree(res.source().toJson().toString());

            // 3. DTO 빌드
            return RedditSummaryDto.builder()
                    .id(getText(src, "id"))
                    .title(getText(src, "title"))
                    .selftext(getText(src, "selftext"))
                    .subreddit(getText(src, "subreddit"))
                    .url(getText(src, "url"))
                    .createdAt(epochToIso(src.path("created_utc")))
                    .score(getInt(src, "score"))
                    .upvoteRatio(getDouble(src, "upvote_ratio"))
                    .upvotesEstimated(getInt(src, "upvotes_estimated"))
                    .downvotesEstimated(getInt(src, "downvotes_estimated"))
                    .numComments(getInt(src, "num_comments"))
                    .over18(getBool(src, "over_18"))
                    .commentCount(getInt(src, "commentcount"))
                    .comments(parseComments(src.path("comments"), 5))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("❌ Reddit 데이터 파싱 실패: " + e.getMessage(), e);
        }
    }
    public PostListDto findAllVideoForChannel(String esDocId, String channelId, String pageToken) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode videosNode = source.path("videos");
        List<PostItemDto> videoItemList = new ArrayList<>();
        for (JsonNode videoNode : videosNode) {
            PostStatisticsDto videoStatistics = PostStatisticsDto.builder()
                    .commentCount(videoNode.path("comment_count").asLong())
                    .likeCount(videoNode.path("like_count").asLong())
                    .viewCount(videoNode.path("view_count").asLong())
                    .build();
            PostItemDto videoItem = PostItemDto.builder()
                    .postId(videoNode.path("video_id").asText())
                    .title(videoNode.path("title").asText())
                    .url("https://www.youtube.com/watch?v=" + videoNode.path("video_id").asText())
                    .thumbnail(videoNode.path("thumbnails").path("standard").path("url").asText())
                    .publishedAt(videoNode.path("upload_date").asText())
                    .statistics(videoStatistics)
                    .build();
            videoItemList.add(videoItem);
        }
        return PostListDto.builder()
                .channelId(channelId)
                .videos(videoItemList)
                .nextPageToken(pageToken)
                .build();
    }
    public ChannelInfoDto findChannel(String esDocId) throws IOException{
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        String channelId = source.path("channel_id").asText();
        String channelTitle = source.path("channel_title").asText();
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return null;
        }
        return ChannelInfoDto.builder()
                .channelId(channelId)
                .channelTitle(channelTitle)
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
                log.info("videoNode raw: {}", postNode.toPrettyString());

                // High quality 썸네일이 있으면 사용, 없으면 기본값
                String thumbnailUrl = null;
                JsonNode thumbnails = postNode.path("thumbnails");
                if (thumbnails.has("high")) {
                    thumbnailUrl = thumbnails.path("high").path("url").asText(null);
                } else if (thumbnails.has("default")) {
                    thumbnailUrl = thumbnails.path("default").path("url").asText(null);
                }

                // DTO 빌드
                RedditContentDetailDto dto = RedditContentDetailDto.builder()
                        .postId(postNode.path("video_id").asText())
                        .uploadDate(postNode.path("upload_date").asText(null))
                        .title(postNode.path("title").asText(null))
                        .viewCount(postNode.path("view_count").asInt(0))
                        .commentCount(postNode.path("comment_count").asInt(0))
                        .likeCount(postNode.path("like_count").asInt(0))
                        .build();

                return dto;
            }
        }

        return null;
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

                    ObjectNode redditNode = objectMapper.createObjectNode();
                    redditNode.put("videoId", videoId);
                    redditNode.set("comments", commentsArray);

                    ObjectNode result = objectMapper.createObjectNode();
                    result.set("youtube", redditNode);

                    return result; // ✅ 원하는 JSON 구조 반환
                }
            }
        }

        log.warn("No video found with videoId: {} in document id: {}", videoId, esDocId);
        return objectMapper.createObjectNode();
    }

}
