package org.example.ssj3pj.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.ssj3pj.dto.RedditCommentDto;

import java.util.*;

import static org.example.ssj3pj.util.JsonNodeUtils.*;

public final class RedditJsonParsers {

    private RedditJsonParsers() {}

    /**
     * comments 오브젝트 (키: "0","1",...) 를 상위 N개 댓글 리스트로 변환
     */
    public static List<RedditCommentDto> parseComments(JsonNode commentsNode, int limit) {
        if (commentsNode == null || !commentsNode.isObject()) return List.of();

        // 키 정렬(숫자 오름차순)
        List<String> keys = new ArrayList<>();
        commentsNode.fieldNames().forEachRemaining(keys::add);
        keys.sort(Comparator.comparingInt(k -> {
            try { return Integer.parseInt(k); } catch (Exception e) { return Integer.MAX_VALUE; }
        }));

        List<RedditCommentDto> list = new ArrayList<>();
        for (String k : keys) {
            if (list.size() >= limit) break;
            JsonNode c = commentsNode.path(k);
            list.add(RedditCommentDto.builder()
                    .id(getText(c, "id"))
                    .author(getText(c, "author"))
                    .body(getText(c, "body"))
                    .score(getInt(c, "score"))
                    .createdAt(epochToIso(c.path("created_utc")))
                    .depth(getInt(c, "depth"))
                    .parentId(getText(c, "parent_id"))
                    .numReplies(getInt(c, "num_replies"))
                    .build());
        }
        return list;
    }
}
