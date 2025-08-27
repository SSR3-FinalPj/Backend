package org.example.ssj3pj.dto.youtube;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeThumbnailDto {
    private ThumbnailDetailDto defaultThumbnail;  // "default"
    private ThumbnailDetailDto medium;            // "medium"
    private ThumbnailDetailDto high;              // "high"
    private ThumbnailDetailDto standard;          // "standard"
    private ThumbnailDetailDto maxres;            // "maxres"
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ThumbnailDetailDto {
        private String url;
        private Integer width;
        private Integer height;
    }
}
