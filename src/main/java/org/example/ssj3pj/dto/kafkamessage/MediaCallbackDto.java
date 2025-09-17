package org.example.ssj3pj.dto.kafkamessage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaCallbackDto {
    private String imageKey;
    private String resultKey;
    private String type;
}
