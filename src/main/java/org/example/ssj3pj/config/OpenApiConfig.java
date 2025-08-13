package org.example.ssj3pj.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI baseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Feedback Loop Dashboard API")
                        .version("v1.0.0")
                        .description("실시간 관제/리포트용 백엔드 대시보드 API"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components().addSecuritySchemes(
                        "BearerAuth",
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }

    // (선택) 경로 그룹화
    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder().group("api").pathsToMatch("/api/**").build();
    }
    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder().group("admin").pathsToMatch("/admin/**").build();
    }
}
