/*
package org.example.ssj3pj.dto.youtube;

import lombok.*;
import org.example.ssj3pj.entity.YoutubeMetadata;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeWithDetailsDto {
    
    // DB 메타데이터 필드들
    private Long id;
    private String youtubeId;
    private String esDocId;
    private String title;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime publishedAt;
    private LocalDateTime indexedAt;
    
    // ES 데이터 필드들
    private String channelTitle;
    private String channelId;
    private String description;
    private String[] tags;
    private String categoryId;
    private YoutubeThumbnailDto thumbnailUrl;
    private List<YoutubeCommentDto> comments;
    private String videoPlayer;
    private String uploadDate;
    
    // 헬퍼 메서드: metadata와 esData로부터 생성
    public static YoutubeWithDetailsDto fromMetadataAndEs(YoutubeMetadata metadata, YoutubeSummaryDto esData) {
        YoutubeWithDetailsDto dto = YoutubeWithDetailsDto.builder()
                .id(metadata.getId())
                .youtubeId(metadata.getYoutubeId())
                .esDocId(metadata.getEsDocId())
                .title(metadata.getTitle())
                .viewCount(metadata.getViewCount())
                .likeCount(metadata.getLikeCount())
                .commentCount(metadata.getCommentCount())
                .publishedAt(metadata.getPublishedAt())
                .indexedAt(metadata.getIndexedAt())
                .build();
        
        // ES 데이터가 있으면 추가
        if (esData != null) {
            dto.setChannelTitle(esData.getChannelTitle());
            dto.setChannelId(esData.getChannelId());
            dto.setDescription(esData.getDescription());
            dto.setTags(esData.getTags());
            dto.setCategoryId(esData.getCategoryId());
            dto.setThumbnailUrl(esData.getThumbnailUrl());
            dto.setComments(esData.getComments());
            dto.setVideoPlayer(esData.getVideoPlayer());
            dto.setUploadDate(esData.getUploadDate());
        }
        
        return dto;
    }
}
*/
