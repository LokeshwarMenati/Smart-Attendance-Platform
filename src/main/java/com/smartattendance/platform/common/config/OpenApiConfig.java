package com.smartattendance.platform.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI smartAttendanceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Attendance Platform API")
                        .description("Production-grade Employee Attendance Management Platform with Clock-in/Clock-out and Reports")
                        .version("1.0.0")
                        .contact(new Contact().name("Engineering Team").email("engineering@smartattendance.com"))
                        .license(new License().name("Apache 2.0").url("https://spring.io/projects/spring-boot")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT bearer token without the 'Bearer ' prefix.")));
    }
}

