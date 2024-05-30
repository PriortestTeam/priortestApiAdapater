package com.priortest.api;

import com.priortest.annotation.TestCaseApi;
import com.priortest.annotation.TestStepApi;
import com.priortest.config.PTApiConfig;
import com.priortest.config.PTApiFieldSetup;
import com.priortest.config.PTConstant;
import com.priortest.config.PriorTestApiClient;
import com.priortest.model.Config;
import com.priortest.run.api.PTApiUtil;
import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IExecutionListener;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.util.ArrayList;
import java.util.Map;


public class PTAPIAdapter extends TestListenerAdapter implements IExecutionListener {
    private static final Logger log = LogManager.getLogger(PTAPIAdapter.class);
    private final Config config;
    ArrayList<String> tcLists = new ArrayList<String>();
    private long startTime;
    private String projectId;
    private String baseUrl;
    private String userToken;
    private String userEmail;
    private String testEnv;
    private String testPlatform;

    private String testCycleId;
    private int release;
    private String version;
    private int currentRelease;
    private boolean enablePTApi;
    private boolean signOff;
    private PriorTestApiClient apiClient;

    public PTAPIAdapter(Config config) {
        this.config = config;

        this.apiClient = new PriorTestApiClient(config.getBaseUrl(), config.getUserToken(), config.getUserEmail());
    }


    @Override
    public void onStart(ITestContext testContext) {
        Map<String, String> params = testContext.getCurrentXmlTest().getAllParameters();
        if (params == null || !validateConfig(params)) {
            throw new IllegalArgumentException("Required configuration missing");
        }

        this.projectId = params.get("projectId");
        this.baseUrl = params.get("baseUrl");
        this.userToken = params.get("userToken");
        this.userEmail = params.get("userEmail");

        this.testEnv = params.get("Env");
        this.testPlatform = params.get("platform");
        this.version = params.get("version");

        // Parse boolean and integer parameters
        this.enablePTApi = Boolean.parseBoolean(params.get("enablePTApi"));
        this.signOff = Boolean.parseBoolean(params.get("signOff"));
        this.release = Integer.parseInt(params.get("release"));
        this.currentRelease = Integer.parseInt(params.get("currentRelease"));



        if (!enablePTApi){
            log.info("PT API is not enabled, Skip to perform test interation");
            return;

        }
        if (projectId == null || baseUrl == null || userToken == null) {
            throw new IllegalArgumentException("Missing required global configuration values.");
        }


        // Initialize test cycle
        this.testCycleId = initializeTestCycle(params);

    }

    private boolean validateConfig(Map<String, String> config) {
        return config.containsKey("projectId") && config.containsKey("userToken")
                && config.containsKey("userEmail") && config.containsKey("testEnv")
                && config.containsKey("testPlatform") && config.containsKey("testReleaseInfo");
    }


    @Override
    public void onExecutionStart() {
        // Initialization can be done here if needed

    }

    @Override
    public void onTestStart(ITestResult tr) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        try {
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
            TestStepApi annotationStep = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestStepApi.class);
            if (annotationStep != null) {
                String stepDesc = annotationStep.stepDesc();
                log.info("==========onTestSuccess for step desc========== " + stepDesc);
            }
            String testCaseId;
            String automationId = null;
            String[] issueId = null;
            String feature;
            String testName;
            if (annotation != null) {
                automationId = annotation.automationId();
                feature = annotation.feature();
                testName = annotation.testName();
                PTApiConfig.setTestName(testName);
                PTApiConfig.setFeature(feature);
                issueId = annotation.issueId();
                log.info("==========onTestSuccess========== " + feature + "_" + automationId + "_" + issueId);
            }
            // setUpTestCaseId - retrieve testCase id as per given automationId
            // or create test case and return the created id
            testCaseId = PTApiUtil.setUpTestCaseId(automationId);

            // put test case into a test cycle
            // return test run id
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            PTApiFieldSetup.setRunDuration(duration);
            PTApiUtil.setUpTestRunInTestCycle(testCaseId, "PASS");

            // Update or Close existing issues
            PTApiUtil.updateAndCloseIssue(issueId, PTApiConfig.getRunCaseId());

            // add test cases id for remove extra tc after execution
            PTApiUtil.addTestCaseId(testCaseId);


        } catch (Exception e) {
            log.error("An error occurred in onTestSuccess: " + e.getMessage(), e);

        }
    }


    @Override
    public void onTestSkipped(ITestResult tr) {
        try {
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
            String testCaseId = null;
            if (annotation != null) {
                testCaseId = annotation.automationId();
                String feature = annotation.feature();

                log.info("==========onTestSkipped========== " + feature + "feature" + testCaseId + "testCaseId");
            }
            PTApiUtil.setUpTestRunInTestCycle(testCaseId, "SKIP");

            // add test cases id for remove extra tc after execution
            PTApiUtil.addTestCaseId(testCaseId);

        } catch (Exception e) {
            log.error("An error occurred in onTestSuccess: " + e.getMessage(), e);

        }
    }


    private String initializeTestCycle(Map<String, String> params) {
        log.info("============== Test Suit onStart - based uri：" + PTConstant.getPTBaseURI());
        // setup basedURI - PASSED BY MAIN Branch
        RestAssured.baseURI = PTConstant.getPTBaseURI();
        // below code to setup testCycle
        if (PTApiConfig.getConnectPTAPI()) {
            String testCycleTitle = PTApiConfig.getTestCycleTitle();
            if (testCycleTitle == null || testCycleTitle.isEmpty()) {
                log.debug("============== " + testCycleTitle + "testCycle Title is not defined, set default test cycle title");
                testCycleTitle = PTConstant.getPTEnv() + "_" + PTConstant.getPTPlatform() + "_" + PTConstant.getPTVersion();
            }
            log.info("============== Start setup testCycle：" + testCycleTitle);
            PTApiUtil.setUpTestCycle(testCycleTitle);
            log.info("============== End setup testCycle：" + testCycleTitle);
        return null;
    }
        return null;
}



    @Override
    public void onFinish(ITestContext testContext) {
        if (!PTConstant.PT_TEST_CYCLE_CREATION) {
            log.info("============== Finished- remove extra tc from testCycle" + PTConstant.PT_TEST_CYCLE_CREATION);
           // PTApiUtil.removeTCsFromTestCycle();
        } else {
            log.info("============== No need to perform removal of extra TCs ");
        }
    }


    @Override
    public void onTestFailure(ITestResult tr) {
        try {
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
            String testCaseId;
            String automationId = null;
            String[] issueId = null;
            String feature;
            String testName;
            String priority;
            String severity;
            String caseCategory;
            if (annotation != null) {
                automationId = annotation.automationId();
                feature = annotation.feature();
                testName = annotation.testName();
                PTApiConfig.setTestName(testName);
                PTApiConfig.setFeature(feature);

                priority = annotation.priority();
                severity = annotation.severity();
                caseCategory = annotation.caseCategory();

                PTApiFieldSetup.setCategory(caseCategory);
                PTApiFieldSetup.setSeverity(severity);
                PTApiFieldSetup.setPriority(priority);

                issueId = annotation.issueId();
                log.info("==========on Failure Test ========== " + feature + "_" + automationId + "_" + issueId);
            }
            // setUpTestCaseId - retrieve testCase id as per given automationId
            // or create test case and return the created id
            testCaseId = PTApiUtil.setUpTestCaseId(automationId);

            // put test case into a test cycle
            // return test run id
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            PTApiFieldSetup.setRunDuration(duration);
            PTApiUtil.setUpTestRunInTestCycle(testCaseId, "FAIL");

            // Update or Close existing issues
            PTApiFieldSetup.setFailureMessage(tr.getThrowable().getMessage());
            PTApiConfig.setTestName(tr.getName());
            PTApiUtil.createIssue(automationId);

            // add test cases id for remove extra tc after execution
            PTApiUtil.addTestCaseId(testCaseId);


        } catch (Exception e) {
            log.error("An error occurred in onTestSuccess: " + e.getMessage(), e);

        }

    }

}