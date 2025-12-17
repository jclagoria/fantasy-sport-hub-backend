package com.fantasysporthub.application.user.query;

import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * Check if user can authenticate.
 */
@Data
@Builder
public class UserAuthenticationProjection {

    private UUID userId;
    private String email;
    private String displayName;
    private Set<String> roles;
    private String accountStatus;

    public boolean canAuthenticate() {
        return "Active".equals(accountStatus);
    }

}
