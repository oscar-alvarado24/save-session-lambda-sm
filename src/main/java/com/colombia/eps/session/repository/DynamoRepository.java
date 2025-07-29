package com.colombia.eps.session.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.Select;
import com.colombia.eps.session.helper.exceptions.CountSessionByIdException;
import com.colombia.eps.session.helper.exceptions.DeleteOldestSessionException;
import com.colombia.eps.session.helper.exceptions.GetOldestSessionException;
import com.colombia.eps.session.helper.exceptions.SaveSessionInTableException;
import com.colombia.eps.session.helper.exceptions.SaveSessionProcessException;
import com.colombia.eps.session.model.Session;

import java.util.Optional;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DynamoRepository {
    private final AmazonDynamoDB dynamoDBClient;
    private DynamoDBMapper mapper;

    @PostConstruct
    public void init() {
        String tableName = System.getenv("DYNAMO_TABLE_NAME");
        log.debug("Inicializando DynamoRepository con tabla: {}", tableName);

        DynamoDBMapperConfig config = DynamoDBMapperConfig.builder()
                .withTableNameOverride(
                        DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableName)
                )
                .build();

        this.mapper = new DynamoDBMapper(dynamoDBClient, config);
    }

    public void saveSession(String sessionId, String ip, String city, String timestamp) {
        try {

            saveInTable(sessionId, ip, city, timestamp);
            int sessionCount = countSessionsById(sessionId);
            log.debug("Cantidad de sesiones encontradas: {}", sessionCount);
            if (sessionCount > 5) {
                log.debug("Límite alcanzado, eliminando sesión más antigua...");
                deleteOldestSession(sessionId);
                log.debug("Sesión más antigua eliminada");
            }
        } catch (GetOldestSessionException | DeleteOldestSessionException | SaveSessionInTableException | CountSessionByIdException exception ) {
            throw exception;
        } catch(Exception exception) {
            throw new SaveSessionProcessException();
        }
    }

    private void saveInTable(String sessionId, String ip, String city, String timestamp) {
        try {
            log.debug("Creando item para DynamoDB");
            Session session = Session.builder()
                    .email(sessionId)
                    .connectionTime(timestamp)
                    .ip(ip)
                    .city(city)
                    .build();

            log.debug("Enviando item a DynamoDB");
            mapper.save(session);
            log.debug("Item guardado exitosamente en DynamoDB");
        } catch (Exception exception) {
            log.error("Error al guardar la sesion en la tabla: {}", exception.getMessage());
            throw new SaveSessionInTableException();
        }
    }

    private int countSessionsById(String sessionId) {
        try {
            log.debug("Contando registros con sessionId: {}", sessionId);
            Session hashKeyValues = Session.builder().email(sessionId).build();
            hashKeyValues.setEmail(sessionId);
            
            DynamoDBQueryExpression<Session> queryExpression = new DynamoDBQueryExpression<Session>()
                    .withHashKeyValues(hashKeyValues)
                    .withSelect(Select.COUNT);
            
            int count = mapper.count(Session.class, queryExpression);
            log.debug("Registros encontrados con sessionId {}: {}", sessionId, count);
            return count;
        } catch (Exception e) {
            log.error("Error contando registros por sessionId: {}", e.getMessage());
            throw new CountSessionByIdException();
        }
    }

    private void deleteOldestSession(String sessionId) {
        try {
            log.debug("Eliminando sesión más antigua para sessionId: {}", sessionId);
            Optional<Session> oldestSession = getSession(sessionId,Boolean.TRUE);
            if (oldestSession.isPresent()) {
                String session =oldestSession.get().toString();
                log.debug("Sesión más antigua encontrada: {}", session);
                mapper.delete(oldestSession.get());
                log.debug("Proceso de eliminacion de la sesión más antigua exitoso");
            }
        } catch (Exception e) {
            log.error("Error eliminando sesión más antigua de tipo: {} con el mensaje {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("Secuencia de error: {}", Arrays.toString(e.getStackTrace()));
            throw new DeleteOldestSessionException();
        }
    }

    private Optional<Session> getSession(String sessionId, Boolean order) {
        try {
            log.debug("Obteniendo sesión más antigua para sessionId: {}", sessionId);
            Session hashKeyValues = Session.builder().email(sessionId).build();
            
            DynamoDBQueryExpression<Session> queryExpression = new DynamoDBQueryExpression<Session>()
                    .withHashKeyValues(hashKeyValues)
                    .withScanIndexForward(order)
                    .withLimit(1);
            
            PaginatedQueryList<Session> sessions = mapper.query(Session.class, queryExpression);
            log.debug(" sesion mas antigua: {}",sessions.isEmpty() ? "ninguna":sessions.getFirst());
            return sessions.isEmpty() ? Optional.empty() : Optional.ofNullable(sessions.getFirst());
        } catch (Exception e) {
            log.error("Error obteniendo sesión más antigua: {}", e.getMessage());
            throw new GetOldestSessionException();
        }
    }
}
