package com.colombia.eps;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

public final class AuxiliaryMethods {
    private AuxiliaryMethods(){
        throw new IllegalCallerException();
    }

    public static Optional<String> parseJson(String requestBody, ObjectMapper objectMapper){
        try {
            String bodyJson = String.valueOf(objectMapper.readTree(requestBody));
            return Optional.ofNullable(bodyJson);
        } catch (Exception e) {
            context.getLogger().log("Error parseando JSON: " + e.getMessage());
            return createResponse(400, "{\"error\": \"Formato JSON inválido\"}");
        }
    }
}
