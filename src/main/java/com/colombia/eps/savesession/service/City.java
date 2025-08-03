package com.colombia.eps.savesession.service;

import com.colombia.eps.savesession.helper.AuxiliaryMethods;
import com.colombia.eps.savesession.helper.Constants;
import com.colombia.eps.savesession.service.client.GeolocationRestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class City {
    private final ObjectMapper objectMapper;
    private final GeolocationRestClient geolocationClient;
    private static final Logger LOG = Logger.getLogger(City.class);

    public City(
            ObjectMapper objectMapper,
            @RestClient GeolocationRestClient geolocationClient
    ) {
        this.objectMapper = objectMapper;
        this.geolocationClient = geolocationClient;
    }

    public String getCity(String ip) {
        try {
            int attempts = 0;
            while (attempts <= 3) {
                attempts++;
                LOG.debug(String.format("Intento %s de obtener ciudad", attempts));
                String response = geolocationClient.getLocationData(ip, "city,status,message");

                LOG.debug(String.format("Respuesta API: %s", response));

                JsonNode responseJsonNode = objectMapper.readTree(response);
                String status = responseJsonNode.get("status").asText();
                if ("success".equals(status)) {
                    return AuxiliaryMethods.getStringFromJson(responseJsonNode, Constants.CITY).orElse(Constants.CITY_NOT_AVAILABLE);

                } else {
                    String message = AuxiliaryMethods.getStringFromJson(responseJsonNode, "message").orElse("Error desconocido");
                    LOG.error(String.format("Error en API: %s", message));
                }
            }
            return Constants.CITY_NOT_AVAILABLE;

        } catch (Exception e) {
            LOG.error(String.format("Exception en getCityFromIp: %s", e.getMessage()));
            Thread.currentThread().interrupt();
            return Constants.CITY_NOT_AVAILABLE;
        }
    }
}
