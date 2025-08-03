package com.colombia.eps.savesession;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.colombia.eps.savesession.dto.SessionRequest;
import com.colombia.eps.savesession.helper.Constants;
import com.colombia.eps.savesession.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;

@Named("saveSessionLambda")
@RequiredArgsConstructor
public class SaveSessionLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    private static final Logger LOG = Logger.getLogger(SaveSessionLambda.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            // Extrae el cuerpo del evento de API Gateway
            String body = event.getBody();
            LOG.info("Raw event body: " + body);

            // Parsea el cuerpo JSON a SessionRequest
            SessionRequest input = objectMapper.readValue(body, SessionRequest.class);

            LOG.info(String.format("Valores recibidos: email=%s, ip=%s", input.email(), input.ip()));

            if (input.email() != null && !input.email().isBlank() && input.ip() != null && !input.ip().isBlank()) {
                return new APIGatewayProxyResponseEvent()
                        .withBody(sessionService.saveSession(input.email(), input.ip()))
                        .withStatusCode(200);
            }
            return new APIGatewayProxyResponseEvent()
                    .withBody(Constants.MSG_ERROR_PARAMS_NOT_VALID)
                    .withStatusCode(400);
        } catch (Exception e) {
            LOG.error("Error procesando la solicitud", e);
            return new APIGatewayProxyResponseEvent()
                    .withBody(Constants.MSG_ERROR_PROCESSING)
                    .withStatusCode(500);
        }
    }
}

