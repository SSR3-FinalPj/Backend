// src/main/java/.../es/IndexResolver.java
package org.example.ssj3pj.es;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.config.EsIndexProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IndexResolver {

    private final EsIndexProperties props;

    public String resolve(IndexType type) {
        return switch (type) {
            case CITYDATA -> props.getCitydata();
            case REDDIT   -> props.getReddit();
            case YOUTUBE  -> props.getYoutube();
        };
    }
}
