# Save Session Lambda

Función Lambda para guardar sesiones de usuarios en DynamoDB, incluyendo datos de geolocalización.

## Resumen

Esta función recibe un payload JSON (desde API Gateway u otro invocador), descifra campos cifrados con AES‑256‑GCM, valida los datos y guarda una entrada en una tabla de DynamoDB. Además, mantiene un máximo de sesiones por usuario eliminando la sesión más antigua cuando se supera el límite.

## Requisitos

- Node.js >= 18
- npm
- Credenciales AWS (IAM Role asignado a la Lambda o variables de entorno para ejecución local)

Dependencias principales (definidas en `package.json`):
- @aws-sdk/client-dynamodb
- @aws-sdk/lib-dynamodb

## Variables de entorno

La aplicación usa las siguientes variables de entorno:

- SECRET_KEY (requerida) — Clave secreta para cifrado/descifrado AES‑256‑GCM. Debe ser una cadena Base64 que represente 32 bytes (256 bits). Ejemplo para generar una clave: 

  node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

- DYNAMO_TABLE_NAME (requerida) — Nombre de la tabla DynamoDB donde se guardan las sesiones.
- AWS_REGION (opcional) — Región AWS; por defecto `us-east-1` si no está definida.

Además, la Lambda necesita permisos de AWS (mediante IAM role) para DynamoDB y CloudWatch Logs. Política mínima recomendada (ejemplo):

- dynamodb:PutItem sobre la tabla
- dynamodb:Query sobre la tabla
- dynamodb:DeleteItem sobre la tabla
- logs:CreateLogGroup, logs:CreateLogStream, logs:PutLogEvents

## Esquema esperado en DynamoDB

La tabla debe tener las siguientes claves:

- Partition key: `id` (String) — se usa el email descifrado como id
- Sort key: `connection_time` (String) — timestamp ISO de la conexión

Además se almacenan atributos: `ip`, `city`, `timezone`, `country`, `latitude`, `longitude`.

## Instalación

Desde la raíz del proyecto:

1. Instala dependencias:

   npm install

2. Empaquetar en ZIP (script para Windows incluido):

   npm run zip

Este proyecto incluye un script `zip` que usa PowerShell para crear `save-session-lambda.zip` listo para subir a AWS Lambda.

## Formato del payload (entrada)

La función espera el `event.body` como JSON string con los campos siguientes:

- email (string) — valor cifrado (Base64 URL safe) con la misma `SECRET_KEY` y AES‑256‑GCM
- ip (string) — cifrado
- city (string) — cifrado
- country (string) — **no cifrado** (p. ej. "Chile")
- localtime (string) — **no cifrado** (p. ej. "2026-01-10T12:34:56Z" o formato legible)
- timezone (string) — cifrado
- latitude (string) — cifrado (ej. "-33.4489")
- longitude (string) — cifrado (ej. "-70.6693")

Nota: el servicio espera que los campos marcados como "cifrados" hayan sido cifrados con el mismo esquema que implementa `src/helpers/crypto.js` (AES‑256‑GCM). El módulo acepta una representación Base64 modificada (URL safe: `+`→`-`, `/`→`_`, sin `=` de padding).

Ejemplo (valores cifrados son placeholders):

{
  "email": "<EMAIL_ENCRYPTED>",
  "ip": "<IP_ENCRYPTED>",
  "city": "<CITY_ENCRYPTED>",
  "country": "Chile",
  "localtime": "2026-01-10T12:34:56Z",
  "timezone": "<TIMEZONE_ENCRYPTED>",
  "latitude": "<LAT_ENCRYPTED>",
  "longitude": "<LON_ENCRYPTED>"
}

### Cómo generar valores cifrados para probar

1. Establece `SECRET_KEY` (localmente) con la clave Base64 de 32 bytes:

   setx SECRET_KEY "<tu_base64_32_bytes>"; PowerShell -Command "echo $Env:SECRET_KEY"

2. Puedes usar un pequeño script Node.js que importe `src/helpers/crypto.js` y utilice `encrypt()` para generar los valores. Ejemplo mínimo (requiere configurar SECRET_KEY en el entorno):

```js
// tools/encrypt-sample.js (crear temporalmente)
const CryptoService = require('../src/helpers/crypto');
(async ()=>{
  const cs = new CryptoService();
  const plain = process.argv[2];
  console.log(await cs.encrypt(plain));
})();
```

Ejecutar:

   node tools/encrypt-sample.js "user@example.com"

Esto imprimirá la cadena cifrada que puede colocarse en el payload.

> Alternativa rápida para generar SECRET_KEY (desde PowerShell):
> PowerShell -Command "[Convert]::ToBase64String((New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes(32))"

## Ejemplo de respuesta

- Éxito (HTTP 200):

  {
    "message": "Proceso realizado exitosamente"
  }

- Error de validación (HTTP 400):

  {
    "message": "Parámetros no válidos"
  }

- Error interno (HTTP 500):

  {
    "message": "Error procesando la solicitud"
  }

El campo `message` corresponde a constantes definidas en `src/helpers/constants.js`.

## Despliegue a AWS Lambda

1. Genera `save-session-lambda.zip` con `npm run zip`.
2. Crea una función Lambda (Node.js 18 runtime).
3. Asigna un role con permisos mínimos para DynamoDB (PutItem/Query/Delete) y CloudWatch Logs.
4. Sube el ZIP a la función (o usa CI/CD).
5. Configura variables de entorno en la Lambda: `SECRET_KEY`, `DYNAMO_TABLE_NAME`, y opcionalmente `AWS_REGION`.
6. (Opcional) Configura API Gateway para exponer un endpoint HTTP y pasar el body como string en `event.body`.

## Troubleshooting (problemas comunes)

- SECRET_KEY faltante o formato incorrecto: el constructor de `CryptoService` lanzará un error si `SECRET_KEY` no existe o no es Base64 de 32 bytes.
- JSON inválido: si `event.body` no es JSON válido, la función devuelve 400 con mensaje de formato inválido.
- Decrypt failed: si el dato cifrado no coincide con la clave se producirá un error de descifrado (mensaje claro en logs).
- Permisos DynamoDB: errores 403/AccessDenied indican que el rol de Lambda no tiene permisos suficientes.
- Esquema de tabla incorrecto: la tabla debe tener `id` (PK) y `connection_time` (SK). Si no, las operaciones Query/Delete fallarán.


## Autor y licencia

Autor: Oscar Alvarado
Licencia: ISC

---
