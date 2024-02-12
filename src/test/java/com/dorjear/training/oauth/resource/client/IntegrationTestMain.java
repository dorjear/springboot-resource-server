package com.dorjear.training.oauth.resource.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;


public class IntegrationTestMain {
    private static RestTemplate restTemplate = TestClientConfig.restTemplate();

    public static void main(String[] args) {

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO); // Set root logger level

        getTokenByClientCredentials();

        getTokenByPassword();

        List<String> csrfTokenAndSessionCookie = getCsrfFromLoginPage();
        String sessionCookie = getCookieByLogin(csrfTokenAndSessionCookie.get(1), csrfTokenAndSessionCookie.get(0));
        String authPageCsrfToken = getCsrfTokenFromAuthPage(sessionCookie);
        String authCode = getAuthCode(sessionCookie, authPageCsrfToken);
        List<String> tokenAndRefreshToken = getTokenByAuthCode(authCode);
        System.out.println(tokenAndRefreshToken);
        tokenAndRefreshToken = getTokenByRefreshToken(tokenAndRefreshToken.get(1));
        System.out.println(tokenAndRefreshToken);
        String response = getUserProfile(tokenAndRefreshToken.get(0));
    }

    private static void getTokenByClientCredentials(){

        String url = "http://localhost:8095/oauth/token";

        // Create HttpHeaders
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", TestClientConfig.CLIENT_BASIC_STRING);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create the request body
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");

        // Create an HttpEntity object with headers and body
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        // Make the POST request
        ResponseEntity<OauthTokenResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, OauthTokenResponse.class);

        // Print the response
        System.out.println(response.getBody().getAccessToken());
    }

    private static void getTokenByPassword(){
        String url = "http://localhost:8095/oauth/token";

        // Create HttpHeaders
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", TestClientConfig.CLIENT_BASIC_STRING);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Create the request body
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("username", TestClientConfig.USER_NAME);
        map.add("password", TestClientConfig.USER_PASSWORD);

        // Create an HttpEntity object with headers and body
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        // Make the POST request
        ResponseEntity<OauthTokenResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, OauthTokenResponse.class);

        // Print the response
        System.out.println(response.getBody().getAccessToken());
    }


    private static List<String> getCsrfFromLoginPage() {
        String url = "http://localhost:8095/login";

        // Execute the GET request
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        String keyWord = "<input name=\"_csrf\" type=\"hidden\" value=\"";
        String html = response.getBody();
        int indexOfCsrfToken = html.lastIndexOf(keyWord);
        String csrfToken = html.substring(indexOfCsrfToken+keyWord.length(), indexOfCsrfToken+keyWord.length()+36);
        System.out.println("csrfToken is " + csrfToken);

        HttpHeaders responseHeaders = response.getHeaders();
        String cookies = responseHeaders.getFirst(HttpHeaders.SET_COOKIE);
        String sessionCookie = cookies.substring(cookies.indexOf("JSESSIONID="), cookies.indexOf(";"));
        System.out.println(sessionCookie);

        return List.of(csrfToken, sessionCookie);
    }

    private static String getCookieByLogin(String sessionCookie, String csrfToken) {

        // URL for the login request
        String url = "http://localhost:8095/login";

        // Setup headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", sessionCookie);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", TestClientConfig.USER_NAME);
        map.add("password", TestClientConfig.USER_PASSWORD);
        map.add("_csrf", csrfToken);

        // Combine headers and body into an entity
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

        // Execute the request
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        HttpHeaders responseHeaders = response.getHeaders();
        String cookies = responseHeaders.getFirst(HttpHeaders.SET_COOKIE);
        System.out.println(cookies);
        String responseSessionCookie = cookies.substring(cookies.indexOf("JSESSIONID="), cookies.indexOf(";"));
        System.out.println(responseSessionCookie);
        return responseSessionCookie;
    }

    private static String getCsrfTokenFromAuthPage(String sessionCookie) {
        String url = "http://localhost:8095/oauth/authorize?response_type=code&client_id=oauth2-jwt-client&redirect_uri=http://localhost:3000/frontend/auth";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", sessionCookie);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        System.out.println(response.getBody());
        String keyWord = "<input type=\"hidden\" name=\"_csrf\" value=\"";
        String html = response.getBody();
        int indexOfCsrfToken = html.lastIndexOf(keyWord);
        String csrfToken = html.substring(indexOfCsrfToken+keyWord.length(), indexOfCsrfToken+keyWord.length()+36);
        System.out.println("csrfToken is " + csrfToken);
        return csrfToken;
    }

    private static String getAuthCode(String sessionCookie, String csrfToken) {

        String url = "http://localhost:8095/oauth/authorize";

        // Setup headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Cookie", sessionCookie);

        // Setup the request body with form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("user_oauth_approval", "true");
        map.add("_csrf", csrfToken);
        map.add("authorize", "Authorize");

        // Combine headers and body into an entity
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

        // Execute the POST request
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        // Output the response body
        String location = response.getHeaders().getFirst("Location");
        String authCode = location.substring(location.lastIndexOf("=")+1);
        System.out.println("Auth Code: " + authCode);
        return authCode;
    }

    private static List<String> getTokenByAuthCode(String authCode) {
        String url = "http://localhost:8095/oauth/token";

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", TestClientConfig.CLIENT_BASIC_STRING);

        // Form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("code", authCode);
        map.add("redirect_uri", "http://localhost:3000/frontend/auth");

        // Wrap headers and body in an HttpEntity
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        // Make the POST request
        ResponseEntity<OauthTokenResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, OauthTokenResponse.class);

        // Print the response
        return List.of(response.getBody().getAccessToken(), response.getBody().getRefreshToken());
    }

    private static List<String> getTokenByRefreshToken(String refreshToken) {
        String url = "http://localhost:8095/oauth/token";

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", TestClientConfig.CLIENT_BASIC_STRING);

        // Form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);

        // Wrap headers and body in an HttpEntity
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        // Make the POST request
        ResponseEntity<OauthTokenResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, OauthTokenResponse.class);

        // Print the response
        return List.of(response.getBody().getAccessToken(), response.getBody().getRefreshToken());
    }

    private static String getUserProfile(String token) {

        // URL for the user profile request
        String url = "http://localhost:8096/user/profile";

        // Setup headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Assuming JSON response
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Execute the GET request
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // Output the response body
        System.out.println(response.getBody());

        return response.getBody();
    }
}



