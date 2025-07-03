package com.firmament.immigration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ImmigrationBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ImmigrationBackendApplication.class, args);
	}

}
