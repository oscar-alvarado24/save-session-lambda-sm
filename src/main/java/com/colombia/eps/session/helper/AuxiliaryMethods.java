package com.colombia.eps.session.helper;

import com.colombia.eps.session.helper.exceptions.GetAndValidateException;
import com.colombia.eps.session.helper.exceptions.GetValueParamException;
import com.colombia.eps.session.helper.exceptions.ParseJsonException;
import com.colombia.eps.session.model.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class AuxiliaryMethods {

    public static Optional<JsonNode> parseJson(String requestBody, ObjectMapper objectMapper){
        try {
            return Optional.ofNullable(objectMapper.readTree(requestBody));
        } catch (Exception e) {
            log.error("Error parseando JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public static String getAndValidateParams(JsonNode bodyJson, String param){
        try{
            return AuxiliaryMethods.getStringFromJson(bodyJson, param).orElseThrow(()->new GetValueParamException(String.format(Constants.MSG_ERROR_WITH_PARAM, param)));
        } catch (Exception exception){
            throw new GetAndValidateException(exception.getMessage());
        }
    }

    public static Map<String, String> getHeaders(String urlFront) {
        Map<String, String> headers =  new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", urlFront);
        headers.put("Access-Control-Allow-Methods", "POST");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        return headers;
    }

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
