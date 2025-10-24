/**
 * Servicio de Session
 * Equivalente a SessionService.java
 */

const CityService = require('./cityService');
const CONSTANTS = require('../helpers/constants');
const CryptoService = require('../helpers/crypto');
const { validateInput } = require('../helpers/auxiliaryMethods');

class SessionService {
    constructor(dynamoRepository) {
        this.cityService = new CityService();
        this.cryptoService = new CryptoService();
        this.dynamoRepository = dynamoRepository;
    }

    /**
     * Guarda una sesión completa
     * @param {string} email - Email del usuario
     * @param {string} ip - Dirección IP
     * @returns {Promise<string>} - Mensaje de resultado
     */
    async saveSession(emailEncripted, ip) {
        try {
            console.log('=== Iniciando función Lambda ===');
            if (!emailEncripted ) {                
                return {
                    success: false,
                    statusCode: 400,
                    message: CONSTANTS.MSG_ERROR_EMAIL_MISSING
                }
            }
            const email = await this.cryptoService.decrypt(emailEncripted);
            const dataValidations = validateInput(email, ip);
            if (dataValidations.isValid) {

                console.log(`Email: ${email}, IP: ${ip}`);

                console.log(`Obteniendo información de geolocalización para IP: ${ip}`);

                const locationInfo = await this.cityService.getLocationInfo(ip);

                console.log('Información de ubicación obtenida:', JSON.stringify(locationInfo));

                await this.dynamoRepository.saveSession(
                    email,
                    ip,
                    locationInfo.city,
                    locationInfo.timezone,
                    locationInfo.country,
                    locationInfo.coordinates
                );
            const sessionCount = await this.dynamoRepository.countSessionsById(email);

            console.log(`Cantidad de sesiones encontradas: ${sessionCount}`);

            if (sessionCount > CONSTANTS.MAX_SESSIONS_PER_USER) {
                console.log('Límite alcanzado, eliminando sesión más antigua...');
                await this.dynamoRepository.deleteOldestSession(email);
                console.log('Sesión más antigua eliminada');
            }

                console.log('=== Función completada exitosamente ===');

                return {
                    success: true,
                    message: CONSTANTS.SUCCESSFULLY
                }
            }

        } catch (error) {
            console.error(`ERROR: ${error.constructor.name} - ${error.message}`);
            console.error('Stack trace: ', error.stack);
            return {
                    success: false,
                    statusCode: error.statusCode || 500,
                    message: dataValidations.errors.join(', ')
                }
        }
    }
}

module.exports = SessionService;