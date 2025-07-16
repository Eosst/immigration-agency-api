package com.firmament.immigration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Development server");

        Contact contact = new Contact();
        contact.setEmail("contact@firmamentimmigration.ca");
        contact.setName("Firmament Immigration");
        contact.setUrl("https://firmamentimmigration.ca");

        Info info = new Info()
                .title("Firmament Immigration API")
                .version("1.0")
                .contact(contact)
                .description("API for managing immigration consultation appointments");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
}