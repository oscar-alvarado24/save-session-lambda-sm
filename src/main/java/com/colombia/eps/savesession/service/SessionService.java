package com.colombia.eps.savesession.service;

import com.colombia.eps.savesession.helper.Constants;
import com.colombia.eps.savesession.repository.DynamoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
@RequiredArgsConstructor
public class SessionService {

    private final City city;
    private final DynamoRepository dynamoRepository;
    private static final Logger LOG = Logger.getLogger(SessionService.class);

    public String saveSession(String email, String ip){
        LOG.debug("=== Iniciando función Lambda ===");
        try {
            LOG.debug(String.format("Email: %s, IP: %s", email, ip));
            LOG.debug(String.format("Obteniendo ciudad para IP: %s", ip));
            // Obtener información de geolocalización
            String cityName = city.getCity(ip);

            // Obtener timestamp actual
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));


            // Guardar en DynamoDB usando email como sessionId
            dynamoRepository.saveSession(email, ip, cityName, timestamp);
            LOG.debug("=== Función completada exitosamente ===");

            return String.format(Constants.MESSAGE,Constants.SUCCESSFULLY);

        } catch (Exception e) {
            LOG.error(String.format("ERROR: %s - %s", e.getClass().getSimpleName(), e.getMessage()));
            LOG.error("Stack trace: ", e);
            return String.format(Constants.MSG_ERROR, e.getMessage());
        }
    }
}
