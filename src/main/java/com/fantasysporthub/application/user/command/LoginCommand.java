package com.fantasysporthub.application.user.command;

import com.fantasysporthub.cqrs.command.Command;

import java.util.UUID;

public record LoginCommand(
        String email,
        String password,
        String deviceFingerprint
) implements Command<String> {

    @Override
    public UUID getCommandId() {
        return null;
    }

    @Override
    public UUID getUserId() {
        return null;
    }

    @Override
    public String getCommandType() {
        return "";
    }
}
