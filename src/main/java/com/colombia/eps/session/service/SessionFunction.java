package com.colombia.eps.session.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.colombia.eps.session.helper.AuxiliaryMethods;
import com.colombia.eps.session.helper.Constants;
import com.colombia.eps.session.helper.exceptions.ParseJsonException;
import com.colombia.eps.session.repository.DynamoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFunction implements Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final String urlFront=System.getenv("URL_FRONT");
    private final ObjectMapper objectMapper;
    private final DynamoRepository dynamoRepository;
    private final City city;
    private Map<String, String> headers ;

    @PostConstruct
    public void init() {
        this.headers= AuxiliaryMethods.getHeaders(this.urlFront);
    }

    @Override
    public APIGatewayProxyResponseEvent apply(APIGatewayProxyRequestEvent request) {
        log.debug("=== Iniciando función Lambda ===");
        try {
            // Extraer el body del evento de API Gateway
            String requestBody = request.getBody();
            log.debug("Request body: {}", requestBody);

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withHeaders(this.headers)
                        .withBody(String.format(Constants.MSG_ERROR, "Cuerpo de la petición es requerido"));
            }

            // Parsear el JSON del cuerpo
            JsonNode bodyJson = AuxiliaryMethods.parseJson(requestBody, objectMapper)
                    .orElseThrow(()->new ParseJsonException(Constants.MSG_PARSE_JSON));

            // Extraer parámetros del JSON
            CompletableFuture<String> emailFuture = CompletableFuture.supplyAsync(() -> AuxiliaryMethods.getAndValidateParams(bodyJson,Constants.EMAIL));
            CompletableFuture<String> ipFuture = CompletableFuture.supplyAsync(() ->  AuxiliaryMethods.getAndValidateParams(bodyJson,Constants.IP));

            CompletableFuture<Void> todoCompleto = CompletableFuture.allOf(emailFuture, ipFuture);

            todoCompleto.join();
            String email = emailFuture.join();
            String ip = ipFuture.join();
            log.debug("Email: {}, IP: {}", email, ip);
            log.debug("Obteniendo ciudad para IP: {}", ip);
            // Obtener información de geolocalización
            String cityName = city.getCity(ip);

            // Obtener timestamp actual
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));


            // Guardar en DynamoDB usando email como sessionId
            dynamoRepository.saveSession(email, ip, cityName, timestamp);
            log.debug("=== Función completada exitosamente ===");

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(this.headers)
                    .withBody(String.format(Constants.MESSAGE,Constants.SUCCESSFULLY));

        } catch (CompletionException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(this.headers)
                    .withBody(String.format(Constants.MSG_ERROR, e.getCause().getMessage()));
        } catch (Exception e) {
            log.error("ERROR: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("Stack trace: ", e);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(this.headers)
                    .withBody(String.format(Constants.MSG_ERROR, e.getMessage()));
        }
    }
}