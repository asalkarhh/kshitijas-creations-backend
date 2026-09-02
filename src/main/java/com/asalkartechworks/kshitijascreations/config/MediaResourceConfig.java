package com.asalkartechworks.kshitijascreations.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    private final String uploadLocation;

    public MediaResourceConfig(AppProperties appProperties) {
        Path root = Path.of(appProperties.getStorage().getUploadDirectory()).toAbsolutePath().normalize();
        this.uploadLocation = root.toUri().toString() + (root.toUri().toString().endsWith("/") ? "" : "/");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**", "/media/**").addResourceLocations(uploadLocation);
    }
}
