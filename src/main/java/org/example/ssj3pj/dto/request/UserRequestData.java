package org.example.ssj3pj.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;


//redis 스케줄링 데이터 저장 테이블
@Data
@AllArgsConstructor
public class UserRequestData {
    private Long userId;
    private String imageKey;
}
