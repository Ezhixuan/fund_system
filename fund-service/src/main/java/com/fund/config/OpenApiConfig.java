package com.fund.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("基金交易系统API")
                        .description("基金交易决策辅助系统接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Fund System")
                                .email("support@fund.com"))
                        .license(new License()
                                .name("MIT License")));
    }
}
