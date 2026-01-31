package com.fantasysporthub.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converter to read PostgreSQL JSONB into Jackson JsonNode.
 */
@Slf4j
@ReadingConverter
public class JsonNodeReadConverter implements Converter<Json, JsonNode> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public JsonNode convert(Json source) {
        try {
            return OBJECT_MAPPER.readTree(source.asString());
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSONB to JsonNode: {}", source.asString(), e);
            return OBJECT_MAPPER.createArrayNode();
        }
    }
}