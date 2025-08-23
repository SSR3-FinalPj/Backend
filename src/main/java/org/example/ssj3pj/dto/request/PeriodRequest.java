package org.example.ssj3pj.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 기간 공통 요청 DTO (YYYY-MM-DD) */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PeriodRequest {
    private String startDate;
    private String endDate;
}
