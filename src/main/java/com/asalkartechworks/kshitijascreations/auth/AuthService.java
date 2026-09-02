package com.asalkartechworks.kshitijascreations.auth;

import com.asalkartechworks.kshitijascreations.auth.dto.AdminProfile;
import com.asalkartechworks.kshitijascreations.auth.dto.LoginRequest;
import com.asalkartechworks.kshitijascreations.auth.dto.LoginResponse;
import com.asalkartechworks.kshitijascreations.domain.entity.AdminUser;
import com.asalkartechworks.kshitijascreations.repository.AdminUserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final JwtService jwtService;
    private final LoginProtectionService loginProtectionService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (loginProtectionService.isLocked(email)) {
            throw new BadCredentialsException("Invalid email or password");
        }

        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
            );
            loginProtectionService.recordSuccessfulLogin(email);
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginProtectionService.recordFailedAttempt(email);
            throw e;
        }

        AdminUser admin = activeAdmin(email);
        JwtService.JwtToken token = jwtService.generateToken(admin);
        return new LoginResponse("Bearer", token.value(), token.expiresAt(), AdminProfile.from(admin));
    }

    @Transactional(readOnly = true)
    public AdminProfile me(String email) {
        return AdminProfile.from(activeAdmin(email));
    }

    private AdminUser activeAdmin(String email) {
        return adminUserRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .orElseThrow(() -> new BadCredentialsException("Admin user is unavailable"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
