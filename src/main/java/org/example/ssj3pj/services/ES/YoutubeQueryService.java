package org.example.ssj3pj.services.ES;
import java.util.Collections;

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
import org.example.ssj3pj.dto.dashboard.DashboardYTDayStats;
import org.example.ssj3pj.dto.dashboard.DashboardYTTotalStats;
import org.example.ssj3pj.dto.youtube.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import static org.example.ssj3pj.util.JsonNodeUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class YoutubeQueryService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    private static final String INDEX = "youtubedata";  // YouTube 인덱스명

    public YTUploadRangeDto findAllVideoRangeDate(String esDocId, String channelId, LocalDate start, LocalDate end) throws IOException {
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

        JsonNode videosNode = src.path("videos");
        List<UploadVideoDetailDto> videoItemList = new ArrayList<>();
        long totalView = 0;
        long totalLike = 0;
        long totalComment = 0;
        long totalVideoCount = 0;
        for (JsonNode videoNode : videosNode) {
            String uploadDateStr = videoNode.path("upload_date").asText();
            if (uploadDateStr == null || uploadDateStr.isEmpty()) continue;

            // upload_date 파싱
            LocalDate uploadDate = LocalDate.parse(uploadDateStr.substring(0, 10)); // yyyy-MM-dd 부분만 추출

            // 전달받은 기간 안에 포함된 영상만 처리
            if ((uploadDate.isEqual(start) || uploadDate.isAfter(start)) &&
                    (uploadDate.isEqual(end)   || uploadDate.isBefore(end))) {

                log.info("✅ 기간 포함 영상: {} ({})", videoNode.path("title").asText(), uploadDateStr);

                int viewCount = videoNode.path("view_count").asInt(0);
                int likeCount = videoNode.path("like_count").asInt(0);
                int commentCount = videoNode.path("comment_count").asInt(0);

                // 영상 DTO 추가
                UploadVideoDetailDto videoItem = UploadVideoDetailDto.builder()
                        .title(videoNode.path("title").asText(null))
                        .description(videoNode.path("description").asText(null))
                        .channelTitle(videoNode.path("channel_title").asText(null))
                        .uploadDate(uploadDateStr)
                        .viewCount(viewCount)
                        .likeCount(likeCount)
                        .commentCount(commentCount)
                        .build();
                videoItemList.add(videoItem);

                // 총합 업데이트
                totalView += viewCount;
                totalLike += likeCount;
                totalComment += commentCount;
                totalVideoCount++;
            }
        }
        // 합산 DTO
        DashboardYTTotalStats totalStats = DashboardYTTotalStats.builder()
                .totalVideoCount(totalVideoCount)
                .totalViewCount(totalView)
                .totalLikeCount(totalLike)
                .totalCommentCount(totalComment)
                .build();

        // 최종 반환 DTO
        return YTUploadRangeDto.builder()
                .total(totalStats)
                .videos(videoItemList)
                .build();
    }
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

    public JsonNode getJsonNodeByDocId(String esDocId) throws IOException {
        GetRequest request = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(request, JsonData.class);
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return null;
        }

        return objectMapper.readTree(response.source().toJson().toString());
    }
    public VideoListDto findAllVideoForChannel(String esDocId, String channelId, String pageToken) throws IOException{
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode videosNode = source.path("videos");
        List<VideoItemDto> videoItemList = new ArrayList<>();
        for (JsonNode videoNode : videosNode) {
            VideoStatisticsDto videoStatistics = VideoStatisticsDto.builder()
                    .commentCount(videoNode.path("comment_count").asLong())
                    .likeCount(videoNode.path("like_count").asLong())
                    .viewCount(videoNode.path("view_count").asLong())
                    .build();
            VideoItemDto videoItem = VideoItemDto.builder()
                    .videoId(videoNode.path("video_id").asText())
                    .title(videoNode.path("title").asText())
                    .url("https://www.youtube.com/watch?v=" + videoNode.path("video_id").asText())
                    .thumbnail(videoNode.path("thumbnails").path("standard").path("url").asText())
                    .publishedAt(videoNode.path("upload_date").asText())
                    .statistics(videoStatistics)
                    .build();
            videoItemList.add(videoItem);
        }
        return VideoListDto.builder()
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
    public DashboardYTDayStats findDayStatForChannel(String esDocId, LocalDate date) throws IOException {
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
        JsonNode channelStatsNode = source.path("channel_stats");
        JsonNode videosNode = source.path("videos");
        long view_count = 0;
        long comment_count = 0;
        long like_count = 0;
        for (JsonNode videoNode : videosNode) {
            comment_count += videoNode.path("comment_count").asLong(0);
            like_count += videoNode.path("like_count").asLong(0);
            view_count += videoNode.path("view_count").asLong(0);
        }
        // DashboardDayStats 객체 생성
        return DashboardYTDayStats.builder()
                .date(date)  // Service에서 받은 날짜 문자열
                .viewCount(view_count)
                .subscriberCount(channelStatsNode.path("subscriber_count").asLong(0))
                .commentCount(comment_count)
                .videoCount(channelStatsNode.path("video_count").asLong(0))
                .likeCount(like_count)
                .build();
    }

    public YoutubeContentDetailDto findAllDetailForVideo(String esDocId, String videoId) throws IOException {
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
        JsonNode videosNode = source.path("videos");

        for (JsonNode videoNode : videosNode) {
            if (videoId.equals(videoNode.path("video_id").asText())) {
                log.info("videoNode raw: {}", videoNode.toPrettyString());

                // High quality 썸네일이 있으면 사용, 없으면 기본값
                String thumbnailUrl = null;
                JsonNode thumbnails = videoNode.path("thumbnails");
                if (thumbnails.has("high")) {
                    thumbnailUrl = thumbnails.path("high").path("url").asText(null);
                } else if (thumbnails.has("default")) {
                    thumbnailUrl = thumbnails.path("default").path("url").asText(null);
                }

                // DTO 빌드
                YoutubeContentDetailDto dto = YoutubeContentDetailDto.builder()
                        .videoId(videoNode.path("video_id").asText())
                        .uploadDate(videoNode.path("upload_date").asText(null))
                        .thumbnailUrl(thumbnailUrl)
                        .title(videoNode.path("title").asText(null))
                        .viewCount(videoNode.path("view_count").asInt(0))
                        .commentCount(videoNode.path("comment_count").asInt(0))
                        .likeCount(videoNode.path("like_count").asInt(0))
                        .build();

                return dto;
            }
        }

        return null;
    }
    public DashboardYTTotalStats findAllStat(String esDocId) throws IOException{
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
        JsonNode videosNode = source.path("videos");
        long view_count = 0;
        long comment_count = 0;
        long like_count = 0;
        for (JsonNode videoNode : videosNode) {
            comment_count += videoNode.path("comment_count").asLong(0);
            like_count += videoNode.path("like_count").asLong(0);
            view_count += videoNode.path("view_count").asLong(0);
        }
        return DashboardYTTotalStats.builder()
                .totalVideoCount(source.path("channel_stats").path("video_count").asLong())
                .totalLikeCount(like_count)
                .totalViewCount(view_count)
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

    /**
     * 특정 비디오의 트래픽 소스 분석 데이터 조회 (esDocId 기반)
     * 채널 전체 트래픽 소스를 해당 비디오의 조회수 비율에 맞춰 계산
     */
    public List<TrafficSourceDto> findTrafficSourceByVideoId(String esDocId, String videoId) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return Collections.emptyList();
        }

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        
        // 1. 해당 비디오의 조회수 찾기
        JsonNode videosNode = source.path("videos");
        long targetVideoViews = 0;
        boolean videoFound = false;
        
        if (videosNode.isArray()) {
            for (JsonNode video : videosNode) {
                if (videoId.equals(video.path("video_id").asText())) {
                    targetVideoViews = video.path("view_count").asLong(0);
                    videoFound = true;
                    log.info("타겟 비디오 찾음: videoId={}, views={}", videoId, targetVideoViews);
                    break;
                }
            }
        }
        
        if (!videoFound) {
            log.warn("해당 비디오를 찾을 수 없음: videoId={}", videoId);
            return Collections.emptyList();
        }
        
        if (targetVideoViews == 0) {
            log.warn("비디오 조회수가 0: videoId={}", videoId);
            return Collections.emptyList();
        }
        
        // 2. 채널 전체 조회수 계산
        long totalChannelViews = 0;
        if (videosNode.isArray()) {
            for (JsonNode video : videosNode) {
                totalChannelViews += video.path("view_count").asLong(0);
            }
        }
        
        if (totalChannelViews == 0) {
            log.warn("채널 전체 조회수가 0");
            return Collections.emptyList();
        }
        
        // 3. 비율 계산
        double videoRatio = (double) targetVideoViews / totalChannelViews;
        log.info("비디오 비율 계산: {}({}) / {}(total) = {}", 
                videoId, targetVideoViews, totalChannelViews, String.format("%.4f", videoRatio));
        
        // 4. 채널 트래픽 소스 데이터 조회 및 총합 계산
        JsonNode channelAnalytics = source.path("channel_analytics");
        JsonNode trafficAnalytics = channelAnalytics.path("traffic_source_analytics");
        
        List<TrafficSourceDto> tempResult = new ArrayList<>();
        long totalChannelTrafficViews = 0;
        
        // 먼저 채널 트래픽 소스 총합을 구함
        if (trafficAnalytics.isArray()) {
            for (JsonNode traffic : trafficAnalytics) {
                String sourceType = traffic.path("insightTrafficSourceType").asText(null);
                long channelViews = traffic.path("views").asLong(0);
                
                if (sourceType != null && channelViews > 0) {
                    totalChannelTrafficViews += channelViews;
                    tempResult.add(TrafficSourceDto.builder()
                            .insightTrafficSourceType(sourceType)
                            .views(channelViews)
                            .build());
                }
            }
        }
        
        log.info("채널 트래픽 소스 총합: {}", totalChannelTrafficViews);
        
        List<TrafficSourceDto> result = new ArrayList<>();
        
        if (totalChannelTrafficViews > 0 && !tempResult.isEmpty()) {
            // 각 트래픽 소스를 실제 비디오 조회수에 맞춰 비례 배분
            long assignedViews = 0;
            
            for (int i = 0; i < tempResult.size(); i++) {
                TrafficSourceDto item = tempResult.get(i);
                long videoTrafficViews;
                
                if (i == tempResult.size() - 1) {
                    // 마지막 항목은 남은 조회수를 모두 할당 (반올림 오차 보정)
                    videoTrafficViews = targetVideoViews - assignedViews;
                } else {
                    // 비례 계산
                    double sourceRatio = (double) item.getViews() / totalChannelTrafficViews;
                    videoTrafficViews = Math.round(targetVideoViews * sourceRatio);
                    assignedViews += videoTrafficViews;
                }
                
                if (videoTrafficViews > 0) {
                    result.add(TrafficSourceDto.builder()
                            .insightTrafficSourceType(item.getInsightTrafficSourceType())
                            .views(videoTrafficViews)
                            .build());
                    
                    double sourceRatio = (double) item.getViews() / totalChannelTrafficViews;
                    log.info("비례 배분: {} = {} × {:.4f} = {} views", 
                            item.getInsightTrafficSourceType(), targetVideoViews, 
                            sourceRatio, videoTrafficViews);
                }
            }
            
            // 검증: 총합 확인
            long totalAssigned = result.stream().mapToLong(TrafficSourceDto::getViews).sum();
            log.info("트래픽 소스 총합 검증: {} (목표: {})", totalAssigned, targetVideoViews);
        }
        
        // 조회수 내림차순 정렬
        result.sort((a, b) -> Long.compare(b.getViews(), a.getViews()));
        
        log.info("최종 결과: videoId={}, {} 개 트래픽 소스 (조회수 비율 기반)", videoId, result.size());
        return result;
    }

    /**
     * 특정 ES 문서에서 demographics 데이터만 조회
     */
    public List<DemographicPoint> getDemographicsFromES(String esDocId) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return Collections.emptyList();
        }

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode channelAnalytics = source.path("channel_analytics");
        JsonNode demographics = channelAnalytics.path("demographics");
        
        List<DemographicPoint> points = new ArrayList<>();
        if (demographics.isArray()) {
            for (JsonNode demo : demographics) {
                points.add(DemographicPoint.builder()
                        .ageGroup(demo.path("ageGroup").asText())
                        .gender(demo.path("gender").asText())
                        .viewerPercentage(demo.path("viewerPercentage").asDouble())
                        .build());
            }
        }
        
        log.info("ES에서 demographics 조회 완료: esDocId={}, {} 개 항목", esDocId, points.size());
        return points;
    }
    
    /**
     * 특정 ES 문서에서 트래픽 소스 요약 데이터 조회 (채널 전체)
     */
    public List<TrafficSourceDto> findTrafficSourceSummary(String esDocId) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return Collections.emptyList();
        }

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode channelAnalytics = source.path("channel_analytics");
        JsonNode trafficAnalytics = channelAnalytics.path("traffic_source_analytics");
        
        List<TrafficSourceDto> result = new ArrayList<>();
        
        if (trafficAnalytics.isArray()) {
            for (JsonNode traffic : trafficAnalytics) {
                String sourceType = traffic.path("insightTrafficSourceType").asText(null);
                long views = traffic.path("views").asLong(0);
                
                if (sourceType != null && views > 0) {
                    result.add(TrafficSourceDto.builder()
                            .insightTrafficSourceType(sourceType)
                            .views(views)
                            .build());
                }
            }
        }
        
        log.info("ES에서 트래픽 소스 요약 조회 완료: esDocId={}, {} 개 항목", esDocId, result.size());
        return result;
    }
    
    /**
     * 사용자별 일별 인구통계 데이터 조회 (esDocId 기반)
     */
    public List<DailyDemographicsDto> findDailyDemographics(String esDocId, LocalDate startDate, LocalDate endDate) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
                .index(INDEX)
                .id(esDocId)
                .build();

        GetResponse<JsonData> response = elasticsearchClient.get(getRequest, JsonData.class);
        if (!response.found()) {
            log.warn("ES document not found for id: {}", esDocId);
            return Collections.emptyList();
        }

        JsonNode source = objectMapper.readTree(response.source().toJson().toString());
        JsonNode channelAnalytics = source.path("channel_analytics");
        JsonNode demographics = channelAnalytics.path("demographics");
        
        List<DailyDemographicsDto> result = new ArrayList<>();
        
        if (demographics.isArray()) {
            for (JsonNode demo : demographics) {
                String date = demo.path("stat_date").asText(null);
                
                if (date != null) {
                    try {
                        LocalDate statDate = LocalDate.parse(date);
                        
                        // 날짜 범위 필터링
                        boolean dateInRange = (statDate.isEqual(startDate) || statDate.isAfter(startDate)) && 
                            (statDate.isEqual(endDate) || statDate.isBefore(endDate));
                        
                        if (dateInRange) {
                            List<DemographicPoint> points = new ArrayList<>();
                            JsonNode demoPoints = demo.path("demographic_points");
                            
                            if (demoPoints.isArray()) {
                                for (JsonNode point : demoPoints) {
                                    points.add(DemographicPoint.builder()
                                            .ageGroup(point.path("ageGroup").asText())
                                            .gender(point.path("gender").asText())
                                            .viewerPercentage(point.path("viewerPercentage").asDouble())
                                            .build());
                                }
                            }
                            
                            result.add(DailyDemographicsDto.builder()
                                    .date(date)
                                    .demographics(points)
                                    .build());
                        }
                    } catch (Exception e) {
                        log.warn("날짜 파싱 실패: {}", date);
                    }
                }
            }
        }
        
        // 날짜 오름차순 정렬
        result.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return result;
    }
    
    /**
     * 채널에 속한 모든 영상 ID 조회
     */
    public List<String> findAllVideoIdsByChannel(String channelId) throws IOException {
        // TODO: Elasticsearch Query DSL로 channelId에 해당하는 모든 영상 ID 조회
        log.info("채널의 모든 영상 ID 조회: channelId={}", channelId);
        return Collections.emptyList();
    }
    
    /**
     * 특정 영상의 기간 내 최신 트래픽 소스 데이터 조회
     */
    public List<TrafficSourceDto> findLatestTrafficSourceByVideoAndPeriod(
            String videoId, LocalDateTime start, LocalDateTime end) throws IOException {
        // TODO: Elasticsearch Query DSL로 videoId와 @timestamp 범위로 필터링하여 최신 문서 1개 조회
        log.info("영상 기간 내 최신 트래픽 소스 조회: videoId={}, 기간={} ~ {}", videoId, start, end);
        return Collections.emptyList();
    }
}
