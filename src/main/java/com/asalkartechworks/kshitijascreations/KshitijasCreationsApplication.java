package com.asalkartechworks.kshitijascreations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@ConfigurationPropertiesScan
@SpringBootApplication
public class KshitijasCreationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(KshitijasCreationsApplication.class, args);
    }
}
