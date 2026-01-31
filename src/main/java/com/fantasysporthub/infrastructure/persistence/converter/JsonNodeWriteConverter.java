package com.fantasysporthub.infrastructure.persistence.converter;

import com.fasterxml.jackson.databind.JsonNode;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converter to write Jackson JsonNode to PostgreSQL JSONB.
 */
@WritingConverter
public class JsonNodeWriteConverter implements Converter<JsonNode, Json> {

    @Override
    public Json convert(JsonNode source) {
        return Json.of(source.toString());
    }
}
