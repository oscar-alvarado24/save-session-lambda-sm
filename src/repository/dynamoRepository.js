/**
 * Repositorio de DynamoDB
 */

const { DynamoDBDocumentClient, PutCommand, QueryCommand, DeleteCommand } = require('@aws-sdk/lib-dynamodb');
const Session = require('../model/session');
const CONSTANTS = require('../helpers/constants');
const { handleError } = require('../helpers/auxiliaryMethods');

class DynamoRepository {
    constructor(dynamoClient) {
        this.docClient = DynamoDBDocumentClient.from(dynamoClient);
        this.tableName = process.env.DYNAMO_TABLE_NAME;
        
        if (!this.tableName) {
            throw new Error('DYNAMO_TABLE_NAME environment variable is required');
        }
        
        console.log(`Inicializando DynamoRepository con tabla: ${this.tableName}`);
    }

    /**
     * Guarda una sesión completa (con lógica de límite)
     * @param {string} sessionId - ID de la sesión (email)
     * @param {string} ip - Dirección IP
     * @param {string} city - Ciudad
     * @param {string} country - País
     * @param {string} timezone - Zona horaria
     * @param {string} timestamp - Timestamp de conexión
     */
    async saveSession(sessionId, connectionTime, ip, city, timezone, country, coordinates) {
        try {
            const sessionCount = await this.countSessionsById(sessionId);
            console.log(`Cantidad de sesiones encontradas: ${sessionCount}`);
            
            await this.saveInTable(sessionId, connectionTime, ip, city, timezone, country, coordinates);
            
            if (sessionCount > CONSTANTS.MAX_SESSIONS_PER_USER) {
                console.log('Límite alcanzado, eliminando sesión más antigua...');
                await this.deleteOldestSession(sessionId);
                console.log('Sesión más antigua eliminada');
            }
        } catch (error) {
            throw handleError('saveSession', error);
        }
    }

    /**
     * Guarda una sesión directamente en la tabla
     * @param {string} sessionId - ID de la sesión
     * @param {string} ip - IP
     * @param {string} city - Ciudad
     * @param {string} country - País
     * @param {string} timezone - Zona horaria
     * @param {string} timestamp - Timestamp
     */
    async saveInTable(sessionId, ip, city, timezone, country, coordinates) {
        try {
            console.log('Creando item para DynamoDB');
            
            const session = new Session(sessionId, ip, city, timezone, country, coordinates);
            console.log(`Item creado: ${session.toString()}`);
            if (!session.isValid()) {
                throw new Error('Session data is invalid');
            }
            
            console.log('Enviando item a DynamoDB');
            
            const params = {
                TableName: this.tableName,
                Item: session.toDynamoItem()
            };
            
            await this.docClient.send(new PutCommand(params));
            console.log('Item guardado exitosamente en DynamoDB');
        } catch (error) {
            throw handleError('saveInTable', error);
        }
    }

    /**
     * Cuenta las sesiones existentes para un sessionId
     * @param {string} sessionId - ID de la sesión
     * @returns {Promise<number>} - Cantidad de sesiones
     */
    async countSessionsById(sessionId) {
        try {
            console.log(`Contando registros con sessionId: ${sessionId}`);
            
            const params = {
                TableName: this.tableName,
                KeyConditionExpression: 'id = :pk',
                ExpressionAttributeValues: {
                    ':pk': sessionId
                },
                Select: 'COUNT'
            };
            
            const result = await this.docClient.send(new QueryCommand(params));
            const count = result.Count;
            
            console.log(`Registros encontrados con sessionId ${sessionId}: ${count}`);
            return count;
        } catch (error) {
            throw handleError('countSessionsById', error);
        }
    }

    /**
     * Elimina la sesión más antigua para un sessionId
     * @param {string} sessionId - ID de la sesión
     */
    async deleteOldestSession(sessionId) {
        try {
            console.log(`Eliminando sesión más antigua para sessionId: ${sessionId}`);
            
            const oldestSession = await this.getOldestSession(sessionId);
            if (oldestSession) {
                console.log(`Sesión más antigua encontrada: ${JSON.stringify(oldestSession)}`);
                
                const params = {
                    TableName: this.tableName,
                    Key: {
                        id: oldestSession.id,
                        connection_time: oldestSession.connection_time
                    }
                };
                
                await this.docClient.send(new DeleteCommand(params));
                console.log('Proceso de eliminación de la sesión más antigua exitoso');
            }
        } catch (error) {
            throw handleError('deleteOldestSession', error);
        }
    }

    /**
     * Obtiene la sesión más antigua para un sessionId
     * @param {string} sessionId - ID de la sesión
     * @returns {Promise<Object|null>} - Sesión más antigua o null
     */
    async getOldestSession(sessionId) {
        try {
            console.log(`Obteniendo sesión más antigua para sessionId: ${sessionId}`);
            
            const params = {
                TableName: this.tableName,
                KeyConditionExpression: 'id = :pk',
                ExpressionAttributeValues: {
                    ':pk': sessionId
                },
                ScanIndexForward: true, // true = ascending (oldest first)
                Limit: 1
            };
            
            const result = await this.docClient.send(new QueryCommand(params));
            return result.Items && result.Items.length > 0 ? result.Items[0] : null;
        } catch (error) {
            throw handleError('getOldestSession', error);
        }
    }

    /**
     * Obtiene todas las sesiones para un sessionId
     * @param {string} sessionId - ID de la sesión
     * @returns {Promise<Session[]>} - Array de sesiones
     */
    async getAllSessions(sessionId) {
        try {
            const params = {
                TableName: this.tableName,
                KeyConditionExpression: 'id = :pk',
                ExpressionAttributeValues: {
                    ':pk': sessionId
                }
            };
            
            const result = await this.docClient.send(new QueryCommand(params));
            return result.Items.map(item => Session.fromDynamoItem(item));
        } catch (error) {
            throw handleError('getAllSessions', error);
        }
    }
}

module.exports = DynamoRepository;