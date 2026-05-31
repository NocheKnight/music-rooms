package ru.music.gateway.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Room API Portal")
                        .description("Единая точка входа для всех API микросервисов Music Room")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Music Room Team")));
    }
}