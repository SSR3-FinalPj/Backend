package org.example.ssj3pj.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "app.es.indices")
public class EsIndexProperties {
    private String citydata;
    private String reddit;
    private String youtube;
}
