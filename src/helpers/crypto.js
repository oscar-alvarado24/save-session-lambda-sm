const crypto = require('crypto');
const CONSTANTS = require('./constants');

class CryptoService {
  #secretKey;
  #algorithm = 'aes-256-gcm';

  constructor() {
    // Validar que SECRET_KEY existe
    if (!process.env.SECRET_KEY) {
      throw new Error('SECRET_KEY environment variable is required');
    }

    try {
      this.#secretKey = Buffer.from(process.env.SECRET_KEY, CONSTANTS.BASE_64);
    } catch (error) {
      throw new Error('SECRET_KEY must be a valid base64 string');
    }

    if (this.#secretKey.length !== 32) {
      throw new Error('La clave debe ser base64 de 32 bytes (256 bits)');
    }
  }

  async encrypt(data) {
    // Validar entrada
    if (data === null || data === undefined) {
      throw new Error('Data to encrypt cannot be null or undefined');
    }

    // Convertir a string si no lo es
    const dataStr = typeof data === 'string' ? data : String(data);

    const iv = crypto.randomBytes(12);
    const cipher = crypto.createCipheriv(this.#algorithm, this.#secretKey, iv);

    const encrypted = Buffer.concat([
      cipher.update(dataStr, CONSTANTS.UTF_8),
      cipher.final()
    ]);

    const authTag = cipher.getAuthTag();

    return Buffer.concat([iv, encrypted, authTag])
      .toString('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=/g, '');
  }

  async decrypt(encryptedData) {
    try {
      // Validar entrada
      if (!encryptedData || typeof encryptedData !== 'string') {
        throw new Error('Encrypted data must be a non-empty string');
      }

      let standardBase64 = encryptedData
        .replace(/-/g, '+')
        .replace(/_/g, '/');

      const paddingNeeded = (4 - (standardBase64.length % 4)) % 4;
      standardBase64 += '='.repeat(paddingNeeded);

      let combined;
      try {
        combined = Buffer.from(standardBase64, 'base64');
      } catch (error) {
        throw new Error('Invalid base64 format');
      }

      if (combined.length < 28) {
        throw new Error(`Datos muy cortos: ${combined.length} bytes (mínimo 28)`);
      }

      const iv = combined.subarray(0, 12);
      const authTag = combined.subarray(combined.length - 16);
      const encrypted = combined.subarray(12, combined.length - 16);

      const decipher = crypto.createDecipheriv(this.#algorithm, this.#secretKey, iv);
      decipher.setAuthTag(authTag);

      const decrypted = Buffer.concat([
        decipher.update(encrypted),
        decipher.final()
      ]);

      return decrypted.toString(CONSTANTS.UTF_8);
    } catch (error) {
      // Mejorar mensajes de error
      if (error.message.includes('Unsupported state')) {
        throw new Error('Decryption failed: Invalid key or corrupted data');
      }
      if (error.message.includes('auth')) {
        throw new Error('Decryption failed: Authentication tag mismatch');
      }

      console.error('❌ Decryption ERROR:', error.message);
      throw new Error(`Decryption failed: ${error.message}`);
    }
  }
}

module.exports = CryptoService;