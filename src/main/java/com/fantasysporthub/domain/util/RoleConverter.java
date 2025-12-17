package com.fantasysporthub.domain.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Utility for converting roles between JSONB, Java collections, and JWT claims.
 */
@Slf4j
public class RoleConverter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Convert JsonNode (JSONB) to List<String> for JWT claims.
     * Handles: arrays, single values, null gracefully.
     *
     * @param rolesJson JsonNode
     * @return List of role strings
     */
    public static List<String> jsonNodeToList(JsonNode rolesJson) {
        if (rolesJson == null || rolesJson.isNull()) {
            return Collections.emptyList();
        }

        if (rolesJson.isArray()) {
            return StreamSupport.stream(rolesJson.spliterator(), false)
                    .map(JsonNode::asText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        if (rolesJson.isTextual()) {
            return List.of(rolesJson.asText());
        }

        log.warn("Unexpected JsonNode type for roles: {}", rolesJson.getNodeType());
        return Collections.emptyList();
    }

    /**
     *  Convert JsonNode to Set<String> for domain models.
     *
     * @param rolesNode JsonNode
     * @return Set of role strings
     */
    public static Set<String> jsonNodeToSet(JsonNode rolesNode) {
        return new HashSet<>(jsonNodeToList(rolesNode));
    }

    /**
     * Convert JWT claim object to Set of roles.
     * Handles: List, String, Array, or null.
     *
     * @param rolesClaim Object from JWT claims
     * @return Set of role strings
     */
    public static Set<String> claimToSet(Object rolesClaim) {
        if (rolesClaim == null) {
            return Collections.emptySet();
        }

        // Handle List<String> from JWT (most common case)
        if (rolesClaim instanceof List<?> rolesList) {
            return rolesList.stream()
                    .filter(Objects::nonNull)
                    .map(Objects::toString)
                    .collect(Collectors.toSet());
        }

        // Handle single role as String
        if (rolesClaim instanceof String singleRole) {
            return Set.of(singleRole);
        }

        // Handle array (rare case)
        if (rolesClaim instanceof String[] arr) {
            return Arrays.stream(arr)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        log.warn("Unexpected claim type for roles: {}", rolesClaim.getClass());
        return Collections.emptySet();
    }

    /**
     * Convert Collection of roles to JSONB JsonNode for persistence.
     *
     * @param roles Collection of role strings
     * @return JsonNode for PostgreSQL JSONB column
     */
    public static JsonNode collectionToJsonNode(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return OBJECT_MAPPER.createArrayNode();
        }

        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        roles.forEach(arrayNode::add);
        return arrayNode;
    }

    /**
     * Sanitize roles collection by removing invalid entries.
     *
     * @param roles Collection of roles
     * @return Set of valid roles only
     */
    public static Set<String> sanitizeRoles(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        return roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toSet());
    }
}
