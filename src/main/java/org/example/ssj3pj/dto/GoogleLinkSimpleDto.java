// src/main/java/org/example/ssj3pj/dto/google/GoogleLinkSimpleDto.java
package org.example.ssj3pj.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class GoogleLinkSimpleDto {
    private boolean linked; // true = refreshToken 존재
}
