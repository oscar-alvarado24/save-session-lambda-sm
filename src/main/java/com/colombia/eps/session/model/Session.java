package com.colombia.eps.session.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "")
public class Session {
    private String email;
    private String connectionTime;
    private String ip;
    private String city;

    @DynamoDBHashKey(attributeName = "id")
    public String getEmail(){
        return this.email;
    }
    @DynamoDBAttribute(attributeName = "id")
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDBRangeKey(attributeName = "connection_time")
    public String getConnectionTime(){
        return this.connectionTime;
    }
    @DynamoDBAttribute(attributeName = "ip")
    public String getIp(){
        return this.ip;
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