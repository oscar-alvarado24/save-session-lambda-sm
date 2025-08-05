/**
 * Métodos auxiliares
 */

/**
 * Obtiene un string de un objeto JSON
 * @param {Object} jsonNode - Objeto JSON
 * @param {string} fieldName - Nombre del campo
 * @returns {string|null} - Valor del campo o null
 */
function getStringFromJson(jsonNode, fieldName) {
    try {
        // Usando optional chaining - más conciso y legible
        return jsonNode?.[fieldName]?.toString() ?? null;
    } catch (error) {
        console.error(`Error obteniendo campo ${fieldName} del JSON:`, error);
        return null;
    }
}

/**
 * Formatea un mensaje con parámetros
 * @param {string} template - Template del mensaje con %s
 * @param {string} value - Valor a reemplazar
 * @returns {string} - Mensaje formateado
 */
function formatMessage(template, value) {
    return template.replace('%s', value);
}

/**
 * Valida si una cadena no está vacía
 * @param {string} str - Cadena a validar
 * @returns {boolean} - true si es válida
 */
function isValidString(str) {
    return str && typeof str === 'string' && str.trim() !== '';
}

/**
 * Obtiene el timestamp actual en formato yyyy-MM-dd HH:mm:ss
 * @returns {string} - Timestamp formateado
 */
function getCurrentTimestamp() {
    return new Date().toLocaleString('sv-SE', { timeZone: 'UTC' })
        .replace('T', ' ').substring(0, 19);
}

/**
 * Maneja errores de manera consistente
 * @param {string} operation - Nombre de la operación
 * @param {Error} error - Error ocurrido
 * @returns {Error} - Error formateado
 */
function handleError(operation, error) {
    const errorMessage = `Error en ${operation}: ${error.message}`;
    console.error(errorMessage);
    console.error('Stack trace:', error.stack);
    return new Error(errorMessage);
}

/**
 * Valida si una IP es válida (formato básico IPv4 o IPv6)
 * @param {string} ip - Dirección IP
 * @returns {boolean} - true si es válida
 */
function isValidIP(ip) {
    if (!ip || typeof ip !== 'string') return false;
    
    ip = ip.trim();
    
    return isValidIPv4Simple(ip) || isValidIPv6Simple(ip);
}

/**
 * Validación IPv4 simplificada
 * @param {string} ip - Dirección IP
 * @returns {boolean} - true si es IPv4 válida
 */
function isValidIPv4Simple(ip) {
    // Regex simple para formato básico
    const ipv4Regex = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;
    
    if (!ipv4Regex.test(ip)) return false;
    
    // Validación manual de rangos y ceros
    const octets = ip.split('.');
    
    for (const octet of octets) {
        // Verificar que no empiece con 0 (excepto "0" solo)
        if (octet.length > 1 && octet.startsWith('0')) return false;
        
        // Verificar rango 0-255
        const num = parseInt(octet, 10);
        if (num < 0 || num > 255) return false;
    }
    
    return true;
}

/**
 * Validación IPv6 simplificada
 * @param {string} ip - Dirección IP
 * @returns {boolean} - true si es IPv6 válida
 */
function isValidIPv6Simple(ip) {
    // Casos especiales simples
    if (ip === '::' || ip === '::1') return true;
    
    // Verificar caracteres válidos
    if (!/^[0-9a-fA-F:]+$/.test(ip)) return false;
    
    // No puede empezar o terminar con : (excepto ::)
    if ((ip.startsWith(':') && !ip.startsWith('::')) || 
        (ip.endsWith(':') && !ip.endsWith('::'))) {
        return false;
    }

    if (validateGroups(ip) === false) return false;
    // No puede tener más de un ::
    const doubleColonCount = (ip.match(/::/g) || []).length;
    if (doubleColonCount > 1) return false;
    
    
    
    // Validar cada grupo (1-4 caracteres hexadecimales)
    return groups.every(group => {
        if (group === '') return true; // Permitir grupos vacíos en compresión
        return /^[0-9a-fA-F]{1,4}$/.test(group);
    });
}

function validateGroups(ip) {
        // Dividir y validar grupos
        let groups;
        if (ip.includes('::')) {
            // Caso con compresión
            const parts = ip.split('::');
            const leftGroups = parts[0] ? parts[0].split(':') : [];
            const rightGroups = parts[1] ? parts[1].split(':') : [];
            groups = [...leftGroups, ...rightGroups];
            
            // No puede tener más de 7 grupos con compresión
            if (groups.length > 7) return false;
        } else {
            // Caso sin compresión - debe tener exactamente 8 grupos
            groups = ip.split(':');
            if (groups.length !== 8) return false;
        }
    }

    /**
 * Valida los datos de entrada
 * @param {string} email - Email del usuario
 * @param {string} ip - Dirección IP
 * @returns {Object} - Resultado de validación
 */
function validateInput(email, ip) {
    const errors = [];
    console.log('iniciando validación de entrada');
    // Verificar que email sea string antes de usar trim()
    if (!email || typeof email !== 'string' || email.trim() === '') {
        console.log('Email no válido');
        errors.push('Email es requerido');
    }
    
    // Verificar que ip sea string antes de usar trim()
    if (!ip || typeof ip !== 'string' || ip.trim() === '') {
        console.log('IP no válida');
        errors.push('IP es requerida');
    } else if (!isValidIP(ip)) {
        console.log('Formato de IP inválido');
        errors.push('Formato de IP inválido');
    }
    
    return {
        isValid: errors.length === 0,
        errors: errors
    };
}


module.exports = {
    getStringFromJson,
    formatMessage,
    isValidString,
    getCurrentTimestamp,
    handleError,
    validateInput
};