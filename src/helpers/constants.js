/**
 * Constantes de la aplicación
 */

const CONSTANTS = {
    CITY: 'city',
    CITY_NOT_AVAILABLE: 'Ciudad no disponible',
    COUNTRY_NOT_AVAILABLE: 'País no disponible',
    TIMEZONE_NOT_AVAILABLE: 'Zona horaria no disponible',
    SUCCESSFULLY: 'Proceso realizado exitosamente',
    MESSAGE: 'message',
    MSG_ERROR: 'Error',
    MSG_ERROR_PARAMS_NOT_VALID: 'Parámetros no válidos',
    MSG_ERROR_PROCESSING: 'Error procesando la solicitud',
    MSG_ERROR_EMAIL_AND_IP_MISSING: 'Faltan parámetros email y ip',
    MSG_ERROR_EMAIL_MISSING: 'Falta parámetro email',
    MSG_ERROR_IP_MISSING: 'Falta parámetro ip',
    MAX_SESSIONS_PER_USER: 5,
    MAX_RETRY_ATTEMPTS: 3,
    HTTP_TIMEOUT: 5000,
    HTTP_STATUS: {
        OK: 200,
        BAD_REQUEST: 400,
        INTERNAL_SERVER_ERROR: 500
    }
};

module.exports = CONSTANTS;