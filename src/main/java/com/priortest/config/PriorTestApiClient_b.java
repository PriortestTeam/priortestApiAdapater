package com.priortest.config;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Collections;
import java.util.Map;

public class PriorTestApiClient_b {

    private final String baseUrl;
    private final String userToken;
    private final String userEmail;


    public PriorTestApiClient_b(String userToken, String userEmail, String baseUrl) {
        this.userToken = userToken;
        this.userEmail = userEmail;
        this.baseUrl = baseUrl;

        // Configure RestAssured
        RestAssured.baseURI = baseUrl;
    }

    public Response getRequest(String endpoint) {
        return getRequest(endpoint, Collections.emptyMap());
    }


    private Response putRequest(String endpoint, String body) {
        return RestAssured.given().header("Authorization", userToken).header("emailid", userEmail).header("Content-Type", "application/json").body(body).put(baseUrl + endpoint);
    }

    public Response getRequest(String endpoint, Map<String, String> queryParams) {
        System.out.println(userToken + userEmail);
        RequestSpecification request = RestAssured.given().header("Authorization", userToken).header("emailid", userEmail);
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            request.param(entry.getKey(), entry.getValue());
        }
        return request.get(baseUrl + endpoint);
    }

    private Response postRequest(String endpoint, String body) {
        return RestAssured.given().header("Authorization", userToken).header("emailid", userEmail).header("Content-Type", "application/json").body(body).post(baseUrl + endpoint);
    }

    public Response checkTestCycle(String endpoint, Map<String, String> queryParams) {
        System.out.println("========= testCycle: retrieve api exception");
        return getRequest(endpoint, queryParams);
    }

    public Response createTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public Response checkTestCase(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }

    public Response createTestCase(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public Response addTestCaseIntoTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public Response updateTestCaseStatusInTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public Response createIssue(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public Response closeIssue(String endpoint, String body) {
        return putRequest(endpoint, body);
    }



    public Response checkIssueList(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }

    public Response testCaseInTestCycle(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }

    public Response removeTCsFromTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }
}
