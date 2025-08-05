/**
 * Handler principal de la Lambda
 */

const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const DynamoRepository = require('./src/repository/dynamoRepository'); 
const SessionService = require('./src/services/sessionService');     
const CONSTANTS = require('./src/helpers/constants');
const { validateInput } = require('./src/helpers/auxiliaryMethods');

const dynamoClient = new DynamoDBClient({ 
    region: process.env.AWS_REGION || 'us-east-1' 
});
const dynamoRepository = new DynamoRepository(dynamoClient);
const sessionService = new SessionService(dynamoRepository);
exports.handler = async (event, context) => {
    try {
        // Extraer el cuerpo del evento de API Gateway
        const body = event.body;
        console.log('Raw event body:', body);
        
        // Parsear el cuerpo JSON
        const input = JSON.parse(body);
        
        console.log(`Valores recibidos: email=${input.email}, ip=${input.ip}`);
        
        const inputValidation = validateInput(input.email, input.ip);
        
        // Validar parámetros
        if (inputValidation.isValid) {

            const result = await sessionService.saveSession(input.email, input.ip);
            
            return {
                statusCode: 200,
                headers: {
                    'Content-Type': 'application/json',
                    'Access-Control-Allow-Origin': '*'
                },
                body: JSON.stringify({ message: result })
            };
        }
        
        const errorMessage = inputValidation.isValid
            ? CONSTANTS.MSG_ERROR_PARAMS_NOT_VALID
            : inputValidation.errors.join(', ');
        console.log('El error es:', errorMessage);
        return {
            statusCode: 400,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({ error: errorMessage })
        };
        
    } catch (error) {
        console.error('Error procesando la solicitud', error);
        
        return {
            statusCode: 500,
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({ error: CONSTANTS.MSG_ERROR_PROCESSING })
        };
    }
};