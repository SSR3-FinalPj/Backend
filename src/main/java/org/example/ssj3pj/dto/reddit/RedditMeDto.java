package org.example.ssj3pj.dto.reddit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @NoArgsConstructor @AllArgsConstructor
public class RedditMeDto {
    private String id;
    private String name;

    public static RedditMeDto from(Map<String,Object> m) {
        if (m == null) throw new IllegalStateException("Reddit me parse failed: null");
        return new RedditMeDto((String) m.get("id"), (String) m.get("name"));
    }
}
