package org.example.ssj3pj.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.RedditSummaryDto;
import org.springframework.stereotype.Service;

import static org.example.ssj3pj.util.JsonNodeUtils.*;
import static org.example.ssj3pj.util.RedditJsonParsers.parseComments;

@Service
@RequiredArgsConstructor
public class RedditQueryService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;

    private static final String INDEX = "reddit";

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
}
