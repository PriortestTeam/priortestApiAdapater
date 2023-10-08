package com.priortest.run.api;

import com.priortest.config.PTApiConfig;
import com.priortest.config.PTConstant;
import com.priortest.config.PTEndPoint;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PTApiUtil {
    private static final Logger log = LogManager.getLogger(PTApiUtil.class);

    public static void setUpTestCycle(String testCycleTitle) {
        // if testCycleTitle is there - do not create
        // if testCycleTitle is not there - created
        //    - testCycleTitle passed by main branch
        if (testCycleTitlePresent(testCycleTitle)) {
            log.info("=========testCycle: " + testCycleTitle + " exist");

        } else {
            log.info("=========start creating testCycle: " + testCycleTitle);
            createTestCycle(testCycleTitle);
        }

    }

    private static boolean testCycleTitlePresent(String testCycleTitle) {
        String PTProjectId = PTConstant.getPTProjectId();
        String payload = "{\"projectId\":" + PTProjectId + "}";
        Response response = PTApiRequest.doPost(PTEndPoint.retrieveTestCycles, payload);
        if (response == null) {
            log.info("=========testCycle: " + testCycleTitle + " does not exist");
            return false;

        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCycleTitleInSystem = jsonPathEvaluator.get("data.list.title[0]");
            log.info("=========testCycle: " + testCycleTitle + " exist");
            if (testCycleTitleInSystem.contentEquals(testCycleTitle)) {
                PTApiConfig.setTestCycleId(jsonPathEvaluator.get("data.list.id[0]"));
            }
            return testCycleTitleInSystem.contentEquals(testCycleTitle);
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


    public static void setUpTestRunInTestCycle(String testCycleId, String testCaseId, String status) {
        // verify testCaseId present in Test Cycle
        if (tcInTestCycle(testCycleId, testCaseId)) {
            log.info("test case in test Cycle");
        } else {
            addTcIntoTestCycle(testCycleId, testCaseId, status);
        }


    }

    private static void updateTCStatus(String status) {
        log.info("========developing ...");
    }

    private static void addTcIntoTestCycle(String testCycleId, String testCaseId, String status) {
        String PTProjectId = PTConstant.getPTProjectId();
        String payload = "{\"projectId\":\"" + PTProjectId + "\",\"testCycleId\":\"" + testCycleId + "\",\"testCaseIds\":[\"" + testCaseId + "\"]}";
        Response response = PTApiRequest.doPost(PTEndPoint.addTCsIntoTestCycle, payload);
        updateTCStatus(status);

    }

    private static boolean tcInTestCycle(String testCycleId, String testCaseId) {
        String PTProjectId = PTConstant.getPTProjectId();

        String payload = "{\"testCycleId\":" + testCycleId + "}";
        Response response = PTApiRequest.doPost(PTEndPoint.retrieveAllTCsInTestCycle, payload);
        if (response == null) {
            log.info("=========testCaseIdSearched expected null: " + response == null);
            return false;
        } else {

            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCaseResult = jsonPathEvaluator.get("data.total");
            if (testCaseResult.contentEquals("0")) {
                return false;
            } else {
                String testCaseIdSearched = jsonPathEvaluator.get("data.list.id[0]");
                log.info("=========testCaseIdSearched: " + testCaseIdSearched + " exist");
                return testCaseId.contentEquals(testCaseIdSearched);
            }

        }


    }

}
