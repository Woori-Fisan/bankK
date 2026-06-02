package com.woorifisan.monitoring.domain.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String loginId;
    private String passwordHash;
    private String name;
    private boolean isActive;

    public static User of(String loginId, String passwordHash, String name) {
        return User.builder()
                .loginId(loginId)
                .passwordHash(passwordHash)
                .name(name)
                .isActive(true)
                .build();
    }
}
