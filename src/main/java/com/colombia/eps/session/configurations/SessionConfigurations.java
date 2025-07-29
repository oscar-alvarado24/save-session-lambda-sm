package com.colombia.eps.session.configurations;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.colombia.eps.session.service.City;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.net.http.HttpClient;
import java.util.TimeZone;


@Configuration
@Slf4j
public class SessionConfigurations {

    @Value("${aws.dynamo.endpoint:http://localhost:4566}")
    private String dynamoDbEndpoint;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    @Profile("local")
    public AmazonDynamoDB localAmazonDynamoDB() {
        log.debug("Configurando dynamo en url: {} y region: {}",this.dynamoDbEndpoint, this.awsRegion);
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                dynamoDbEndpoint,
                                awsRegion
                        )
                )
                .build();
    }

    @Bean
    @Profile("!local")
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.defaultClient();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setTimeZone(TimeZone.getTimeZone("America/Bogota"));
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public City city(HttpClient httpClient, ObjectMapper objectMapper) {
        return new City(httpClient, objectMapper);
    }
}