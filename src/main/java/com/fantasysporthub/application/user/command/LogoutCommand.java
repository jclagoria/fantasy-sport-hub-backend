package com.fantasysporthub.application.user.command;

import com.fantasysporthub.cqrs.command.Command;

import java.util.UUID;

/**
 * Command to logout user by blacklisting JWT.
 */
public record LogoutCommand() implements Command<Void> {
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
