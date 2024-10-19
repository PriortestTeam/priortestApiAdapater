package com.priortest.run.api;

import com.priortest.config.*;
import com.priortest.model.ConfigManager;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PTApiUtil {
    private static final Logger log = LogManager.getLogger(PTApiUtil.class);
    public static List<String> testCaseIds = new ArrayList<>();
    static String PTProjectId = PTConstant.getPTProjectId();
    static ConfigManager configManager = ConfigManager.getInstance();
    private String casePayload;

    public static void setUpTestCycle(String testCycleTitle) {
        // if testCycleTitle is there - do not create
        // if testCycleTitle is not there - created
        //  - testCycleTitle passed by main branch
        log.info("========= testCycle Title: Verify " + testCycleTitle + "Present In DB");
        boolean isTestCyclePresent = testCycleTitlePresent(testCycleTitle);
        if (isTestCyclePresent) {
            log.debug("========= testCycle: " + testCycleTitle + " Exist ");
        } else {
            log.warn("========= Start Creating testCycle: " + testCycleTitle);
            createTestCycle(testCycleTitle);
        }
    }

    private static boolean testCycleTitlePresent(String testCycleTitle) {
        Map<String, String> queryParameter = new HashMap<>();
        queryParameter.put("title", testCycleTitle);
        Response response = PriorTestApiClient.checkTestCycle(PTEndPoint.retrieveTestCycleAsTitle, queryParameter);
        log.debug("================= Here Is Response" + response.asString());
        if (response.statusCode() != 200) {
            log.error("API Request Code: " + response.statusCode() + " 查无记录");
            return false;
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCycleId = jsonPathEvaluator.get("data.id");
            if (testCycleId.isEmpty()) {
                log.warn("=========testCycle: " + testCycleTitle + " Does Not Exist");
                return false;
            } else {
                log.info("=========testCycle: " + testCycleTitle + " Set Test Cycle ID: " + testCycleId);
                PTApiConfig.setTestCycleId(testCycleId);
                return true;
            }
        }
    }

    private static void createTestCycle(String testCycleTitle) {
        String testCyclePayload = testCyclePayload(testCycleTitle);
        Response response = PriorTestApiClient.createTestCycle(PTEndPoint.createTestCycle, testCyclePayload);
        if (response == null) {
            log.error("======== Failed: Create testCycle " + testCycleTitle);
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCycleId = jsonPathEvaluator.get("data.id");
            PTApiConfig.setTestCycleId(testCycleId);
            PTConstant.PT_TEST_CYCLE_CREATION = true;  // set test cycle is fresh creation
            log.info("========== Passed: Create testCycle " + testCycleId + " For Title  " + testCycleTitle);
        }
    }

    static String currentDataFormation() {
        // Get the current date and time
        LocalDateTime currentTime = LocalDateTime.now();
        // Define the desired format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // Format the current date and time
        return currentTime.format(formatter);
    }

    public static JSONObject customFiledData(Map<String, String> customField) {
        JSONObject customFieldDatas = new JSONObject();
        for (Map.Entry<String, String> entry : customField.entrySet()) {
            log.info("==== Key: " + entry.getKey() + ",==== Value: " + entry.getValue());
            customFieldDatas.put(entry.getKey(), entry.getValue());
        }
        return customFieldDatas;
    }

    public static void addTestCaseId(String caseId) {
        testCaseIds.add(caseId);
    }

    public static JSONArray getTestCaseIds() {
        return new JSONArray(testCaseIds); // Returning a copy to avoid external modification
    }

    public static String removedTCsPayload() {
        JSONObject removedTCsPayload = new JSONObject();
        removedTCsPayload.put("projectId", PTProjectId);
        removedTCsPayload.put("testCycleId", PTApiConfig.getTestCycleId());
        removedTCsPayload.put("testCaseIds", getTestCaseIds());
        return removedTCsPayload.toString();
    }

    public static String testCyclePayload(String testCycleTitle) {
        // Create a JSONObject and add values to it
        JSONObject testCyclePayload = new JSONObject();
        testCyclePayload.put("title", testCycleTitle);
        testCyclePayload.put("description", "this is from testing ");
        testCyclePayload.put("version", PTConstant.getPTVersion());
        testCyclePayload.put("projectId", PTProjectId);
        testCyclePayload.put("planExecuteDate", currentDataFormation());
        testCyclePayload.put("testPlatform", PTConstant.getPTPlatform());
        testCyclePayload.put("reportTo", "huju");
        testCyclePayload.put("testCycleStatus", "可执行");
        testCyclePayload.put("testMethod", "自动");
        testCyclePayload.put("env", PTConstant.getPTEnv());
        testCyclePayload.put("browser", "Chrome");
        testCyclePayload.put("testFrame", "Java + TestNg");
        testCyclePayload.put("released", 0);
        testCyclePayload.put("currentRelease", 0);
        testCyclePayload.put("allureReportUrl", "http://wwww.google.com");

        Map<String, String> custom = new HashMap();
        testCyclePayload.put("customFieldDatas", customFiledData(custom));
        testCyclePayload.put("remarks", "this is from auto script");
        return testCyclePayload.toString();
    }

    public static void setUpTestRunInTestCycle(String testCaseId, String status) {
        if (PTApiConfig.getIsTestCaseNewCreation()) {
            // new test case created
            log.info("======== New Test Case Created, Start To Add Test Case " + testCaseId + " Into testCycle  ======= " + PTApiConfig.getTestCycleId());
            addTcIntoTestCycle(testCaseId);
            log.info("======== Test Case " + testCaseId + " In testCycle - Status Update - Start");
            boolean updateTCStatus = updateTCStatus(status);
            if (updateTCStatus) {
                log.info("======== Passed: Run Case Status  " + status + " Update In testCycle - End =========");
            }
        } else {
            // verify testCaseId present in testCycle
            if (isTCInTestCycle(testCaseId)) {
                // update run test case status
                log.info("Test Case In testCycle - Status Update - Start");
                boolean updateTCStatus = updateTCStatus(status);
                if (updateTCStatus) {
                    log.info("======== Passed: Run Case Status  " + status + " Update In testCycle - End =========");
                } else {
                    log.error("======== Failed: Run Case Status Update In testCycle - End =========");
                }
            } else { // when testCaseId not present in testCycle
                log.warn("======== Test Case Not In Test Cycle, Start To Add Test Case " + testCaseId + " Into testCycle  ======= " + PTApiConfig.getTestCycleId());
                addTcIntoTestCycle(testCaseId);
                log.info("======== Test Case " + testCaseId + " In testCycle - Status Update - Start");
                boolean updateTCStatus = updateTCStatus(status);
                if (updateTCStatus) {
                    log.info("======== Passed: Run Case Status  " + status + " Update In testCycle - End =========");
                } else {
                    log.error("======== Failed:  Run Case Status Update In testCycle - End =========");
                }
            }
        }
    }

    static JSONObject runCaseStatusUpdatePayload(int stepStatus, boolean updateMethod) {
        boolean addedOn = updateMethod;
        long caseRunDuration = PTApiFieldSetup.getRunDuration();
        int runCount = 1;

        // Create a JSONObject and add values to it
        JSONObject payload = new JSONObject();
        payload.put("addedOn", addedOn);
        payload.put("caseRunDuration", caseRunDuration);
        payload.put("caseTotalPeriod", caseRunDuration);
        payload.put("projectId", PTProjectId);
        payload.put("runCount", runCount);
        payload.put("runStatus", stepStatus);
        payload.put("testCaseId", PTApiConfig.getTestCaseId());
        payload.put("testCycleId", PTApiConfig.getTestCycleId());
        log.info("==== Update runCase Status ===== " + payload);
        return payload;
    }

    static String issueCreatePayload() {
        if (!PTApiConfig.getIsIssuePayloadFromUser()) {
            JSONObject issueCPayload = new JSONObject();
            issueCPayload.put("browser", "browser");
            issueCPayload.put("caseCategory", PTApiFieldSetup.getCategory());
            issueCPayload.put("description", "THIS IS From Automation");
            issueCPayload.put("projectId", PTProjectId);
            issueCPayload.put("env", PTConstant.getPTEnv());
            issueCPayload.put("fixVersion", "");
            issueCPayload.put("issueStatus", "新建");
            issueCPayload.put("issueVersion", PTConstant.getPTVersion());
            issueCPayload.put("module", PTApiFieldSetup.getModule());
            issueCPayload.put("planFixDate", "");
            issueCPayload.put("platform", PTConstant.getPTPlatform());
            issueCPayload.put("reportTo", "Test Person");
            issueCPayload.put("priority", PTApiFieldSetup.getPriority());
            issueCPayload.put("severity", PTApiFieldSetup.getSeverity());
            issueCPayload.put("verifiedResult", "");
            issueCPayload.put("title", PTApiFieldSetup.getTitle());
            issueCPayload.put("testDevice", PTConstant.getPTPlatform());
            issueCPayload.put("runcaseId", PTApiConfig.getRunCaseId());
            JSONObject customFieldDatas = new JSONObject();
            issueCPayload.put("customFieldDatas", customFieldDatas);
            return issueCPayload.toString();
        }
        else {
            String issuePayload = PTApiPayloadConfig.getIssuePayload();
            log.info("============== Issue Payload Created By User :" + issuePayload);
            return issuePayload;
        }

    }

    private static boolean updateTCStatus(String status) {
        int stepStatusCode = 5;
        boolean addedOn = false;
        if (status.contains("FAIL")) {
            stepStatusCode = 2;
            addedOn = false;
        } else if (status.contains("PASS")) {
            stepStatusCode = 1;
            addedOn = true;
        } else if (status.contains("SKIP")) {
            stepStatusCode = 3;
            addedOn = true;
        }
        JSONObject payload = runCaseStatusUpdatePayload(stepStatusCode, addedOn);
        Response response = PriorTestApiClient.updateTestCaseStatusInTestCycle(PTEndPoint.updateTestCaseStatusInTestCycle, payload.toString());
        log.info("============= Update TC status : " + response.asString());
        return response != null;
    }

    private static void addTcIntoTestCycle(String testCaseId) {
        Response response = PriorTestApiClient.addTestCaseIntoTestCycle(PTEndPoint.addTCsIntoTestCycle, addTcIntoTestCyclePayload(testCaseId));
        if (response == null) {
            log.error("=============== Failed added test case into to test cycle");
        } else {
            isTCInTestCycle(testCaseId);
        }
    }

    private static boolean isTCInTestCycle(String testCaseId) {
        Map<String, String> queryTCInTestCycle = new HashMap<>();
        queryTCInTestCycle.put("testCaseId", testCaseId);
        queryTCInTestCycle.put("testCycleId", PTApiConfig.getTestCycleId());
        Response response = PriorTestApiClient.isTestCaseInTestCycle(PTEndPoint.retrieveTCInTestCycle, queryTCInTestCycle);
        if (response == null) {
            log.warn("=========testCaseId Searched Response: " + response);
            return false;
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String runTCIdSearched = jsonPathEvaluator.get("data.id");
            if (runTCIdSearched.isEmpty()) {
                log.warn("======== Failed to Retrieve runCase Id " + runTCIdSearched + " Of testCase Id " + testCaseId);
                return false;
            } else {
                log.info("======== Retrieved runCase Id " + runTCIdSearched + " of testCase Id " + testCaseId +"In testCycle");
                PTApiConfig.setRunCaseId(runTCIdSearched);
                return true;
            }

        }


    }

    public static void createIssue() {
        log.info("=== Start to Create issue For testCase Id" + PTApiFieldSetup.getTitle()) ;
        Response response = PriorTestApiClient.createIssue(PTEndPoint.createIssue, issueCreatePayload());
        log.info(response.asString());

    }

    public static void removeTCsFromTestCycle() {
        log.info("=== Start to Remove Extra TC From testCycle: " + removedTCsPayload());
        Response response = PriorTestApiClient.removeTCsFromTestCycle(PTEndPoint.retrieveAllTCsInTestCycle, removedTCsPayload());
        log.info(response.asString());
    }

    public static String setUpTestCaseId(String automationId) {
        String tcId = null;
        Map<String, String> queryTCExternalId = new HashMap<>();
        queryTCExternalId.put("externalId", automationId);

        Response response = PriorTestApiClient.checkTestCase(PTEndPoint.getTestCaseInProjectByAutomationId, queryTCExternalId);
        if (response.statusCode() == 404) {
            log.info("==== Start To Create TC For automationId " + automationId + " In Project");
            PTApiConfig.setIsTestCaseNewCreation(true);
            return createTestCase(automationId);
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            tcId = jsonPathEvaluator.get("data.id");
            log.debug("==== TC Id " + tcId + " In Project For automationId " + automationId);
            if (tcId != null) {
                PTApiConfig.setTestCaseId(tcId);
                return tcId;
            } else {
                log.info("==== Start To Create TC Id For automationId " + automationId + " In Project");
                PTApiConfig.setIsTestCaseNewCreation(true);
                return createTestCase(automationId);
            }
        }
    }

    private static String createTestCase(String automationId) {
        String tcId;
        Response response = PriorTestApiClient.createTestCase(PTEndPoint.createTCinProj, testCasePayload(automationId));
        if (response.statusCode() != 200) {
            JsonPath jsonPathEvaluator = response.jsonPath();
            log.error("Create Test Case Fail: " + jsonPathEvaluator.get("msg"));
            return null;
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            tcId = jsonPathEvaluator.get("data.id");
            log.info("==== Test Case ID " + tcId + " In Project For automationId " + automationId);
            PTApiConfig.setTestCaseId(tcId);
            return tcId;
        }

    }

    private static String testCasePayload(String automationId) {
        if (!PTApiConfig.getIsTestCasePayloadFromUser()) {
            JSONObject createTc = new JSONObject();
            createTc.put("externalLinkId", automationId); // mandatory
            createTc.put("title", PTApiFieldSetup.getTitle()); // mandatory
            createTc.put("feature", PTApiFieldSetup.getFeature()); // mandatory
            createTc.put("module", PTApiFieldSetup.getFeature()); // mandatory
            createTc.put("testStatus", "已评审"); // mandatory
            createTc.put("projectId", PTConstant.getPTProjectId()); // mandatory
            createTc.put("description", "this is from auto script");
            createTc.put("version", PTConstant.getPTVersion()); // mandatory
            createTc.put("caseCategory", PTApiFieldSetup.getCategory());
            createTc.put("priority", PTApiFieldSetup.getPriority());
            createTc.put("severity", PTApiFieldSetup.getSeverity());
            createTc.put("reportTo", "huju");
            createTc.put("testData", PTConstant.getPTVersion());
            createTc.put("testMethod", "自动");
            createTc.put("env", PTConstant.getPTEnv());
            createTc.put("testType", "正向");
            createTc.put("testDevice", PTConstant.getPTPlatform());
            createTc.put("platform", PTConstant.getPTPlatform());
            createTc.put("browser", "Chrome");
            createTc.put("testCondition", "");
            createTc.put("remarks", "");
            JSONObject customFieldDatas = new JSONObject();
            createTc.put("customFieldDatas", customFieldDatas);
            log.info("============== Test Case Payload Created by API Adapter :" + createTc);
            return createTc.toString();
        } else {
            String testPayload = PTApiPayloadConfig.getTestCasePayload();
            log.info("============== Test Case Payload Created By User :" + testPayload);
            return testPayload;
        }
    }

    public static void updateAndCloseIssue(String[] issueId, String runCaseId) {
        log.info("========= Issue From testCase  ======== " + issueId + "------==");
        if (issueId.length == 0) {
            log.info("========= Search Issue As runCase Id ========");
            Map<String, String> queryIssues = new HashMap<>();
            queryIssues.put("runCaseId", runCaseId.trim());
            Response response = PriorTestApiClient.checkIssueList(PTEndPoint.getIssueListByRunCaseId, queryIssues);
            if (response.statusCode() == 200) {
                log.info("========= No Closed Issue Found " + runCaseId + " " + response.asString());
                JsonPath jsonPathEvaluator = response.jsonPath();
                List<String> issueIdList = jsonPathEvaluator.getList("data.id", String.class);
                issueId = issueIdList.toArray(new String[0]);
            } else if (response.statusCode() == 404) {
                log.warn("=========  No Issues Found  " + runCaseId + " " + response.asString());

            } else {
                log.error("========= Check runCase id " + runCaseId + " " + response.asString());
            }
            if (issueId.length == 0) {
                log.info("=========  No Need To Close Issue For runCase Id " + runCaseId + " " + issueId.length);
            } else {
                log.info("========= Close Issue For runCase Id " + runCaseId + " " + issueId.length);
                intervalIssueAndClose(issueId, false);
            }
        } else {
            log.info("========= Check Issue Status And Close For runCase Id " + runCaseId + " " + issueId.length);
            intervalIssueAndClose(issueId, true);
        }
    }

    public static void intervalIssueAndClose(String[] issueIds, boolean checkIssueStatus) {
        for (String id : issueIds) {
            if (!checkIssueStatus) {
                closeIssue(id);
                continue;
            }
            String issueIdStatus = getIssueStatus(id);
            if (issueIdStatus != null && !(issueIdStatus.contains("关闭"))) {
                closeIssue(id);
            } else if (issueIdStatus == null) {
                log.info("======== No Need To Close Issue:  " + id + " Not found");
            } else {
                log.info("======== No Need To Repeat To Close :" + id + "Already With Status +" + issueIdStatus);
            }
        }
    }

    public static String getIssueStatus(String issueId) {
        Map<String, String> queryIssues = new HashMap<>();
        queryIssues.put("issueId", issueId.trim());
        Response response = PriorTestApiClient.checkIssueStatus(PTEndPoint.getIssueStatusByIssueId, queryIssues);

        String issueIdStatus = null;
        if (response.statusCode() != 200) {
            JsonPath jsonPathEvaluator = response.jsonPath();
            log.info("Issue Id " + issueId + jsonPathEvaluator.get("msg"));
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            issueIdStatus = jsonPathEvaluator.get("data.issueStatus");
            log.info("=== Get Issue Status:" + issueIdStatus + " For " + issueId);
        }
        return issueIdStatus;
    }

    public static void closeIssue(String issueId) {
        Response response = PriorTestApiClient.closeIssue(PTEndPoint.updateIssueStatusByIssueId, issueStatusClosePayload(issueId).toString());
        if (response != null) {
            log.info("Issue " + issueId + " Status Updated To Closed ");
        }
    }

    private static JSONObject issueStatusClosePayload(String issueId) {
        JSONObject payload = new JSONObject();
        payload.put("id", issueId);
        payload.put("issueStatus", "关闭");
        payload.put("fixVersion", PTConstant.getPTVersion());
        payload.put("verifiedResult", "this is from automation");
        return payload;
    }

    static String addTcIntoTestCyclePayload(String testCaseId) {
        JSONObject addTcIntoTestCycle = new JSONObject();
        addTcIntoTestCycle.put("projectId", PTProjectId);
        addTcIntoTestCycle.put("testCycleId", PTApiConfig.getTestCycleId());
        addTcIntoTestCycle.put("testCaseIds", PTProjectId);
        JSONArray testCaseIdsArray = new JSONArray();
        testCaseIdsArray.put(testCaseId);
        addTcIntoTestCycle.put("testCaseIds", testCaseIdsArray);

        return addTcIntoTestCycle.toString();
    }

    private String getTestPayload() {
        return this.casePayload;
    }

    public void setTestPayload(String casePayload) {
        this.casePayload = casePayload;
    }
}
