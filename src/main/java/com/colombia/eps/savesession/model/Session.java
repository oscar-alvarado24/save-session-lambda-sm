package com.colombia.eps.savesession.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class Session {
    private String email;
    private String connectionTime;
    private String ip;
    private String city;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getEmail(){
        return this.email;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("connection_time")
    public String getConnectionTime(){
        return this.connectionTime;
    }


    @Override
    public String toString() {
        return "{\n" +
                "        \"email\": \"" + this.email + "\",\n" +
                "        \"connectionTime\": \"" + this.connectionTime + "\",\n" +
                "        \"ip\": \"" + this.ip + "\",\n" +
                "        \"city\": \"" + this.city + "\"\n" +
                "    }";
    }
}
