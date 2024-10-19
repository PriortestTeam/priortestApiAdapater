package com.priortest.api;

import com.priortest.annotation.TestCaseApi;
import com.priortest.annotation.TestStepApi;
import com.priortest.config.PTApiConfig;
import com.priortest.config.PTApiFieldSetup;
import com.priortest.config.PTConstant;
import com.priortest.config.PriorTestApiClient;
import com.priortest.model.Config;
import com.priortest.model.ConfigManager;
import com.priortest.run.api.PTApiUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.util.ArrayList;
import java.util.Map;


public class PTAPIAdapter extends TestListenerAdapter {
    private static final Logger log = LogManager.getLogger(PTAPIAdapter.class);
    static ConfigManager configManager = ConfigManager.getInstance();
    ArrayList<String> tcLists = new ArrayList<String>();
    private Config config;
    private PTApiUtil apiService;
    private long startTime;
    private PriorTestApiClient apiClient;



    private void initializeApiService(Config config) {
       // this.apiClient = new PriorTestApiClient(config.getBaseUrl(), config.getUserToken(), config.getUserEmail());
        //this.apiService = new PTApiUtil(apiClient);
    }

    @Override
    public void onStart(ITestContext testContext) {
        log.info("OnStart=======SetUP");
        Map<String, String> params = testContext.getCurrentXmlTest().getAllParameters();
        validateConfig(params);
        if (params == null || !validateConfig(params)) {
            throw new IllegalArgumentException("Required Configuration Missing： " + params + validateConfig(params));
        }
        // Set parameters in the ConfigManager
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.setParams(params);

        if (!configManager.getBooleanParam("enablePTApi")) {
            log.info("PT API is Not Enabled, Skip To Perform Test Execution Updating");
            return;
        }
        this.config = new Config(params.get("ptProjectId"), params.get("ptBaseEndPoint"), params.get("ptUserToken"), params.get("ptUserEmail"));
        if (config.getBaseUrl() == null || Config.getProjectId() == null || config.getUserEmail() == null || config.getUserToken() == null) {
            throw new IllegalArgumentException("Missing Required Global Configuration Values.");
        }
        initializeApiService(config);
        initializeTestCycle();
    }

    private boolean validateConfig(Map<String, String> config) {
        return config.containsKey("ptProjectId") && config.containsKey("ptBaseEndPoint") && config.containsKey("ptUserToken") && config.containsKey("ptUserEmail") && config.containsKey("Env") && config.containsKey("platform") && config.containsKey("version") && config.containsKey("enablePTApi") && config.containsKey("issueDealWith") && config.containsKey("signOff") && config.containsKey("release") && config.containsKey("currentRelease");
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
                log.info("==========onTestSuccess For Step Desc========== " + stepDesc);
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
            log.error("An Error Occurred In onTestSuccess: " + e.getMessage(), e);

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

    public String generateTestCycleTitle() {
        return configManager.getParam("env") + "_" + configManager.getParam("platform") + "_" + configManager.getParam("platform");
    }

    private void initializeTestCycle() {
        log.info("============== Test Suit onStart - Based URI：" + configManager.getParam("ptBaseEndPoint") + configManager.getBooleanParam("enablePTApi"));

        // Code to Setup testCycle
        if (configManager.getBooleanParam("enablePTApi")) {
            String testCycleTitle = configManager.getParam("testCycle");
            if (testCycleTitle == null || testCycleTitle.isEmpty()) {
                log.debug("============== " + testCycleTitle + "testCycle Title Is Not Defined, Set Default Test Cycle Title");
                testCycleTitle = generateTestCycleTitle();
            }
            log.debug("============== Start Setup testCycle：" + testCycleTitle);
            PTApiUtil.setUpTestCycle(testCycleTitle);
            log.debug("============== End Setup testCycle：" + testCycleTitle);
        }
    }

    @Override
    public void onFinish(ITestContext testContext) {
        if (!PTConstant.PT_TEST_CYCLE_CREATION) {
            log.info("============== Finished - Remove Extra TCs From testCycle: " + PTConstant.PT_TEST_CYCLE_CREATION);
            PTApiUtil.removeTCsFromTestCycle();
        } else {
            log.info("============== No Need To Perform Removal Of Extra TCs ");
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
            PTApiUtil.createIssue();

            // add test cases id for remove extra tc after execution
            PTApiUtil.addTestCaseId(testCaseId);


        } catch (Exception e) {
            log.error("An error occurred in onTestSuccess: " + e.getMessage(), e);

        }

    }

}