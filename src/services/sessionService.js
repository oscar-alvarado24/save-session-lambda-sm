/**
 * Servicio de Session
 * Equivalente a SessionService.java
 */

const CityService = require('./cityService');
const CONSTANTS = require('../helpers/constants');
const { formatMessage, handleError } = require('../helpers/auxiliaryMethods');

class SessionService {
    constructor(dynamoRepository) {
        this.cityService = new CityService();
        this.dynamoRepository = dynamoRepository;
    }

    /**
     * Guarda una sesión completa
     * @param {string} email - Email del usuario
     * @param {string} ip - Dirección IP
     * @returns {Promise<string>} - Mensaje de resultado
     */
    async saveSession(email, ip) {
        console.log('=== Iniciando función Lambda ===');
        
        try {
            console.log(`Email: ${email}, IP: ${ip}`);

            console.log(`Obteniendo información de geolocalización para IP: ${ip}`);
            
            // Obtener información completa de geolocalización
            const locationInfo = await this.cityService.getLocationInfo(ip);
            
            console.log('Información de ubicación obtenida:', JSON.stringify(locationInfo));
            
            // Guardar en DynamoDB usando email como sessionId
            await this.dynamoRepository.saveSession(
                email, 
                ip, 
                locationInfo.city,
                locationInfo.timezone,
                locationInfo.country,
                locationInfo.coordinates
            );
            
            console.log('=== Función completada exitosamente ===');
            
            return formatMessage(CONSTANTS.MESSAGE, CONSTANTS.SUCCESSFULLY);
            
        } catch (error) {
            console.error(`ERROR: ${error.constructor.name} - ${error.message}`);
            console.error('Stack trace: ', error.stack);
            return formatMessage(CONSTANTS.MSG_ERROR, error.message);
        }
    }

    
    /**
     * Obtiene todas las sesiones de un usuario
     * @param {string} email - Email del usuario
     * @returns {Promise<Array>} - Array de sesiones
     */
    async getUserSessions(email) {
        try {
            console.log(`Obteniendo sesiones para usuario: ${email}`);
            return await this.dynamoRepository.getAllSessions(email);
        } catch (error) {
            throw handleError('getUserSessions', error);
        }
    }

}

module.exports = SessionService;