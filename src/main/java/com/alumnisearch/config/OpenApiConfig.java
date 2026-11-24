package com.alumnisearch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alumniOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Alumni LinkedIn Profile Searcher API")
                .version("1.0.0")
                .description("Search LinkedIn alumni of a university by designation (and optional pass-out year) "
                        + "through PhantomBuster, and persist the results in PostgreSQL."));
    }
}
