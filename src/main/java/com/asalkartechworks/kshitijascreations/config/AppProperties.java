package com.asalkartechworks.kshitijascreations.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.unit.DataSize;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Bootstrap bootstrap = new Bootstrap();
    private final Whatsapp whatsapp = new Whatsapp();
    private final Storage storage = new Storage();
    private final Media media = new Media();
    private final Cors cors = new Cors();
    private final Swagger swagger = new Swagger();
    private final Security security = new Security();
    @Setter
    private String publicSiteUrl;

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private Duration expiration = Duration.ofHours(8);
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String adminEmail;
        private String adminPassword;
    }

    @Getter
    @Setter
    public static class Whatsapp {
        private String adminNumber;
    }

    @Getter
    @Setter
    public static class Storage {
        private String uploadDirectory;
    }

    @Getter
    @Setter
    public static class Media {
        private String publicBaseUrl;
        private DataSize maxImageSize = DataSize.ofMegabytes(5);
        private DataSize maxVideoSize = DataSize.ofMegabytes(30);
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    @Getter
    @Setter
    public static class Swagger {
        private boolean enabled = false;
    }

    @Getter
    @Setter
    public static class Security {
        private int loginMaxAttempts = 5;
        private int loginCooldownMinutes = 15;
        private int orderRateLimitPerHour = 10;
    }
}
