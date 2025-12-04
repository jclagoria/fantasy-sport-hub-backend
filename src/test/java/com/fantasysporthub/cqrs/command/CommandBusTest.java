package com.fantasysporthub.cqrs.command;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CommandBusTest {

    @Test
    void shouldRegisterAndDispatchCommand() {
        // Given
        var commandBus = new CommandBus();
        var handler = new TestCommandHandler();
        commandBus.register(TestCommand.class, handler);

        var command = new TestCommand(UUID.randomUUID(), UUID.randomUUID());

        // When
        var result = commandBus.dispatch(command);

        // Then
        StepVerifier.create(result)
                .assertNext(r -> assertThat(r).isEqualTo("success"))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenNoHandlerRegistered() {
        // Given
        var commandBus = new CommandBus();
        var command = new TestCommand(UUID.randomUUID(), UUID.randomUUID());

        // When
        var result = commandBus.dispatch(command);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(e ->
                        e instanceof IllegalStateException &&
                                e.getMessage().contains("No handler registered")
                )
                .verify();
    }

    // Test classes
    record TestCommand(UUID commandId, UUID userId) implements Command {
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
            return "TestCommand";
        }
    }

    static class TestCommandHandler implements CommandHandler<TestCommand, String> {
        @Override
        public Mono<String> handle(TestCommand command) {
            return Mono.just("success");
        }
    }

}