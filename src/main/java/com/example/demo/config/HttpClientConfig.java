package com.example.demo.config;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ssl.TLS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;


@Configuration
public class HttpClientConfig {

    @Value("${the.bundle.to-use}")
    String bundleName;

    @Bean
    public HttpClient httpClientWithSSL(SslBundles sslBundles) {
        SslBundle sslBundle = sslBundles.getBundle(bundleName);
        SSLContext sslContext = sslBundle.createSslContext();
        HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .sslContext(sslContext)
                .connectTimeout(Duration.of(10000, ChronoUnit.SECONDS)).build();
        return httpClient;
    }

    @Bean
    public CloseableHttpClient apacheHttpClientWithSSL(SslBundles sslBundles) {
        SslBundle sslBundle = sslBundles.getBundle(bundleName);
        SSLContext sslContext = sslBundle.createSslContext();
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create().setSslContext(sslContext).setTlsVersions(TLS.V_1_3,TLS.V_1_2).build();
        HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslSocketFactory)
                .setDefaultTlsConfig(TlsConfig.DEFAULT).build();
        CloseableHttpClient httpClientImpl = HttpClients.custom().setConnectionManager(cm).build();
        return httpClientImpl;
    }
}
