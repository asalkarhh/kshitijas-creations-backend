package com.asalkartechworks.kshitijascreations.auth;

import com.asalkartechworks.kshitijascreations.config.AppProperties;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginProtectionService {

    private final int maxAttempts;
    private final int cooldownMinutes;
    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public LoginProtectionService(AppProperties properties) {
        this.maxAttempts = properties.getSecurity().getLoginMaxAttempts();
        this.cooldownMinutes = properties.getSecurity().getLoginCooldownMinutes();
    }

    public boolean isLocked(String email) {
        LoginAttempt attempt = attempts.get(email);
        if (attempt == null) {
            return false;
        }
        if (attempt.count >= maxAttempts) {
            if (Instant.now().isBefore(attempt.lockedUntil)) {
                return true;
            } else {
                attempts.remove(email);
                return false;
            }
        }
        return false;
    }

    public void recordFailedAttempt(String email) {
        attempts.compute(email, (k, v) -> {
            if (v == null) {
                return new LoginAttempt(1, null);
            }
            int newCount = v.count + 1;
            Instant lockedUntil = null;
            if (newCount >= maxAttempts) {
                lockedUntil = Instant.now().plusSeconds(cooldownMinutes * 60L);
            }
            return new LoginAttempt(newCount, lockedUntil);
        });
    }

    public void recordSuccessfulLogin(String email) {
        attempts.remove(email);
    }

    private record LoginAttempt(int count, Instant lockedUntil) {}
}

