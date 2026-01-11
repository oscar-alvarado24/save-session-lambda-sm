/**
 * Auxiliary Methods
 */

/**
 * Gets a string from a JSON object
 * @param {Object} jsonNode - JSON object
 * @param {string} fieldName - Field name
 * @returns {string|null} - Field value or null
 */
function getStringFromJson(jsonNode, fieldName) {
    try {
        // Using optional chaining - more concise and readable
        return jsonNode?.[fieldName]?.toString() ?? null;
    } catch (error) {
        console.error(`Error getting field ${fieldName} from JSON:`, error);
        return null;
    }
}

/**
 * Validates if a string is not empty
 * @param {string} str - String to validate
 * @returns {boolean} - true if valid
 */
function isValidString(str) {
    return str && typeof str === 'string' && str.trim() !== '';
}

/**
 * Gets the current timestamp in yyyy-MM-dd HH:mm:ss format
 * @returns {string} - Formatted timestamp
 */
function getCurrentTimestamp() {
    return new Date().toLocaleString('sv-SE', { timeZone: 'UTC' })
        .replace('T', ' ').substring(0, 19);
}

/**
 * Handles errors consistently
 * @param {string} operation - Operation name
 * @param {Error} error - Occurred error
 * @returns {Error} - Formatted error
 */
function handleError(operation, error) {
    const errorMessage = `Error in ${operation}: ${error.message}`;
    console.error(errorMessage);
    console.error('Stack trace:', error.stack);
    return new Error(errorMessage);
}

/**
 * Validates if an IP is valid (basic IPv4 or IPv6 format)
 * @param {string} ip - IP address
 * @returns {boolean} - true if valid
 */
function isValidIP(ip) {
    if (!ip || typeof ip !== 'string') return false;

    ip = ip.trim();

    return isValidIPv4Simple(ip) || isValidIPv6Simple(ip);
}

/**
 * Simplified IPv4 validation
 * @param {string} ip - IP address
 * @returns {boolean} - true if valid IPv4
 */
function isValidIPv4Simple(ip) {
    // Simple regex for basic format
    const ipv4Regex = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;

    if (!ipv4Regex.test(ip)) return false;

    // Manual validation of ranges and zeros
    const octets = ip.split('.');

    for (const octet of octets) {
        // Verify it doesn't start with 0 (except "0" alone)
        if (octet.length > 1 && octet.startsWith('0')) return false;

        // Verify range 0-255
        const num = parseInt(octet, 10);
        if (num < 0 || num > 255) return false;
    }

    return true;
}

/**
 * Simplified IPv6 validation
 * @param {string} ip - IP address
 * @returns {boolean} - true if valid IPv6
 */
function isValidIPv6Simple(ip) {
    // Simple special cases
    if (ip === '::' || ip === '::1') return true;

    // Verify valid characters
    if (!/^[0-9a-fA-F:]+$/.test(ip)) return false;

    // Cannot start or end with : (except ::)
    if ((ip.startsWith(':') && !ip.startsWith('::')) ||
        (ip.endsWith(':') && !ip.endsWith('::'))) {
        return false;
    }

    // Validate groups
    const groupsValid = validateGroups(ip);
    if (!groupsValid) return false;

    // Cannot have more than one ::
    const doubleColonCount = (ip.match(/::/g) || []).length;
    if (doubleColonCount > 1) return false;

    return true;
}

/**
 * Validates the groups of an IPv6
 * @param {string} ip - IPv6 address
 * @returns {boolean} - true if groups are valid
 */
function validateGroups(ip) {
    let groups;

    if (ip.includes('::')) {
        // Case with compression
        const parts = ip.split('::');
        const leftGroups = parts[0] ? parts[0].split(':') : [];
        const rightGroups = parts[1] ? parts[1].split(':') : [];
        groups = [...leftGroups, ...rightGroups];

        // Cannot have more than 7 groups with compression
        if (groups.length > 7) return false;
    } else {
        // Case without compression - must have exactly 8 groups
        groups = ip.split(':');
        if (groups.length !== 8) return false;
    }

    // Validate each group (1-4 hexadecimal characters)
    return groups.every(group => {
        if (group === '') return true; // Allow empty groups in compression
        return /^[0-9a-fA-F]{1,4}$/.test(group);
    });
}

/**
 * Validates input data
 * @param {string} email - User email
 * @param {string} ip - IP address
 * @param {string} city - City
 * @param {string} country - Country
 * @param {string} localtime - Local time
 * @param {string} timezone - Timezone
 * @param {string} latitude - Latitude
 * @param {string} longitude - Longitude
 * @returns {Object} - Validation result
 */
function validateInput(email, ip, city, country, localtime, timezone, latitude, longitude) {
    const errors = [];
    console.log('Starting input validation');

    // Verify email is string before using trim()
    if (!email || typeof email !== 'string' || email.trim() === '') {
        console.log('Invalid email');
        errors.push('Email is required');
    } else {
        // Validate email format
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email.trim())) {
            errors.push('Invalid email format');
        }
    }

    // Verify ip is string before using trim()
    if (!ip || typeof ip !== 'string' || ip.trim() === '') {
        console.log('Invalid IP');
        errors.push('IP is required');
    } else if (!isValidIP(ip)) {
        console.log('Invalid IP format');
        errors.push('Invalid IP format');
    }

    if (!isValidString(city)) {
        console.log('Invalid city');
        errors.push('City is required');
    }

    if (!isValidString(timezone)) {
        console.log('Invalid timezone');
        errors.push('Timezone is required');
    }

    if (!isValidString(country)) {
        console.log('Invalid country');
        errors.push('Country is required');
    }

    if (!isValidString(localtime)) {
        console.log('Invalid local time');
        errors.push('Local time is required');
    }

    // Validate coordinates
    if (!isValidString(latitude)) {
        console.log('Invalid latitude');
        errors.push('Latitude is required');
    } else {
        const lat = parseFloat(latitude);
        if (isNaN(lat) || lat < -90 || lat > 90) {
            errors.push('Latitude must be between -90 and 90');
        }
    }

    if (!isValidString(longitude)) {
        console.log('Invalid longitude');
        errors.push('Longitude is required');
    } else {
        const lon = parseFloat(longitude);
        if (isNaN(lon) || lon < -180 || lon > 180) {
            errors.push('Longitude must be between -180 and 180');
        }
    }

    return {
        isValid: errors.length === 0,
        errors: errors
    };
}

module.exports = {
    getStringFromJson,
    isValidString,
    getCurrentTimestamp,
    handleError,
    validateInput
};