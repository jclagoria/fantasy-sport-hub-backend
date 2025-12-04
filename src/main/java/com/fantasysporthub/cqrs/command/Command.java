package com.fantasysporthub.cqrs.command;

import java.util.UUID;

/**
 * Base interface for all commands in the system.
 * Commands represent user intentions to change state.
 */
public interface Command {

    UUID getCommandId();
    UUID getUserId();
    String getCommandType();

}
