package com.example.demo.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.AccessTokenResponse;

import com.nimbusds.oauth2.sdk.GrantType;


@RestController
public class ManualOauthRequestController {

    private static Logger logger = LoggerFactory.getLogger(ManualOauthRequestController.class);

    @Value("${the.resource.endpoint}")
    private String resourceEndpoint;
    
    @Value("${the.authorization.client-id}")
    private String clientId;
    
    @Value("${the.authorization.client-secret}")
    private String clientSecret;
    
    @Value("${the.authorization.token-uri}")
    private String tokenUri;

    @Value("${the.authorization.resource}")
    private String adfsResource;
    
    @Value("${the.authorization.scope}")
    private String scope;

    @Autowired
    HttpClient httpClientWithSSL;

    private String getCCGToken(String scope) throws IOException, InterruptedException, URISyntaxException {
        //logger.info("client {} secret {} type {} scope {}", clientId, clientSecret, GrantType.CLIENT_CREDENTIALS.getValue(), scope);
        var req = Map.of("client_id", clientId
                ,"client_secret", clientSecret
                ,"grant_type", GrantType.CLIENT_CREDENTIALS.getValue()
                ,"resource", adfsResource
                ,"scope", scope);
        return getAccessToken(tokenUri, req).accessToken();
    }

    private AccessTokenResponse getAccessToken(String uri, Map<String, String> requestMap) throws IOException, InterruptedException, URISyntaxException {
        var targetUri = new URI(uri);
        var requestBody = toUrlencodedFormData(requestMap);
        var request = HttpRequest.newBuilder()
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .uri(targetUri).build();
        HttpResponse<String> response = getStringHttpResponse(request);
        if (response.statusCode() > 399) {
            logger.error("Failed to fetch new OAuth 2 access token for scope {}, response {} on {} \nResponse: {}", scope, response, targetUri, response.body());
            throw new RuntimeException("Failed to fetch new OAUth2 access token for client_credentials flow scope " + scope + " http status was " + response.statusCode());
        }
        var objMapper = new ObjectMapper();
        var atr = objMapper.readValue(response.body(), AccessTokenResponse.class);
        logger.info("Stored access token in cache {}", atr.accessToken());

        return atr;
    }

    private HttpResponse<String> getStringHttpResponse(HttpRequest request) throws IOException, InterruptedException {


        var response = httpClientWithSSL.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    private String consumeRestAPI(String uri) throws IOException, InterruptedException, URISyntaxException {


        var targetUri = new URI(uri);
        var request = HttpRequest.newBuilder()
                //.header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization","Bearer " +  getCCGToken(scope))
                //.POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .uri(targetUri).build();
        HttpResponse<String> response = getStringHttpResponse(request);
        if (response.statusCode() > 399) {
            logger.error("Failed to call target with headers {}, response {} on {} \nResponse: {}",  request.headers(), response, targetUri, response.body());
            throw new RuntimeException("Failed to call target for client_credentials flow scope " + scope + " http status was " + response.statusCode());
        }
        //var objMapper = new ObjectMapper();
        //var atr = objMapper.readValue(response.body(), AccessTokenResponse.class);
        logger.info("Result from REST server {}", response.body());
        return response.body();

    }

    private String toUrlencodedFormData(Map<?,?> map) {
        return map.entrySet().stream().map(entry -> String.join("=",
                urlEncode(entry.getKey().toString()),
                urlEncode(entry.getValue().toString()))
        ).collect(Collectors.joining("&"));
    }
    private String urlEncode(String unencoded) {
        return URLEncoder.encode(unencoded, StandardCharsets.UTF_8);
    }
    @GetMapping("/manual-request-oauth")
    public String obtainSecuredResource() throws IOException, URISyntaxException, InterruptedException {
        logger.info("trying with http client...");
        return consumeRestAPI(resourceEndpoint);
    }
}
