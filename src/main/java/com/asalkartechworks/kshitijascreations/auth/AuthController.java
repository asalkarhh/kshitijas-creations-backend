package com.asalkartechworks.kshitijascreations.auth;

import com.asalkartechworks.kshitijascreations.auth.dto.AdminProfile;
import com.asalkartechworks.kshitijascreations.auth.dto.LoginRequest;
import com.asalkartechworks.kshitijascreations.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AdminProfile me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
