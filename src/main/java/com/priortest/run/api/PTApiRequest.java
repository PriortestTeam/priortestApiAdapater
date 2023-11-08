package com.priortest.run.api;

import com.priortest.config.PTApiConfig;
import com.priortest.config.PTConstant;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import static java.lang.Long.parseLong;

public class PTApiRequest {
    private static final Logger log = LogManager.getLogger(PTApiRequest.class);
    static RequestSpecification httpRequest;
    static String loginToken;
    static String loginPayload = "{\n" + "  \"password\": \"Hjyhappy123!\",\n" + "  \"username\": \"qatest.hu.mary3@gmail.com\"\n" + "}";


    public static void doLogin() {
        RestAssured.baseURI = "http://43.139.159.146:8082/api";
        httpRequest = RestAssured.given();

        log.info("=========" + "start user login");
        httpRequest.body(loginPayload);
        httpRequest.contentType("application/json");
        httpRequest.log().all();
        Response response = httpRequest.post("/login");
        int responseCode = response.statusCode();
        Assert.assertEquals(responseCode, 200);
        if (responseCode == 200) {
            JsonPath jsonPathEvaluator = response.jsonPath();
            loginToken = jsonPathEvaluator.get("data.token");
        }
        log.info("=========" + "end user login" + loginToken);
    }

    public static void doLogout() {
        httpRequest = RestAssured.given();
        log.info("=========" + "start user logout");
        httpRequest.contentType("application/json");
        httpRequest.log().all();
        httpRequest.header("Authorization", "Bearer " + loginToken);
        Response response = httpRequest.get("/logout");
        int responseCode = response.statusCode();
        Assert.assertEquals(responseCode, 200);
        if (responseCode == 200) {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String logout = jsonPathEvaluator.get("data.msg");
            Assert.assertTrue(logout.contains("注销成功。"));
            log.info("=========" + "user logout successfully");
        }
        log.info("=========" + "end user logout");
    }


    public static Response doPost(String endPoint, String payload) {
        doLogin();
        httpRequest = RestAssured.given();
        if (payload != null) {
            httpRequest.body(payload);
        }
        httpRequest.contentType("application/Json");
        httpRequest.header("Authorization", "Bearer " + loginToken);
        log.info("============loginToken" + loginToken);
        httpRequest.log().all();
        Response response = httpRequest.post(endPoint);
        log.info("============post from API response " + response.asString());
        int responseCode = response.statusCode();
        doLogout();
        if (responseCode == 200) {
            return response;
        }
        return null;
    }

    public static Response doGet(String endPoint,String parameter) {
        RestAssured.baseURI = PTConstant.getPTBaseURI();
        httpRequest = RestAssured.given();
        httpRequest.param("title", parameter);
        httpRequest.contentType("application/Json");
        httpRequest.header("Authorization", "7d585s07pc6t3hcxsf32w827gjqsyizh9959750sqbck2p088g");
        httpRequest.header("emailid", "qatest.hu.mary3@gmail.com");
        httpRequest.log().all();
        Response response = httpRequest.get(endPoint);
        int responseCode = response.statusCode();
        if (responseCode == 200) {
            return response;
        }
        return null;
    }

    public static Response doGetTestRunId(String endPoint,String parameter) {
        RestAssured.baseURI=PTConstant.getPTBaseURI();
        httpRequest = RestAssured.given();
        httpRequest.param("testCaseId", parameter);
        httpRequest.param("testCycleId",  PTApiConfig.getTestCycleId());
        httpRequest.contentType("application/Json");
        httpRequest.header("Authorization", "7d585s07pc6t3hcxsf32w827gjqsyizh9959750sqbck2p088g");
        httpRequest.header("emailid", "qatest.hu.mary3@gmail.com");
        httpRequest.log().all();
        Response response = httpRequest.get(endPoint);
        log.info("testingn-------------- "+ response.asString());
        int responseCode = response.statusCode();
        if (responseCode == 200) {
            return response;
        }
        return null;
    }

    public void doDelete(String endPoint, String payload) {
        //httpRequest = RestAssured.given();
        RequestSpecBuilder reqBuilder = new RequestSpecBuilder();
        reqBuilder.setBaseUri(PTConstant.getPTBaseURI());
        reqBuilder.addHeader("Authorization", "Bearer " + loginToken);
        //reqBuilder.addParam("demoParam2", "demoParam2value");
        //reqBuilder.addPathParam("demoPathParam2", "demoPathParam2value");
        reqBuilder.setBody(payload);
        reqBuilder.setBasePath(endPoint);

        RequestSpecification reqSpec = reqBuilder.build();
        Response Response = RestAssured.given(reqSpec).when().get();

    }

}
