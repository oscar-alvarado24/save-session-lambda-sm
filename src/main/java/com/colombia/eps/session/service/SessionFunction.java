package com.colombia.eps.session.service;

import com.colombia.eps.session.helper.AuxiliaryMethods;
import com.colombia.eps.session.helper.Constants;
import com.colombia.eps.session.helper.exceptions.ParseJsonException;
import com.colombia.eps.session.repository.DynamoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFunction implements Function<Map<String, Object>, Map<String, Object>> {

    private final String urlFront=System.getenv("URL_FRONT");
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DynamoRepository dynamoRepository;

    @Override
    public Map<String, Object> apply(Map<String, Object> input) {


        log.debug("=== Iniciando función Lambda ===");

        try {
            // Extraer el body del evento de API Gateway
            String requestBody = (String) input.get("body");
            log.debug("Request body: {}", requestBody);

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return AuxiliaryMethods.createErrorResponse(400, "Cuerpo de la petición es requerido", this.urlFront);
            }

            // Parsear el JSON del cuerpo
            JsonNode bodyJson = AuxiliaryMethods.parseJson(requestBody, objectMapper)
                    .orElseThrow(() -> new ParseJsonException(Constants.MSG_PARSE_JSON));

            // Extraer parámetros del JSON
            String email = AuxiliaryMethods.getStringFromJson(bodyJson, "email");
            String clientIp = AuxiliaryMethods.getStringFromJson(bodyJson, "ip");

            log.debug("Email: {}, IP: {}", email, clientIp);

            // Validar parámetros requeridos
            if (email == null || email.trim().isEmpty()) {
                return AuxiliaryMethods.createErrorResponse(400, "Parámetro 'email' es requerido", this.urlFront);
            }

            if (clientIp == null || clientIp.trim().isEmpty()) {
                return AuxiliaryMethods.createErrorResponse(400, "Parámetro 'ip' es requerido", this.urlFront);
            }

            // Validar formato de IP básico
            if (!AuxiliaryMethods.isValidIpFormat(clientIp.trim())) {
                return AuxiliaryMethods.createErrorResponse(400, "Formato de IP inválido", this.urlFront);
            }

            log.debug("Obteniendo ciudad para IP: {}", clientIp.trim());
            // Obtener información de geolocalización
            String city = getCityFromIp(clientIp.trim());

            // Obtener timestamp actual
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));


            // Guardar en DynamoDB usando email como sessionId
            dynamoRepository.saveSession(email.trim(), clientIp.trim(), city, timestamp);

            log.debug("=== Función completada exitosamente ===");

            // Respuesta exitosa
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "Conexión registrada exitosamente");
            responseData.put("sessionId", email.trim());
            responseData.put("ip", clientIp.trim());
            responseData.put("city", city);
            responseData.put("timestamp", timestamp);

            // Convertir a JsonNode
            JsonNode responseBody = objectMapper.valueToTree(responseData);

            return AuxiliaryMethods.createSuccessResponse( responseBody, this.urlFront);

        } catch (Exception e) {
            log.error("ERROR: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("Stack trace: ", e);
            return AuxiliaryMethods.createErrorResponse(500, "Error interno del servidor: " + e.getMessage(), this.urlFront);
        }
    }

    private String getCityFromIp(String ip) {
        try {
            log.debug("Llamando API de geolocalización para: {}", ip);

            String apiUrl = "http://ip-api.com/json/" + ip + "?fields=city,status,message";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            log.debug("Respuesta API status: {}", response.statusCode());
            log.debug("Respuesta API body: {}", response.body());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                String status = jsonNode.get("status").asText();

                if ("success".equals(status)) {
                    String city = jsonNode.get("city").asText();
                    return (city != null && !city.isEmpty()) ? city : "Ciudad no disponible";
                } else {
                    String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "Error desconocido";
                    log.error("Error en API: {}", message);
                    return "Error: " + message;
                }
            }

            return "Servicio no disponible";

        } catch (IOException e) {
            log.error("IOException en getCityFromIp: {}", e.getMessage());
            return "Error de conexión";
        } catch (InterruptedException e) {
            log.error("InterruptedException en getCityFromIp: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return "Operación interrumpida";
        } catch (Exception e) {
            log.error("Exception en getCityFromIp: {}", e.getMessage());
            return "Error desconocido";
        }
    }

}