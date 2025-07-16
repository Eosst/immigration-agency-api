package com.firmament.immigration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableJpaAuditing
public class ImmigrationBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImmigrationBackendApplication.class, args);
	}

	// Log all endpoints during startup
	public void logEndpoints(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping) {
		handlerMapping.getHandlerMethods().forEach((key, value) -> {
			System.out.println("Endpoint: " + key + " -> " + value);
		});
	}
}