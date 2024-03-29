package com.priortest.run.api;

import com.priortest.config.PTApiConfig;
import com.priortest.config.PTConstant;
import com.priortest.config.PTEndPoint;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;


public class PTApiUtil {
    private static final Logger log = LogManager.getLogger(PTApiUtil.class);

    public static void setUpTestCycle(String testCycleTitle) {
        // if testCycleTitle is there - do not create
        // if testCycleTitle is not there - created
        //  - testCycleTitle passed by main branch
        boolean isTestCyclePresent = testCycleTitlePresent(testCycleTitle);
        if (isTestCyclePresent) {
            log.info("=========testCycle: " + testCycleTitle + " exist=----" + isTestCyclePresent);

        } else {
            log.info("=========start creating testCycle: " + testCycleTitle);
            createTestCycle(testCycleTitle);
        }
    }

    private static boolean testCycleTitlePresent(String testCycleTitle) {
        String PTProjectId = PTConstant.getPTProjectId();
        String payload = "{\"projectId\":" + PTProjectId + "}";
        Response response = PTApiRequest.doGet(PTEndPoint.retrieveTestCycleAsTitle, testCycleTitle);
        if (response == null) {
            log.info("=========testCycle: retrieve api exception");
            return false;
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCycleId = jsonPathEvaluator.get("data.id");
            if (testCycleId.isEmpty()) {
                log.info("=========testCycle: " + testCycleTitle + " does not exist");
                return false;
            } else {
                PTApiConfig.setTestCycleId(testCycleId);
                log.info("=========testCycle: " + testCycleTitle + "set test cycle id" + testCycleId);
                return true;
            }
        }
    }

    private static void createTestCycle(String testCycleTitle) {
        String PTProjectId = PTConstant.getPTProjectId();
        String testCyclePayload = "{\n" + "  \"title\":" + "\"" + testCycleTitle + "\"" + "," + "\n" + "  \"description\":\"sfjsldfjd\",\n" + "  \"version\":\"2.0.0.0\",\n" + "  \"reportTo\":\"huju\",\n" + "  \"planExecuteDate\":\"\",\n" + "  \"testPlatform\":\"Window11\",\n" + "  \"allureReportUrl\":\"\",\n" + "  \"testCycleStatus\":\"可执行\",\n" + "  \"testMethod\":\"自动化\",\n" + "  \"env\":\"用户验收测试\",\n" + "  \"browser\":\"Chrome\",\n" + "  \"testFrame\":\"Java + TestNg\",\n" + "  \"remarks\":\"this is from automation TCs\",\n" + "  \"currentRelease\":1,\n" + "  \"released\":0,\n" + " \"projectId\":" + "\"" + PTProjectId + "\"" + "," + "\n" + "\"customFieldDatas\":{\n" + "  }\n" + "} ";
        Response response = PTApiRequest.doPost(PTEndPoint.createTestCycle, testCyclePayload);
        if (response == null) {
            log.error("failed to create testCycle" + testCycleTitle);
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCycleId = jsonPathEvaluator.get("data.id");
            PTApiConfig.setTestCycleId(testCycleId);
        }
    }


    public static void setUpTestRunInTestCycle(String testCaseId, String status) {
        // verify testCaseId present in Test Cycle
        if (tcInTestCycle(testCaseId)) {
            // update run test case status
            log.info("test case in test Cycle - status update - start");
            updateTCStatus(testCaseId, status);
            log.info("test case in test Cycle - status update done" + status);
        } else {
            log.warn("test case not in test Cycle, start to add test case into test cycle");
            addTcIntoTestCycle(testCaseId);
        }
        log.info("Update Test Case Status- " + status);
        updateTCStatus(testCaseId, status);

    }


    static JSONObject payloadCreate(String projectId , String runCaseId, String testCycleId, int stepStatus,boolean updateMethod) {
        boolean addedOn = updateMethod;
        long caseRunDuration = 12312312;
        int caseTotalPeriod = 3434;
        int runCount = 1;
        String testCaseId = runCaseId;

        // Create a JSONObject and add values to it
        JSONObject payload = new JSONObject();
        payload.put("addedOn", addedOn);
        payload.put("caseRunDuration", caseRunDuration);
        payload.put("caseTotalPeriod", caseTotalPeriod);
        payload.put("projectId", projectId);
        payload.put("runCount", runCount);
        payload.put("runStatus", stepStatus);
        payload.put("testCaseId", testCaseId);
        payload.put("testCycleId", testCycleId);
        log.info("hereid " + payload.toString());
        return payload;
    }

    private static void updateTCStatus(String runCaseId, String status) {
        int stepStatusCode = 5;
        boolean addedOn =false;
        if (status.contains("FAIL")) {

            stepStatusCode = 2;
            addedOn = false;
        } else if (status.contains("PASS")) {
            stepStatusCode = 1;
            addedOn = true;
        }
        else if (status.contains("SKIP")){
            stepStatusCode = 3;
            addedOn = true;
        }
        log.info("========starting run case id update in test cycle " +runCaseId + ""+ stepStatusCode   + "  "+addedOn );
        String PTProjectId = PTConstant.getPTProjectId();
        String testCycleId = PTApiConfig.getTestCycleId();
        JSONObject payload = payloadCreate(PTProjectId, runCaseId, testCycleId, stepStatusCode,addedOn);
        Response response = PTApiRequest.doPostWithPayload(PTEndPoint.updateTestCaseStatusInTestCycle, payload.toString());
        if (response == null) {
            log.error("=============== Failed to test cases status update in to test cycle");
        } else {
           // tcInTestCycle(runCaseId);
            log.info("======== end run case id update in test cycle successfully ");
        }
    }

    private static void addTcIntoTestCycle(String testCaseId) {
        String PTProjectId = PTConstant.getPTProjectId();
        // condition: test case not in testCycle
        //
        String payload = "{\"projectId\":\"" + PTProjectId + "\",\"testCycleId\":\"" + PTApiConfig.getTestCycleId() + "\",\"testCaseIds\":[\"" + testCaseId + "\"]}";
        Response response = PTApiRequest.doPost(PTEndPoint.addTCsIntoTestCycle, payload);
        if (response == null) {
            log.error("=============== Failed to test cases added in to test cycle");
        } else {
            tcInTestCycle(testCaseId);
        }
    }

    private static boolean tcInTestCycle(String testCaseId) {
        Response response = PTApiRequest.doGetTestRunId(PTEndPoint.retrieveTCInTestCycle, testCaseId);
        if (response == null) {
            log.info("=========testCaseIdSearched expected null: " + response == null);
            return false;
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String runTCIdSearched = jsonPathEvaluator.get("data.id");
            log.info("========================" + runTCIdSearched);
            if (runTCIdSearched.isEmpty()) {
                return false;
            } else {
                log.info("========= run case id for testCaseIdSearched " + runTCIdSearched + " exist");
                PTApiConfig.setRunCaseId(runTCIdSearched);
                return true;
            }

        }


    }

    public static void createIssue() {
        log.info("developing.... soon --- FOR createing issue");
    }


    public static void removeTCsFromTestCycle(ArrayList<String> tcLists) {
        String testCycleId = PTApiConfig.getTestCycleId();
        String payload = "{\"testCycleId\":" + testCycleId + "}";
        Response response = PTApiRequest.doPost(PTEndPoint.retrieveAllTCsInTestCycle, payload);

    }

    public static String setUpTestCaseId(String testCaseId) {
        String createdTestCaseId = null;
        Response response = PTApiRequest.doGetTestCaseId(PTEndPoint.retrieveTestCaseInProject, testCaseId);
        if ( response!=null){
            log.info("==== test case id " + testCaseId + " in project");
        }
        else {
            log.info("==== test case id " + testCaseId + " not in project");
            createTestCase();
        }

        return createdTestCaseId;

    }

    private static void createTestCase() {
        log.info("Create Test Case ---- developing ");
        //JsonPath jsonPathEvaluator = response.jsonPath();
        //loginToken = jsonPathEvaluator.get("data.token");

    }
}
