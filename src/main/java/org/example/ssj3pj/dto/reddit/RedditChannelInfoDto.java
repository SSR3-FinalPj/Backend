package org.example.ssj3pj.dto.reddit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedditChannelInfoDto {
    private String channelId;

    private String channelTitle;

}
