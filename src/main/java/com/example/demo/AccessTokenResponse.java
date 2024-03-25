package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public record AccessTokenResponse(@JsonProperty("access_token") String accessToken,
                                  @JsonProperty("expires_in") long expiresIn,
                                  @JsonProperty("token_type") String tokenType, String scope) implements Serializable {}
