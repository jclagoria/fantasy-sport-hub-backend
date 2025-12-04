package com.fantasysporthub.domain.match.command;

import com.fantasysporthub.cqrs.command.Command;
import lombok.Value;

import java.util.UUID;

@Value
public class StartMatchCommand implements Command {

    UUID commandId;
    UUID userId;
    UUID matchId;
    UUID homeTeamId;
    UUID awayTeamId;
    String sportId;
    String providerId;

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
        return "StartMatch";
    }
}
