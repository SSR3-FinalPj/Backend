package org.example.ssj3pj.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * YouTube 비디오 업로드 요청 데이터
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeUploadRequestDto {

    /** 비디오 제목 (필수, 최대 100자) */
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요")
    private String title;

    /** 비디오 설명 (선택) */
    @Size(max = 5000, message = "설명은 5000자 이내로 입력해주세요")
    private String description;

    /** 비디오 태그 목록 (선택) */
    private List<String> tags;

    /** 공개 설정: private, unlisted, public */
    @Builder.Default
    private String privacyStatus = "private";

    /** YouTube 카테고리 ID (예: 22는 People & Blogs) */
    private String categoryId;

    /** 어린이 대상 콘텐츠 여부 */
    @Builder.Default
    private Boolean madeForKids = false;
}