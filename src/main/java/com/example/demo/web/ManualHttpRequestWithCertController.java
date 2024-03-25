package com.example.demo.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;



@RestController
public class ManualHttpRequestWithCertController {

    private static Logger logger = LoggerFactory.getLogger(ManualHttpRequestWithCertController.class);

    @Value("${the.resource.endpoint}")
    private String resourceEndpoint;

    @Autowired
    HttpClient httpClientWithSSL;

    private HttpResponse<String> getStringHttpResponse(HttpRequest request) throws IOException, InterruptedException {
        var response = httpClientWithSSL.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    private String consumeRestAPI(String uri) throws IOException, InterruptedException, URISyntaxException {
        var targetUri = new URI(uri);
        var request = HttpRequest.newBuilder()
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .uri(targetUri)
                .build();
        HttpResponse<String> response = getStringHttpResponse(request);
        if (response.statusCode() > 399) {
            logger.error("Failed to call REST url with headers {}, response {} on {} \nResponse: {}",  request.headers(), response, targetUri, response.body());
            throw new RuntimeException("Failed to consume the REST resource http status was " + response.statusCode());
        }
        logger.info("Result from REST server {}", response.body());
        return response.body();
    }

    @GetMapping("/consume-rest-with-mtls")
    public String obtainSecuredResource() throws IOException, URISyntaxException, InterruptedException {
        logger.info("Trying to consume an endpoint with a client certificate that the REST server will use to authenticate the client.");
        return consumeRestAPI(resourceEndpoint);
    }
}
