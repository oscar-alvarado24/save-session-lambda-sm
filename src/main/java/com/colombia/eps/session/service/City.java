package com.colombia.eps.session.service;

import com.colombia.eps.session.helper.AuxiliaryMethods;
import com.colombia.eps.session.helper.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RequiredArgsConstructor
@Slf4j
public class City {
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public String getCity(String ip){
        try {
            int attempts = 0;
            while (attempts <=3) {
                attempts++;
                log.debug("Intento {} de obtener ciudad", attempts);
                HttpResponse<String> response = getApiCityResponse(ip);

                log.debug("Respuesta API status: {}", response.statusCode());
                log.debug("Respuesta API body: {}", response.body());

                if (response.statusCode() == 200) {
                    JsonNode responseJsonNode = objectMapper.readTree(response.body());
                    String status = responseJsonNode.get("status").asText();

                    if ("success".equals(status)) {
                        return AuxiliaryMethods.getStringFromJson(responseJsonNode, Constants.CITY).orElse(Constants.CITY_NOT_AVAILABLE);

                    } else {
                        String message = AuxiliaryMethods.getStringFromJson(responseJsonNode, "message").orElse("Error desconocido");
                        log.error("Error en API: {}", message);
                    }
                }
            }
            return Constants.CITY_NOT_AVAILABLE;

        }  catch (Exception e) {
            log.error("Exception en getCityFromIp: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return Constants.CITY_NOT_AVAILABLE;
        }
    }

    private HttpResponse<String> getApiCityResponse(String ip) throws IOException, InterruptedException {
        log.debug("Llamando API de geolocalización para: {}", ip);

        String apiUrl = String.format(Constants.API_URL, ip);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();
        return client.send(request,HttpResponse.BodyHandlers.ofString());
    }
}
