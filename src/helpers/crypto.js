const crypto = require('crypto');

class CryptoService {
  #secretKey;
  #algorithm = 'aes-256-gcm';

  constructor() {
    this.#secretKey = Buffer.from(process.env.SECRET_KEY, 'base64');
    
    if (this.#secretKey.length !== 32) {
      throw new Error('La clave debe ser base64 de 32 bytes (256 bits)');
    }
  }

  async decrypt(encryptedData) {
    try {
      const combined = Buffer.from(encryptedData, 'base64');
      
      const iv = combined.subarray(0, 12);
      const authTag = combined.subarray(combined.length - 16);
      const encrypted = combined.subarray(12, combined.length - 16);
      
      const decipher = crypto.createDecipheriv(this.#algorithm, this.#secretKey, iv);
      
      // IMPORTANTE: Establecer authTag ANTES de actualizar los datos
      decipher.setAuthTag(authTag);
      
      const decrypted = Buffer.concat([
        decipher.update(encrypted),
        decipher.final()
      ]);
      
      return decrypted.toString('utf8');
    } catch (error) {
      console.error('Error en descifrado:', error.message);
      throw new Error(`Error descifrando datos: ${error.message}`);
    }
  }
}

module.exports = CryptoService;