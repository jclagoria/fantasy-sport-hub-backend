package com.fantasysporthub.application.user.command;

import com.fantasysporthub.cqrs.command.Command;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RegisterUserCommand(
        UUID commandId,
        String email,
        String password,
        String displayName,
        String ipAddress,
        String userAgent
) implements Command<UUID> {

    @Override
    public UUID getCommandId() {
        return commandId;
    }

    @Override
    public UUID getUserId() {
        return null;
    }

    @Override
    public String getCommandType() {
        return "RegisterUser";
    }
}
