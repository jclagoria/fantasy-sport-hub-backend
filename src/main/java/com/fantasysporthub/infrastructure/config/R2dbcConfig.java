package com.fantasysporthub.infrastructure.config;

import com.fantasysporthub.domain.model.ManagedPersistable;
import com.fantasysporthub.infrastructure.persistence.converter.JsonNodeReadConverter;
import com.fantasysporthub.infrastructure.persistence.converter.JsonNodeWriteConverter;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * R2DBC configuration with custom type converters.
 */
@Configuration
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    private final ConnectionFactory connectionFactory;

    public R2dbcConfig(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public ConnectionFactory connectionFactory() {
        return connectionFactory;
    }

    @Bean
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return R2dbcCustomConversions.of(
                dialect,
                List.of(
                        new JsonNodeReadConverter(),
                        new JsonNodeWriteConverter()
                )
        );
    }

    /**
     * Generic callback to mark all ManagedPersistable entities as existing after loading from database.
     * <p>
     * R2DBC does not populate @Transient fields when loading entities, so the isNew flag
     * retains its default value (true). This callback sets isNew=false for all loaded entities,
     * ensuring that subsequent save() calls perform UPDATE instead of INSERT.
     * <p>
     * Any new entity implementing ManagedPersistable is automatically handled.
     */
    @Bean
    public AfterConvertCallback<ManagedPersistable<?>> managedPersistableAfterConvertCallback() {
        return (entity, table) -> {
            entity.markAsExisting();
            return Mono.just(entity);
        };
    }
}
