package com.colombia.eps.savesession.repository;

import com.colombia.eps.savesession.helper.exception.CountSessionByIdException;
import com.colombia.eps.savesession.helper.exception.DeleteOldestSessionException;
import com.colombia.eps.savesession.helper.exception.GetOldestSessionException;
import com.colombia.eps.savesession.helper.exception.SaveSessionInTableException;
import com.colombia.eps.savesession.helper.exception.SaveSessionProcessException;
import com.colombia.eps.savesession.model.Session;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@ApplicationScoped
public class DynamoRepository {

    private final DynamoDbClient dynamoDbClient;
    private DynamoDbTable<Session> sessionTable;
    private static final Logger LOG = Logger.getLogger(DynamoRepository.class);

    @PostConstruct
    public void init() {
        String tableName = System.getenv("DYNAMO_TABLE_NAME");
        LOG.debug(String.format("Inicializando DynamoRepository con tabla: %s", tableName));

        // Inicializa el cliente mejorado
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        // Configura la tabla
        this.sessionTable = enhancedClient.table(tableName, TableSchema.fromBean(Session.class));
    }

    public void saveSession(String sessionId, String ip, String city, String timestamp) {
        try {
            int sessionCount = countSessionsById(sessionId);
            LOG.debug(String.format("Cantidad de sesiones encontradas: %s", sessionCount));
            saveInTable(sessionId, ip, city, timestamp);
            if (sessionCount > 5) {
                LOG.debug("Límite alcanzado, eliminando sesión más antigua...");
                deleteOldestSession(sessionId);
                LOG.debug("Sesión más antigua eliminada");
            }
        } catch (Exception exception) {
            LOG.error(String.format("Error en el proceso de guardar sesión: %s", exception.getMessage()));
            throw new SaveSessionProcessException();
        }
    }

    private void saveInTable(String sessionId, String ip, String city, String timestamp) {
        try {
            LOG.debug("Creando item para DynamoDB");
            Session session = Session.builder()
                    .email(sessionId)
                    .connectionTime(timestamp)
                    .ip(ip)
                    .city(city)
                    .build();

            LOG.debug("Enviando item a DynamoDB");
            sessionTable.putItem(session);  // Usamos putItem con el cliente mejorado
            LOG.debug("Item guardado exitosamente en DynamoDB");
        } catch (Exception exception) {
            LOG.error(String.format("Error al guardar la sesión en la tabla: %s", exception.getMessage()));
            throw new SaveSessionInTableException();
        }
    }

    private int countSessionsById(String sessionId) {
        try {
            LOG.debug(String.format("Contando registros con sessionId: %s", sessionId));
            
            String tableName = System.getenv("DYNAMO_TABLE_NAME");
            
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("id = :pk")
                    .expressionAttributeValues(Map.of(":pk", AttributeValue.builder().s(sessionId).build()))
                    .select(Select.COUNT)
                    .build();

            QueryResponse response = dynamoDbClient.query(queryRequest);
            int count = response.count();
            LOG.debug(String.format("Registros encontrados con sessionId %s: %s", sessionId, count));
            return count;
        } catch (Exception e) {
            LOG.error(String.format("Error contando registros por sessionId: %s", e.getMessage()));
            throw new CountSessionByIdException();
        }
    }

    private void deleteOldestSession(String sessionId) {
        try {
            LOG.debug(String.format("Eliminando sesión más antigua para sessionId: %s", sessionId));
            Optional<Session> oldestSession = getSession(sessionId);
            if (oldestSession.isPresent()) {
                String session = oldestSession.get().toString();
                LOG.debug(String.format("Sesión más antigua encontrada: %s", session));
                sessionTable.deleteItem(oldestSession.get());  // Usamos deleteItem con el cliente mejorado
                LOG.debug("Proceso de eliminación de la sesión más antigua exitoso");
            }
        } catch (Exception e) {
            LOG.error(String.format("Error eliminando sesión más antigua: %s", e.getMessage()));
            throw new DeleteOldestSessionException();
        }
    }

    private Optional<Session> getSession(String sessionId) {
        try {
            LOG.debug(String.format("Obteniendo sesión más antigua para sessionId: %s", sessionId));
            
            QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
                    .partitionValue(sessionId)
                    .build());

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .scanIndexForward(true)  // true = ascending (oldest first)
                    .limit(1)
                    .build();

            return sessionTable.query(queryRequest).items().stream().findFirst();
        } catch (Exception e) {
            LOG.error(String.format("Error obteniendo sesión más antigua: %s", e.getMessage()));
            throw new GetOldestSessionException();
        }
    }
}
