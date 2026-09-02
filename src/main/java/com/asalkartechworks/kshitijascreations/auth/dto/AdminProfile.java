package com.asalkartechworks.kshitijascreations.auth.dto;

import com.asalkartechworks.kshitijascreations.domain.entity.AdminUser;
import com.asalkartechworks.kshitijascreations.domain.enums.AdminRole;

public record AdminProfile(
        Long id,
        String name,
        String email,
        AdminRole role
) {

    public static AdminProfile from(AdminUser admin) {
        return new AdminProfile(admin.getId(), admin.getName(), admin.getEmail(), admin.getRole());
    }
}
