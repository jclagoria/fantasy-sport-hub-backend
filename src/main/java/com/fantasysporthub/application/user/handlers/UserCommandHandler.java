package com.fantasysporthub.application.user.handlers;

import com.fantasysporthub.application.user.command.LoginCommand;
import com.fantasysporthub.application.user.command.LogoutCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Handler for user authentication commands.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCommandHandler {

    /**
     * Handle login command.
     */
    public Mono<String> handelLogin(LoginCommand command) {
        // TODO Phase 1 implementation
        return Mono.error(new UnsupportedOperationException("Phase 1 implementation required"));
    }

    /**
     * Handle logout command.
     */
    public Mono<Void> handleLogout(LogoutCommand command) {
        // TODO Phase 1 implementation
        return Mono.error(new UnsupportedOperationException("Phase 1 implementation required"));
    }

}
