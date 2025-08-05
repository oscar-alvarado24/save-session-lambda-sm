/**
 * Servicio de City/Geolocalización
 * Equivalente a City.java
 */

const axios = require('axios');
const CONSTANTS = require('../helpers/constants');
const GeolocalizationResponse = require('../dto/geolocalizationResponse');
const { handleError } = require('../helpers/auxiliaryMethods');

class CityService {
    constructor() {
        this.maxAttempts = CONSTANTS.MAX_RETRY_ATTEMPTS;
        this.timeout = CONSTANTS.HTTP_TIMEOUT;
    }

    /**
     * Obtiene información de geolocalización completa basada en la IP
     * @param {string} ip - Dirección IP
     * @returns {Promise<Object>} - Información de geolocalización
     */
    async getLocationInfo(ip) {
        let attempts = 0;
        
        while (attempts <= this.maxAttempts) {
            attempts++;
            console.log(`Intento ${attempts} de obtener información de geolocalización`);
            
            try {
                const apiResponse = await this._makeGeolocationRequest(ip);
                console.log('Respuesta API:', JSON.stringify(apiResponse));
                
                // Crear el DTO desde la respuesta de la API
                const geoResponse = GeolocalizationResponse.fromApiResponse(apiResponse);
                
                // Validar que la respuesta sea válida
                if (geoResponse.isValid()) {
                    return geoResponse.getLocationData();
                } else {
                    console.error('Respuesta de API inválida o incompleta');
                }
            } catch (error) {
                console.error(`Error en intento ${attempts}: ${error.message}`);
                
                // Si es el último intento, loggear el error completo
                if (attempts > this.maxAttempts) {
                    console.error('Máximo número de intentos alcanzado');
                    handleError('getLocationInfo', error);
                }
            }
        }
        
        return this._getDefaultLocationInfo();
    }

    /**
     * Obtiene la respuesta completa de geolocalización como DTO
     * @param {string} ip - Dirección IP
     * @returns {Promise<GeolocalizationResponse|null>} - DTO de respuesta completo
     */
    async getGeolocalizationResponse(ip) {
        try {
            const apiResponse = await this._makeGeolocationRequest(ip);
            const geoResponse = GeolocalizationResponse.fromApiResponse(apiResponse);
            
            if (geoResponse.isValid()) {
                return geoResponse;
            }
            
            return null;
        } catch (error) {
            console.error('Error obteniendo respuesta de geolocalización:', error);
            return null;
        }
    }

    /**
     * Retorna información de ubicación por defecto cuando falla la API
     * @returns {Object} - Información por defecto
     * @private
     */
    _getDefaultLocationInfo() {
        return {
            city: CONSTANTS.CITY_NOT_AVAILABLE,
            country: CONSTANTS.COUNTRY_NOT_AVAILABLE,
            timezone: CONSTANTS.TIMEZONE_NOT_AVAILABLE,
            coordinates: {
                latitude: 0,
                longitude: 0
            },
            connectionTime: new Date().toISOString()
        };
    }


    /**
     * Realiza la petición HTTP al servicio de geolocalización
     * @param {string} ip - Dirección IP
     * @returns {Promise<Object>} - Respuesta del servicio
     * @private
     */
    async _makeGeolocationRequest(ip) {
        // URL de la nueva API (ajusta según el endpoint real)
        const url = `https://api.ipquery.io/${ip}`;

        const response = await axios.get(url, {
            timeout: this.timeout,
            headers: {
                'User-Agent': 'SaveSessionLambda/1.0',
                'Accept': 'application/json'
            }
        });

        return response.data;
    }

    
}

module.exports = CityService;