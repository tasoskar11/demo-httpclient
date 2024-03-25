package com.example.demo.web;

import com.example.demo.AccessTokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.GrantType;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import java.net.URISyntaxException;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
public class ManualOauthRequestApacheHttpClientController {

    private static Logger logger = LoggerFactory.getLogger(ManualOauthRequestApacheHttpClientController.class);

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
    CloseableHttpClient apacheHttpClientWithSSL;

    private String getCCGToken(String scope) throws IOException, InterruptedException, URISyntaxException, ParseException {

        var req = Map.of("client_id", clientId
                ,"client_secret", clientSecret
                ,"grant_type", GrantType.CLIENT_CREDENTIALS.getValue()
                ,"resource", adfsResource
                ,"scope", scope);
        return getAccessToken(tokenUri, req).accessToken();
    }

    private AccessTokenResponse getAccessToken(String uri, Map<String, String> map) throws IOException, InterruptedException, URISyntaxException, ParseException {

        var httpPost = new HttpPost(uri);
        httpPost.setHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        httpPost.setHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        StringEntity entity = new StringEntity(toUrlencodedFormData(map));
        httpPost.setEntity(entity);
        var response = apacheHttpClientWithSSL.execute(httpPost);
        if (response.getCode() > 399) {
            logger.error("Failed to fetch new OAuth2 access token for scope {}, response {} on {} \nResponse: {}", scope, response, uri, EntityUtils.toString(response.getEntity()));
            throw new RuntimeException("Failed to fetch new OAUth2 access token for client_credentials flow scope " + scope + " http status was " + response.getCode());
        }
        var objMapper = new ObjectMapper();
        var atr = objMapper.readValue(EntityUtils.toString(response.getEntity()), AccessTokenResponse.class);
        logger.info("Should have Stored access token in cache {}", atr.accessToken());
        return atr;
    }

    private String consumeRestAPI(String uri) throws IOException, InterruptedException, URISyntaxException, ParseException {
        var httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpGet.setHeader("Authorization","Bearer " +  getCCGToken(scope));
        var response = apacheHttpClientWithSSL.execute(httpGet);
        return EntityUtils.toString(response.getEntity());
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
    @GetMapping("/manual-apache-request-oauth")
    public String obtainSecuredResource() throws IOException, URISyntaxException, InterruptedException, ParseException {
        logger.info("trying with http client...");
        return consumeRestAPI(resourceEndpoint);
    }
}
