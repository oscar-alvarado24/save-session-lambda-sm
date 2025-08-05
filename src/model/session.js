/**
 * Modelo de Session
 * Equivalente a Session.java
 */

class Session {
    constructor(email, ip, city, timezone, country, coordinates) {
        this.email = email;
        this.connectionTime = new Date().toISOString();
        this.ip = ip;
        this.city = city;
        this.timezone = timezone;
        this.country = country;
        this.latitude = coordinates.latitude;
        this.longitude = coordinates.longitude;
    }

    /**
     * Convierte la sesión a formato DynamoDB
     * @returns {Object} - Objeto para DynamoDB
     */
    toDynamoItem() {
        return {
            id: this.email,                    // Partition Key
            connection_time: this.connectionTime,  // Sort Key
            ip: this.ip,
            city: this.city,
            timezone: this.timezone,
            country: this.country,
            latitude: this.latitude,
            longitude: this.longitude
            
        };
    }

    /**
     * Crea una sesión desde un item de DynamoDB
     * @param {Object} dynamoItem - Item de DynamoDB
     * @returns {Session} - Instancia de Session
     */
    static fromDynamoItem(dynamoItem) {
        return new Session(
            dynamoItem.id,
            dynamoItem.connection_time,
            dynamoItem.ip,
            dynamoItem.city,
            dynamoItem.timezone,
            dynamoItem.country,
            {
                latitude: dynamoItem.latitude,
                longitude: dynamoItem.longitude
            }
        );
    }

    /**
     * Valida si la sesión tiene todos los campos requeridos
     * @returns {boolean} - true si es válida
     */
    isValid() {
        return !!(this.email && 
                 this.connectionTime && 
                 this.ip && 
                 this.city)&&
                 this.timezone &&
                 this.country &&
                 this.latitude &&
                 this.longitude;
    }

    /**
     * Convierte la sesión a JSON string
     * @returns {string} - JSON string de la sesión
     */
    toString() {
        return JSON.stringify({
            email: this.email,
            connectionTime: this.connectionTime,
            ip: this.ip,
            city: this.city,
            timezone: this.timezone,
            country: this.country, 
            coordinates: {
                latitude: this.latitude,
                longitude: this.longitude
            }
        }, null, 2);
    }

    /**
     * Convierte la sesión a objeto plano
     * @returns {Object} - Objeto plano
     */
    toJSON() {
        return {
            email: this.email,
            connectionTime: this.connectionTime,
            ip: this.ip,
            city: this.city,
            timezone: this.timezone,
            country: this.country, 
            coordinates: {
                latitude: this.latitude,
                longitude: this.longitude
            }
        };
    }
}

module.exports = Session;