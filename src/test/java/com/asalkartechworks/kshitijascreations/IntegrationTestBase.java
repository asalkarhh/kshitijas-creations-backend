package com.asalkartechworks.kshitijascreations;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kshitijas_creations;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.jwt.secret=test-jwt-secret-with-at-least-thirty-two-bytes-123456789",
        "app.jwt.expiration=PT1H",
        "app.bootstrap.admin-email=admin@kshitijascreations.test",
        "app.bootstrap.admin-password=CorrectPassword123!",
        "app.cors.allowed-origins=http://localhost:3000",
        "app.storage.upload-directory=target/test-uploads",
        "app.whatsapp.admin-number=919876543210",
        "app.media.public-base-url=http://localhost:8080/media",
        "app.security.order-rate-limit-per-hour=1000"
})
public abstract class IntegrationTestBase {
}
