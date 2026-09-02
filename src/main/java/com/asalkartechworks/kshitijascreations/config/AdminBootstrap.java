package com.asalkartechworks.kshitijascreations.config;

import com.asalkartechworks.kshitijascreations.domain.entity.AdminUser;
import com.asalkartechworks.kshitijascreations.domain.enums.AdminRole;
import com.asalkartechworks.kshitijascreations.repository.AdminUserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }

        String email = appProperties.getBootstrap().getAdminEmail();
        String password = appProperties.getBootstrap().getAdminPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("No admin exists; set ADMIN_EMAIL and ADMIN_PASSWORD to create the initial account");
            return;
        }

        AdminUser admin = new AdminUser(
                "Administrator",
                email.trim().toLowerCase(Locale.ROOT),
                passwordEncoder.encode(password),
                AdminRole.ADMIN
        );
        adminUserRepository.save(admin);
        log.info("Created initial admin account for {}", admin.getEmail());
    }
}
