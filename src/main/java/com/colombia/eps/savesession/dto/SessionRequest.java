package com.colombia.eps.savesession.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionRequest(
        @JsonProperty("email") String email,
        @JsonProperty("ip") String ip
) {
    @JsonCreator
    public SessionRequest(
            @JsonProperty("email") String email,
            @JsonProperty("ip") String ip
    ) {
        this.email = email;
        this.ip = ip;
    }
}