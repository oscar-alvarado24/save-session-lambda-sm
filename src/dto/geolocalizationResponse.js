
/**
 * DTO para GeolocalizationResponse
 * Maneja la respuesta de la API de geolocalización
 */

const CONSTANTS = require('../helpers/constants');

class GeolocalizationResponse {
    constructor(data) {
        this.ip = data?.ip || '';
        this.isp = data?.isp || {};
        this.location = data?.location || {};
        this.risk = data?.risk || {};
    }

    /**
     * Crea una instancia desde la respuesta JSON de la API
     * @param {Object} apiResponse - Respuesta de la API
     * @returns {GeolocalizationResponse} - Instancia del DTO
     */
    static fromApiResponse(apiResponse) {
        return new GeolocalizationResponse(apiResponse);
    }

    /**
     * Obtiene la ciudad
     * @returns {string} - Nombre de la ciudad
     */
    getCity() {
        return this.location.city || CONSTANTS.CITY_NOT_AVAILABLE;
    }

    /**
     * Obtiene el país
     * @returns {string} - Nombre del país
     */
    getCountry() {
        return this.location.country || CONSTANTS.COUNTRY_NOT_AVAILABLE;
    }


    /**
     * Obtiene la zona horaria
     * @returns {string} - Zona horaria
     */
    getTimezone() {
        return this.location.timezone || CONSTANTS.TIMEZONE_NOT_AVAILABLE;
    }

    /**
     * Obtiene el tiempo local (connectionTime)
     * @returns {string} - Tiempo local en formato ISO
     */
    getConnectionTime() {
        return this.location.localtime || new Date().toISOString();
    }

    /**
     * Obtiene las coordenadas (latitud, longitud)
     * @returns {Object} - Objeto con lat y lng
     */
    getCoordinates() {
        return {
            latitude: this.location.latitude || 0,
            longitude: this.location.longitude || 0
        };
    }

    /**
     * Valida si la respuesta es válida y completa
     * @returns {boolean} - true si es válida
     */
    isValid() {
        return !!(this.ip && this.location && Object.keys(this.location).length > 0);
    }

    /**
     * Obtiene todos los datos de ubicación en un objeto simple
     * @returns {Object} - Datos de ubicación extraídos
     */
    getLocationData() {
        return {
            city: this.getCity(),
            country: this.getCountry(),
            timezone: this.getTimezone(),
            connectionTime: this.getConnectionTime(),
            coordinates: this.getCoordinates(),
        };
    }

    /**
     * Convierte a objeto plano para logging o debugging
     * @returns {Object} - Representación del objeto
     */
    toJSON() {
        return {
            ip: this.ip,
            isp: this.isp,
            location: this.location,
            risk: this.risk
        };
    }

    /**
     * Convierte a string para logging
     * @returns {string} - String representation
     */
    toString() {
        return JSON.stringify(this.toJSON(), null, 2);
    }

    /**
     * Verifica si la conexión viene de un datacenter
     * @returns {boolean} - true si es datacenter
     */
    isFromDatacenter() {
        return this.risk.is_datacenter || false;
    }

    /**
     * Verifica si la conexión usa VPN/Proxy/Tor
     * @returns {boolean} - true si usa algún tipo de proxy
     */
    isUsingProxy() {
        return this.risk.is_vpn || this.risk.is_proxy || this.risk.is_tor;
    }
}

module.exports = GeolocalizationResponse;