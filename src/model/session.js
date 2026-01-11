/**
 * Session Model for SAVE Lambda
 */

class Session {
    constructor(email, ip, city, timezone, country, coordinates) {
        // Basic validations
        if (!email) throw new Error('Email is required');
        if (!ip) throw new Error('IP is required');
        if (!city) throw new Error('City is required');
        if (!timezone) throw new Error('Timezone is required');
        if (!country) throw new Error('Country is required');
        if (!coordinates) throw new Error('Coordinates are required');

        this.email = email;
        this.connectionTime = new Date().toISOString();
        this.ip = ip;
        this.city = city;
        this.timezone = timezone;
        this.country = country;
        this.latitude = coordinates.latitude || 0;
        this.longitude = coordinates.longitude || 0;
    }

    /**
     * Converts the session to DynamoDB format
     * @returns {Object} - Object for DynamoDB
     */
    toDynamoItem() {
        return {
            id: this.email,
            connection_time: this.connectionTime,
            ip: this.ip,
            city: this.city,
            timezone: this.timezone,
            country: this.country,
            latitude: this.latitude,
            longitude: this.longitude
        };
    }

    /**
     * Creates a session from a DynamoDB item
     * @param {Object} dynamoItem - DynamoDB item
     * @returns {Session} - Session instance
     */
    static fromDynamoItem(dynamoItem) {
        if (!dynamoItem) {
            throw new Error('DynamoDB item is null or undefined');
        }

        const requiredFields = ['id', 'connection_time', 'ip', 'city', 'timezone', 'country'];
        const missingFields = requiredFields.filter(field => !dynamoItem[field]);

        if (missingFields.length > 0) {
            throw new Error(`Missing required fields: ${missingFields.join(', ')}`);
        }

        const session = new Session(
            dynamoItem.id,
            dynamoItem.ip,
            dynamoItem.city,
            dynamoItem.timezone,
            dynamoItem.country,
            {
                latitude: dynamoItem.latitude ?? 0,
                longitude: dynamoItem.longitude ?? 0
            }
        );

        session.connectionTime = dynamoItem.connection_time;

        return session;
    }

    /**
     * Validates if the session has all required fields
     * @returns {boolean} - true if valid
     */
    isValid() {
        return !!(
            this.email &&
            this.connectionTime &&
            this.ip &&
            this.city &&
            this.timezone &&
            this.country &&
            this.latitude !== undefined &&
            this.longitude !== undefined
        );
    }

    /**
     * Converts the session to JSON string
     * @returns {string} - JSON string of the session
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
     * Converts the session to plain object
     * @returns {Object} - Plain object
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