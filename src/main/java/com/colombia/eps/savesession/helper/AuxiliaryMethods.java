package com.colombia.eps.savesession.helper;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuxiliaryMethods {
    public static Optional<String> getStringFromJson(JsonNode jsonNode, String fieldName) {
        JsonNode field = jsonNode.get(fieldName);
        if (field != null && !field.isNull()) {
            String fieldValue = field.asText();
            if (fieldValue.trim().isBlank()){
                return Optional.empty();
            }
            return Optional.of(fieldValue);
        }
        return Optional.empty();
    }

    // Validación básica de formato IP
    public static boolean isValidIpFormat(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Regex básico para IPv4 (versión más legible)
        String octet = "(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)";
        String ipv4Pattern = "^(" + octet + "\\.){3}" + octet + "$";

        // Regex básico para IPv6 (simplificado)
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$";

        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern);
    }
}
