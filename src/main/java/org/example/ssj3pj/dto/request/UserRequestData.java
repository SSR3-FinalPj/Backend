package org.example.ssj3pj.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestData {
    private Long jobId;
    private Long userId;
    private String imageKey;
    private String locationCode;
}
