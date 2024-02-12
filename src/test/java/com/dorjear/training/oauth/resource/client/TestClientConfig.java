package com.dorjear.training.oauth.resource.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class TestClientConfig {

    public static final String CLIENT_BASIC_STRING = "Basic b2F1dGgyLWp3dC1jbGllbnQ6YWRtaW4xMjM0"; //oauth2-jwt-client:admin1234
    public static final String USER_NAME = "admin";
    public static final String USER_PASSWORD = "admin1234";
    //Not use in this lab. Just keep the code
    private static RestTemplate restTemplateWithCookieStore() {
        CookieStore cookieStore = new BasicCookieStore();
        // Set up HttpClient with the cookie store
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        // Create a request factory with the HttpClient that contains the cookie store
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }

    public static RestTemplate restTemplate() {

        RestTemplate restTemplate = new RestTemplate();

        // Find and replace the MappingJackson2HttpMessageConverter with a custom one that accepts snake case
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        restTemplate.getMessageConverters().forEach(converter -> {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                messageConverters.add(customJackson2HttpMessageConverter());
            } else {
                messageConverters.add(converter);
            }
        });

        restTemplate.setMessageConverters(messageConverters);
        restTemplate.setInterceptors(List.of(new StatefulRestTemplateInterceptor()));
        return restTemplate;
    }

    private static HttpMessageConverter<?> customJackson2HttpMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        // Add any other custom configuration here
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Example of additional configuration

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }
}