const { DynamoDBClient } = require('@aws-sdk/client-dynamodb');
const DynamoRepository = require('./src/repository/dynamoRepository');
const SessionService = require('./src/services/sessionService');
const CONSTANTS = require('./src/helpers/constants');

const dynamoClient = new DynamoDBClient({
    region: process.env.AWS_REGION || 'us-east-1'
});
const dynamoRepository = new DynamoRepository(dynamoClient);
const sessionService = new SessionService(dynamoRepository);
exports.handler = async (event, context) => {
    try {
        const body = event.body;
        console.log('Raw event body:', body);

        const input = JSON.parse(body);

        const response = await sessionService.saveSession(input.email, input.ip);

        if (response.success) {
            return {
                statusCode: 200,
                body: JSON.stringify({ [CONSTANTS.MESSAGE]: response.message})
            };
        } else {
            return {
                statusCode: response.statusCode,
                body: JSON.stringify({ [CONSTANTS.MESSAGE]: response.message})
            };
        }
    } catch (error) {
        console.error('Error procesando la solicitud', error);

        return {
            statusCode: 500,
            body: JSON.stringify({ [CONSTANTS.MSG_ERROR]: CONSTANTS.MSG_ERROR_PROCESSING })
        };
    }
};