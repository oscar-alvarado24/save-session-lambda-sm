# Save Session Lambda

## Installation

1. Ensure you have Node.js and npm installed on your system.
2. Install the required dependencies by running `npm install` in the project directory.
3. Set the necessary environment variables, including `AWS_REGION` and `DYNAMO_TABLE_NAME`.

## Usage

The main entry point of the application is the `index.js` file, which contains the AWS Lambda handler function. This function is responsible for processing incoming API requests, validating the input, and saving the session information to DynamoDB.

To use the application, you can deploy the Lambda function to AWS and configure an API Gateway to handle the incoming requests.

The expected input for the Lambda function is a JSON object with the following structure:

```json
{
  "email": "user@example.com",
  "ip": "192.168.1.1"
}
```

The function will then retrieve the geolocation information for the provided IP address, and save the session details (email, IP, city, timezone, country, and coordinates) to DynamoDB.

## API

The application exposes a single API endpoint that accepts POST requests. The endpoint expects the input JSON object as described in the "Usage" section.

Upon successful processing, the Lambda function will return a JSON response with a success message. In case of errors, the function will return an error message with a 400 or 500 status code.

## Contributing

If you would like to contribute to this project, please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Make your changes and ensure the code passes any existing tests.
4. Submit a pull request with a detailed description of your changes.

## License

This project is licensed under the [MIT License](LICENSE).
