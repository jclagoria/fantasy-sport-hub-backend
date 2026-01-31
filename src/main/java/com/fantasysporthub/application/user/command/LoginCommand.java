package com.fantasysporthub.application.user.command;

import com.fantasysporthub.cqrs.command.Command;
import lombok.Builder;

import java.util.UUID;

@Builder
public record LoginCommand(
        UUID commandId,
        String email,
        String password,
        String deviceFingerprint,
        String ipAddress,
        String userAgent
) implements Command<String> {

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
        return "Login";
    }
}
