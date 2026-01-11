/**
 * Servicio de Session
 */

const CONSTANTS = require('../helpers/constants');
const CryptoService = require('../helpers/crypto');
const { validateInput } = require('../helpers/auxiliaryMethods');

class SessionService {
    constructor(dynamoRepository) {
        this.cryptoService = new CryptoService();
        this.dynamoRepository = dynamoRepository;
    }

    /**
     * Guarda una sesión completa
     * @param {string} email - Email del usuario
     * @param {string} ip - Dirección IP
     * @param {string} city - Ciudad
     * @param {string} country - País
     * @param {string} localtime - Hora local
     * @param {string} timezone - Zona horaria
     * @param {string} latitude - Latitud
     * @param {string} longitude - Longitud
     * @returns {Promise<string>} - Mensaje de resultado
     */
    async saveSession(emailEncripted, ipEncripted, cityEncripted, country, localtime, timezoneEncripted, latitudeEncripted, longitudeEncripted) {
        try {
            console.log('=== Iniciando función Lambda ===');
            if (!emailEncripted || !ipEncripted || !cityEncripted || !timezoneEncripted || !latitudeEncripted || !longitudeEncripted) {
                return {
                    success: false,
                    statusCode: 400,
                    message: CONSTANTS.MSG_ERROR_PARAMS_MISSING
                }
            }
            const email = await this.cryptoService.decrypt(emailEncripted);
            const ip = await this.cryptoService.decrypt(ipEncripted);
            const city = await this.cryptoService.decrypt(cityEncripted);
            const timezone = await this.cryptoService.decrypt(timezoneEncripted);
            const latitude = await this.cryptoService.decrypt(latitudeEncripted);
            const longitude = await this.cryptoService.decrypt(longitudeEncripted);
            const dataValidations = validateInput(email, ip, city, country, localtime, timezone, latitude, longitude);
            if (dataValidations.isValid) {

                console.log(`Email: ${email}, IP: ${ip}`, `City: ${city}`, `Timezone: ${timezone}`, `Country: ${country}`, `Localtime: ${localtime}`, `Latitude: ${latitude}`, `Longitude: ${longitude}`);
                const coordinates={
                    latitude: latitude,
                    longitude: longitude
                }
                await this.dynamoRepository.saveSession(
                    email,
                    ip,
                    city,
                    timezone,
                    country,
                    coordinates
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
            } else {
                console.error('Errores de validación:', dataValidations.errors.join(', '));
                return {
                    success: false,
                    statusCode: 400,
                    message: CONSTANTS.MSG_ERROR_PARAMS_MISSING
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