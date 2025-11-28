package com.fantasysporthub.eventsourcing.config;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.eventstore.dbclient.NodePreference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EventStoreDB client configuration.
 * Provides reactive-compatible client with connection pooling.
 */
@Configuration
public class EventStoreConfig {

    @Value("${eventstore.connection-string}")
    private String connectionString;

    @Value("${eventstore.max-discovery-attempts:10}")
    private int maxDiscoveryAttempts;

    @Value("${eventstore.discovery-interval-ms:1000}")
    private int discoveryInternal;

    @Bean
    public EventStoreDBClient eventStoreDBClient() {
        // Parse connection string and create settings with additional configuration
        EventStoreDBClientSettings baseSettings = EventStoreDBConnectionString.parseOrThrow(connectionString);

        // Build enhanced settings with discovery configuration
        // Note: EventStoreDB client handles threading internally via gRPC
        EventStoreDBClientSettings configuredSettings = EventStoreDBClientSettings.builder()
                .addHost(baseSettings.getHosts()[0])
                .tls(baseSettings.isTls())
                .tlsVerifyCert(baseSettings.isTlsVerifyCert())
                .maxDiscoverAttempts(maxDiscoveryAttempts)
                .discoveryInterval(discoveryInternal)
                .nodePreference(NodePreference.LEADER)
                .buildConnectionSettings();

        return EventStoreDBClient.create(configuredSettings);
    }

}
