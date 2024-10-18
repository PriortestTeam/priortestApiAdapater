package com.priortest.config;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Collections;
import java.util.Map;

public class PriorTestApiClient {

    private static String baseUrl = PTConstant.getPTBaseURI();
    private static String userToken = PTConstant.getPTToken();
    private static String userEmail = PTConstant.getPTEmail();

    public Response getRequest(String endpoint) {
        return getRequest(endpoint, Collections.emptyMap());
    }

    private static Response putRequest(String endpoint, String body) {
        return RestAssured.given().header("Authorization", userToken).header("emailid", userEmail).header("Content-Type", "application/json").body(body).put(baseUrl + endpoint);
    }

    public static Response getRequest(String endpoint, Map<String, String> queryParams) {
        RequestSpecification request = RestAssured.given().header("Authorization", userToken).header("emailid", userEmail).log().body().log().uri();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            request.param(entry.getKey(), entry.getValue());
        }
        return request.get(baseUrl + endpoint);
    }

    private static Response postRequest(String endpoint, String body) {
        return RestAssured.given().header("Authorization", userToken).header("emailid", userEmail).header("Content-Type", "application/json").body(body).log().body().log().uri().post(baseUrl + endpoint);
    }

    public static Response checkTestCycle(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }

    public static Response createTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public static Response checkTestCase(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }

    public static Response createTestCase(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public static Response addTestCaseIntoTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public static Response updateTestCaseStatusInTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public static Response createIssue(String endpoint, String body) {
        return postRequest(endpoint, body);
    }

    public static Response closeIssue(String endpoint, String body) {
        return putRequest(endpoint, body);
    }



    public static Response checkIssueList(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }

    public static Response checkIssueStatus(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }


    public static Response testCaseInTestCycle(String endpoint, Map<String, String> queryParams) {
        return getRequest(endpoint, queryParams);
    }

    public static Response removeTCsFromTestCycle(String endpoint, String body) {
        return postRequest(endpoint, body);
    }
}
