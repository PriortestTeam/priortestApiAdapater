package com.priortest.run.api;

import com.priortest.config.PTApiConfig;
import com.priortest.config.PTApiFieldSetup;
import com.priortest.config.PTConstant;
import com.priortest.config.PTEndPoint;
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

    public static void setUpTestCycle(String testCycleTitle) {
        // if testCycleTitle is there - do not create
        // if testCycleTitle is not there - created
        //  - testCycleTitle passed by main branch
        boolean isTestCyclePresent = testCycleTitlePresent(testCycleTitle);
        if (isTestCyclePresent) {
            log.info("========= testCycle: " + testCycleTitle + " exist with status == ");
        } else {
            log.warn("========= Start creating testCycle: " + testCycleTitle);
            createTestCycle(testCycleTitle);
        }
    }

    private static boolean testCycleTitlePresent(String testCycleTitle) {
        Response response = PTApiRequest.doGet(PTEndPoint.retrieveTestCycleAsTitle, testCycleTitle);
        if (response == null) {
            log.error("========= testCycle: retrieve api exception");
            return false;
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCycleId = jsonPathEvaluator.get("data.id");
            if (testCycleId.isEmpty()) {
                log.info("=========testCycle: " + testCycleTitle + " does not exist");
                return false;
            } else {
                log.info("=========testCycle: " + testCycleTitle + " set test cycle id " + testCycleId);
                PTApiConfig.setTestCycleId(testCycleId);
                return true;
            }
        }
    }

    private static void createTestCycle(String testCycleTitle) {
        String testCyclePayload = testCyclePayload(testCycleTitle);
        Response response = PTApiRequest.doPostWithPayload(PTEndPoint.createTestCycle, testCyclePayload);
        if (response == null) {
            log.error("======== Failed: create testCycle " + testCycleTitle);
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String testCycleId = jsonPathEvaluator.get("data.id");
            PTApiConfig.setTestCycleId(testCycleId);
            PTConstant.PT_TEST_CYCLE_CREATION = true;  // set test cycle is fresh creation
            log.info("========== Passed: create testCycle " + testCycleId + " for title  " + testCycleTitle);
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
        removedTCsPayload.put("projectId", PTConstant.getPTProjectId());
        removedTCsPayload.put("testCycleId", PTApiConfig.getTestCycleId());
        removedTCsPayload.put("testCaseIds", getTestCaseIds());
        return removedTCsPayload.toString();
    }

    private static String testCyclePayload(String testCycleTitle) {
        // Create a JSONObject and add values to it
        JSONObject testCyclePayload = new JSONObject();
        testCyclePayload.put("title", testCycleTitle);
        testCyclePayload.put("description", "this is from testing ");
        testCyclePayload.put("version", PTConstant.getPTVersion());
        testCyclePayload.put("projectId", PTConstant.getPTProjectId());
        testCyclePayload.put("planExecuteDate", currentDataFormation());
        testCyclePayload.put("testPlatform", PTConstant.getPTPlatform());
        testCyclePayload.put("reportTo", "huju");
        testCyclePayload.put("testCycleStatus", "可执行");
        testCyclePayload.put("testMethod", "自动化");
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
        // verify testCaseId present in Test Cycle
        if (tcInTestCycle(testCaseId)) {
            // update run test case status
            log.info("Test case in test Cycle - status update - start");
            boolean updateTCStatus = updateTCStatus(status);
            if (updateTCStatus) {
                log.info("======== Passed: run case status  " + status + " update in testCycle - End =========");
            } else {
                log.error("======== Failed: run case status update in testCycle - End =========");
            }

        } else {
            log.warn("======== Test case not in test Cycle, start to add test case " + testCaseId + " into test cycle ======= " + PTApiConfig.getTestCycleId());
            addTcIntoTestCycle(testCaseId);
            log.info("======== Test case " + testCaseId + " in test Cycle - status update - Start");
            boolean updateTCStatus = updateTCStatus(status);
            if (updateTCStatus) {
                log.info("======== Passed: run case status  " + status + " update in testCycle - End =========");
            } else {
                log.error("======== Failed:  run case status update in testCycle - End =========");
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
        payload.put("projectId", PTConstant.getPTProjectId());
        payload.put("runCount", runCount);
        payload.put("runStatus", stepStatus);
        payload.put("testCaseId", PTApiConfig.getTestCaseId());
        payload.put("testCycleId", PTApiConfig.getTestCycleId());
        log.info("==== Update run case status ===== " + payload);
        return payload;
    }


    static String issueCreatePayload() {
        String browser = "";
        String caseCategory = "";
        String description = PTApiFieldSetup.getFailureMessage();
        String testDevice = PTConstant.getPTPlatform();
        String env = PTConstant.getPTEnv();
        String fixVersion = "";
        String issueStatus = "新建";
        String issueVersion = PTConstant.getPTVersion();
        String module = "";
        String planFixDate = "";
        String platform = PTConstant.getPTPlatform();
        String reportTo = "";
        String severity = "";
        String title = PTApiConfig.getTestName();
        String projectId = PTConstant.getPTProjectId();
        JSONObject issueCPayload = new JSONObject();

        issueCPayload.put("browser", browser);
        issueCPayload.put("caseCategory", caseCategory);
        issueCPayload.put("description", description);
        issueCPayload.put("projectId", projectId);
        issueCPayload.put("env", env);
        issueCPayload.put("fixVersion", fixVersion);
        issueCPayload.put("issueStatus", issueStatus);
        issueCPayload.put("issueVersion", issueVersion);
        issueCPayload.put("module", module);
        issueCPayload.put("planFixDate", planFixDate);
        issueCPayload.put("platform", platform);
        issueCPayload.put("reportTo", reportTo);
        issueCPayload.put("severity", severity);
        issueCPayload.put("verifiedResult", "");
        issueCPayload.put("title", title);
        issueCPayload.put("testDevice", testDevice);
        issueCPayload.put("runcaseId", PTApiConfig.getRunCaseId());

        JSONObject customFieldDatas = new JSONObject();
        issueCPayload.put("customFieldDatas", customFieldDatas);

        return issueCPayload.toString();
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
        Response response = PTApiRequest.doPostWithPayload(PTEndPoint.updateTestCaseStatusInTestCycle, payload.toString());
        log.info("============= Update TC status : " + response.asString());
        return response != null;
    }

    private static void addTcIntoTestCycle(String testCaseId) {

        Response response = PTApiRequest.doPostWithPayload(PTEndPoint.addTCsIntoTestCycle, addTcIntoTestCyclePayload(testCaseId));
        if (response == null) {
            log.error("=============== Failed added test case into to test cycle");
        } else {
            tcInTestCycle(testCaseId);
        }
    }

    private static boolean tcInTestCycle(String testCaseId) {
        Response response = PTApiRequest.doGetTestRunId(PTEndPoint.retrieveTCInTestCycle, testCaseId);
        if (response == null) {
            log.info("=========testCaseId Searched expected: " + response);
            return false;
        } else {
            JsonPath jsonPathEvaluator = response.jsonPath();
            String runTCIdSearched = jsonPathEvaluator.get("data.id");
            if (runTCIdSearched.isEmpty()) {
                log.info("======== Failed to retrieve test run id " + runTCIdSearched + " for test case id " + testCaseId);
                return false;
            } else {
                log.info("======== Retrieved test run id " + runTCIdSearched + " for test case id " + testCaseId);
                log.info("======== Saved test run id " + runTCIdSearched + " for test case id " + testCaseId);
                PTApiConfig.setRunCaseId(runTCIdSearched);
                return true;
            }

        }


    }

    public static void createIssue(String caseId) {
        log.info("=== start to create issue for test case Id");
        PTApiRequest.doPostWithPayload(PTEndPoint.createIssue, issueCreatePayload());
    }

    public static void removeTCsFromTestCycle() {
        log.info("=== start to remove extra test case from test cycle " + removedTCsPayload());
        Response response = PTApiRequest.doPostWithPayload(PTEndPoint.retrieveAllTCsInTestCycle, removedTCsPayload());

        log.info(response.asString());
    }

    public static String setUpTestCaseId(String automationId) {
        String tcId = null;
        Response response = PTApiRequest.doGetTestCaseIdByAutomationId(PTEndPoint.getTestCaseInProjectByAutomationId, automationId);
        if (response != null) {
            JsonPath jsonPathEvaluator = response.jsonPath();
            tcId = jsonPathEvaluator.get("data.id");
            log.info("==== test case id " + tcId + " in project for automationId " + automationId);
            PTApiConfig.setTestCaseId(tcId);
            return tcId;
        } else {
            log.info("==== Start to create test case id for automationId " + automationId + " in project");
            return createTestCase(automationId);
        }
    }

    private static String createTestCase(String automationId) {
        String tcId;
        Response response = PTApiRequest.doPostWithPayload(PTEndPoint.createTCinProj, testCasePayload(automationId));
        log.info("==== Response create test case for automationId " + automationId + "  " + response.asString());
        JsonPath jsonPathEvaluator = response.jsonPath();
        tcId = jsonPathEvaluator.get("data.id");
        log.info("==== test case id " + tcId + " in project for automationId " + automationId);
        PTApiConfig.setTestCaseId(tcId);
        return tcId;
    }

    private static String testCasePayload(String automationId) {
        JSONObject createTc = new JSONObject();

        createTc.put("externalLinkId", automationId);
        createTc.put("title", PTApiConfig.getTestName());
        createTc.put("feature", PTApiConfig.getFeature());
        createTc.put("testStatus", "已评审");
        createTc.put("projectId", PTProjectId);
        createTc.put("description", "this for auto script");
        createTc.put("version", PTConstant.getPTVersion());
        createTc.put("caseCategory", PTApiFieldSetup.getCategory());
        createTc.put("priority", PTApiFieldSetup.getPriority());
        createTc.put("reportTo", "huju");
        createTc.put("testData", PTConstant.getPTVersion());
        createTc.put("testMethod", "自动化");
        createTc.put("env", PTConstant.getPTEnv());
        createTc.put("testType", "正向");
        createTc.put("testDevice", PTConstant.getPTPlatform());
        createTc.put("platform", PTConstant.getPTPlatform());
        createTc.put("browser", "Chrome");
        createTc.put("testCondition", "");
        createTc.put("remarks", "");
        JSONObject customFieldDatas = new JSONObject();

        createTc.put("customFieldDatas", customFieldDatas);
        log.info("--------------here is to create tc payload :" + createTc);

        return createTc.toString();

    }

    public static void updateAndCloseIssue(String[] issueId, String runCaseId) {
        log.info("========= Issue from test case  ======== " + issueId + "------==");
        if (issueId.length == 0) {
            log.info("========= Search Issue as run case Id ========");
            Response response = PTApiRequest.doGetIssueIds(PTEndPoint.getIssueListByRunCaseId, runCaseId.trim());
            if (response.statusCode() == 200) {
                log.info("========= None closed issue found " + runCaseId + " " + response.asString());
                JsonPath jsonPathEvaluator = response.jsonPath();
                List<String> issueIdList = jsonPathEvaluator.getList("data.id", String.class);
                issueId = issueIdList.toArray(new String[0]);

            } else if (response.statusCode() == 404) {
                log.warn("========= Response no issues found  " + runCaseId + " " + response.asString());

            } else {
                log.error("========= Check runCase id " + runCaseId + " " + response.asString());
            }

            if (issueId.length == 0) {
                log.info("=========  No need to close Issue for rune case id " + runCaseId + " " + issueId.length);
            } else {
                log.info("========= Close Issue for run case id " + runCaseId + " " + issueId.length);
                intervalIssueAndClose(issueId, false);
            }
        } else {
            log.info("========= Check issue status and close for run case id " + runCaseId + " " + issueId.length);
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
            } else {
                log.info("======== No need to close Issue for " + id + " with status " + issueIdStatus);
            }
        }

    }

    public static String getIssueStatus(String issueId) {
        Response response = PTApiRequest.doGetIssueStatus(PTEndPoint.getIssueStatusByIssueId, issueId);
        String issueIdStatus = null;
        if (response != null) {

            JsonPath jsonPathEvaluator = response.jsonPath();
            issueIdStatus = jsonPathEvaluator.get("data.issueStatus");
            log.info("=== Get Issue Status:" + issueIdStatus + " for " + issueId);
        }
        return issueIdStatus;
    }

    public static void closeIssue(String issueId) {
        Response response = PTApiRequest.doPutWithPayload(PTEndPoint.updateIssueStatusByIssueId, issueStatusClosePayload(issueId).toString());
        if (response != null) {
            log.info("Issue Status update for" + issueId);
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
}
