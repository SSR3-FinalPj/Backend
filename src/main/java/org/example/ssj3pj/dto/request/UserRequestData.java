package org.example.ssj3pj.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRequestData {
    private Long userId;
    private String imageKey;
    private String locationCode; // ✅ 추가
}
