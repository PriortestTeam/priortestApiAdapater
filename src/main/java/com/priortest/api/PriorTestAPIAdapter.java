package com.priortest.api;

import com.priortest.annotation.TestCaseApi;
import com.priortest.annotation.TestStepApi;
import com.priortest.config.PTApiConfig;
import com.priortest.config.PTApiFieldSetup;
import com.priortest.config.PTConstant;
import com.priortest.run.api.PTApiUtil;
import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.util.ArrayList;


public class PriorTestAPIAdapter extends TestListenerAdapter {
    private static final Logger log = LogManager.getLogger(PriorTestAPIAdapter.class);
    ArrayList<String> tcLists = new ArrayList<String>();
    private long startTime;


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
                PTApiConfig.setFeature(feature);
                issueId = annotation.issueId();
                testName = annotation.testName();
                if (testName.isEmpty()){
                    testName =tr.getMethod().getMethodName();
                }
                PTApiConfig.setTestName(testName);
                log.info("==========onTestSuccess========== " + feature + "_" + automationId + "_" + issueId.length + "_"+ testName );
            }
            // setUpTestCaseId - retrieve testCase id as per given automationId
            // or create test case and return the created id
            log.info("==========Start Setup Test Case ID========== ");
            testCaseId = PTApiUtil.setUpTestCaseId(automationId);

            // put test case into a test cycle
            // return test run id
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            PTApiFieldSetup.setRunDuration(duration);
            PTApiUtil.setUpTestRunInTestCycle(testCaseId, "PASS");

            // Update or Close existing issues
            log.info("==========Start Close Issue ========== ");
            PTApiUtil.updateAndCloseIssue(issueId, PTApiConfig.getRunCaseId());

            // add test cases id for remove extra tc after execution
            log.info("==================== ");
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

    @Override
    public void onStart(ITestContext testContext) {
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
        }
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
                log.info("==========on Failure Test ========== " + feature + "_" + automationId + "_" + issueId + "_"+PTApiConfig.getTestName());
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