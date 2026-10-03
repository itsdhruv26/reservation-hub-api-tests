package com.reservationhub.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservationhub.model.Booking;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a valid booking into a deliberately broken JSON payload: one field removed, or one field
 * set to a value of the wrong type. Paths use dot notation for nested fields, e.g. "bookingdates.checkin".
 */
public final class Payloads {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Payloads() {
    }

    public static Map<String, Object> without(Booking booking, String path) {
        Map<String, Object> payload = toMap(booking);
        parentOf(payload, path).remove(leafOf(path));
        return payload;
    }

    public static Map<String, Object> with(Booking booking, String path, Object value) {
        Map<String, Object> payload = toMap(booking);
        parentOf(payload, path).put(leafOf(path), value);
        return payload;
    }

    private static Map<String, Object> toMap(Booking booking) {
        return MAPPER.convertValue(booking, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parentOf(Map<String, Object> root, String path) {
        Map<String, Object> node = root;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            node = (Map<String, Object>) node.get(parts[i]);
        }
        return node;
    }

    private static String leafOf(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
