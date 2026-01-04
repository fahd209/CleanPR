package com.fahd.cleanPR.util;

import com.fahd.cleanPR.model.CommentDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fahd.cleanPR.CleanPrConstants.*;

@Service
public class GitHubServiceCaller {


    private RestTemplate restTemplate;

    @Autowired
    public GitHubServiceCaller(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public JsonNode fetchFilePaths(String accessToken,
                                   String uri) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(AUTHORIZATION, BEARER + accessToken);
        httpHeaders.set(ACCEPT, GITHUB_REQUEST_BODY_TYPE);
        httpHeaders.set("User-Agent", "clean-pr/1.0");

        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                httpEntity,
                String.class
        );
        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode;
    }

    public List<String> fetchFileContent(List<String> rawUrls,
                                         String accessToken) {
        List<String> fileContent = new ArrayList<>();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(AUTHORIZATION, BEARER + accessToken);
        httpHeaders.set(ACCEPT, GITHUB_REQUEST_BODY_TYPE);
        httpHeaders.set("User-Agent", "clean-pr/1.0");
        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);

        for (String url : rawUrls) {

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    httpEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            String downloadUrl = (String) response.getBody().get("download_url");
            String fileName = (String) response.getBody().get("name");

            ResponseEntity<String> fileResponse = restTemplate.exchange(
                    downloadUrl,
                    HttpMethod.GET,
                    httpEntity,
                    String.class
            );
            fileContent.add(fileName + "\n " + fileResponse.getBody());
        }

        return fileContent;
    }

    public ResponseEntity<String> postReview(String url,
                                             String summary,
                                             List<CommentDTO> codeCommentList,
                                             String accessToken) {
        ResponseEntity<String> response = null;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", BEARER + accessToken);
        httpHeaders.set("User-Agent", "clean-pr/1.0");
        httpHeaders.set(ACCEPT, GITHUB_REQUEST_BODY_TYPE);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("body", summary);
            body.put("event", "COMMENT");
            body.put("comments", codeCommentList);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);

            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity, String.class
            );
        } catch (Exception e) {
            // if an error happened post to pr with intruction on what to do
            Map<String, Object> body = new HashMap<>();
            body.put("body", "We were unable to review the code. Please try closing the pr opening it again," +
                    "if it still doesn't work please try again later :)");
            body.put("event", "COMMENT");

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, httpHeaders);
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity, String.class
            );
        }
        return response;
    }

    /**
     * This method is called
     * when a user first
     * opens a pr to let them know
     * there PR is being reviewed
     */
    public ResponseEntity<String> postWelcomeMessage(
            String url,
            String accessToken
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", BEARER + accessToken);
        headers.set("User-Agent", "clean-pr/1.0");
        headers.set(ACCEPT, GITHUB_REQUEST_BODY_TYPE);
        Map<String, Object> body = new HashMap<>();
        body.put("body", "Thanks for using cleanPR your code is being reviewed :)");
        body.put("event", "COMMENT");
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        return response;
    }

}
