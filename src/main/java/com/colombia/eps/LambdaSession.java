package com.colombia.eps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class LambdaSession implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = System.getenv("DYNAMO_TABLE_NAME");

    private final AmazonDynamoDB dynamoClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LambdaSession() {
        // Cliente DynamoDB usando SDK v1
        this.dynamoClient = AmazonDynamoDBClientBuilder.defaultClient();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        context.getLogger().log("=== Iniciando función Lambda ===");

        try {
            // Obtener parámetros de la URL
            Map<String, String> queryParams = event.getQueryStringParameters();
            context.getLogger().log("Query params: " + queryParams);

            if (queryParams == null) {
                return createResponse(400, "{\"error\": \"Parámetros requeridos: email e ip\"}");
            }

            String email = queryParams.get("email");
            String clientIp = queryParams.get("ip");

            context.getLogger().log("Email: " + email + ", IP: " + clientIp);

            // Validar parámetros requeridos
            if (email == null || email.trim().isEmpty()) {
                return createResponse(400, "{\"error\": \"Parámetro 'email' es requerido\"}");
            }

            if (clientIp == null || clientIp.trim().isEmpty()) {
                return createResponse(400, "{\"error\": \"Parámetro 'ip' es requerido\"}");
            }

            // Validar formato de IP básico
            if (!isValidIpFormat(clientIp.trim())) {
                return createResponse(400, "{\"error\": \"Formato de IP inválido\"}");
            }

            context.getLogger().log("Obteniendo ciudad para IP: " + clientIp.trim());
            // Obtener información de geolocalización
            String city = getCityFromIp(clientIp.trim(), context);

            // Obtener timestamp actual
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            context.getLogger().log("Guardando en DynamoDB - Table: " + TABLE_NAME);
            // Guardar en DynamoDB usando email como sessionId
            saveConnectionRecord(email.trim(), clientIp.trim(), city, timestamp, context);

            context.getLogger().log("=== Función completada exitosamente ===");
            // Respuesta exitosa
            return createResponse(200,
                    String.format("{\"message\": \"Conexión registrada exitosamente\", \"sessionId\": \"%s\", \"ip\": \"%s\", \"city\": \"%s\", \"timestamp\": \"%s\"}",
                            email.trim(), clientIp.trim(), city, timestamp));

        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            context.getLogger().log("Stack trace:");
            for (StackTraceElement element : e.getStackTrace()) {
                context.getLogger().log("  " + element.toString());
            }
            return createResponse(500, "{\"error\": \"Error interno del servidor: " + e.getMessage() + "\"}");
        }
    }

    // Validación básica de formato IP
    private boolean isValidIpFormat(String ip) {
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

    private String getCityFromIp(String ip, Context context) {
        try {
            context.getLogger().log("Llamando API de geolocalización para: " + ip);

            String apiUrl = "http://ip-api.com/json/" + ip + "?fields=city,status,message";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            context.getLogger().log("Respuesta API status: " + response.statusCode());
            context.getLogger().log("Respuesta API body: " + response.body());

            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                String status = jsonNode.get("status").asText();

                if ("success".equals(status)) {
                    String city = jsonNode.get("city").asText();
                    return (city != null && !city.isEmpty()) ? city : "Ciudad no disponible";
                } else {
                    String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "Error desconocido";
                    context.getLogger().log("Error en API: " + message);
                    return "Error: " + message;
                }
            }

            return "Servicio no disponible";

        } catch (IOException e) {
            context.getLogger().log("IOException en getCityFromIp: " + e.getMessage());
            return "Error de conexión";
        } catch (InterruptedException e) {
            context.getLogger().log("InterruptedException en getCityFromIp: " + e.getMessage());
            Thread.currentThread().interrupt();
            return "Operación interrumpida";
        } catch (Exception e) {
            context.getLogger().log("Exception en getCityFromIp: " + e.getMessage());
            return "Error desconocido";
        }
    }

    private void saveConnectionRecord(String sessionId, String ip, String city, String timestamp, Context context) {
        try {
            context.getLogger().log("Creando item para DynamoDB");

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("sessionId", new AttributeValue().withS(sessionId));
            item.put("ip_address", new AttributeValue().withS(ip));
            item.put("city", new AttributeValue().withS(city));
            item.put("connection_time", new AttributeValue().withS(timestamp));

            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(TABLE_NAME)
                    .withItem(item);

            context.getLogger().log("Enviando item a DynamoDB");
            dynamoClient.putItem(putItemRequest);
            context.getLogger().log("Item guardado exitosamente en DynamoDB");

        } catch (Exception e) {
            context.getLogger().log("Error guardando en DynamoDB: " + e.getMessage());
            throw e; // Re-lanzar para que sea manejado por el catch principal
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setBody(body);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        response.setHeaders(headers);

        return response;
    }
}