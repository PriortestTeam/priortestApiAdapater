package com.priortest.api;

import com.priortest.annotation.TestCaseApi;
import com.priortest.config.PTApiConfig;
import com.priortest.config.PTConstant;
import com.priortest.run.api.PTApiUtil;
import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.util.ArrayList;


public class ConnectAPIListener extends TestListenerAdapter {
    private static final Logger log = LogManager.getLogger(ConnectAPIListener.class);
    ArrayList<String> tcLists = new ArrayList<String>();

    @Override
    public void onTestSuccess(ITestResult tr) {
        TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
        String testCaseId = null;
        if (annotation != null) {
            testCaseId = annotation.testCaseId();
            String feature = annotation.feature();
            log.info("==========onTestSuccess========== " + feature + "_"+ testCaseId );
        }
        PTApiUtil.setUpTestRunInTestCycle(testCaseId, "Pass");

    }


    @Override
    public void onTestSkipped(ITestResult tr) {
        TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
        String testCaseId = null;
        if (annotation != null) {
            testCaseId = annotation.testCaseId();
            String feature = annotation.feature();
            log.info("==========onTestSkipped========== " + feature + "feature" + testCaseId + "testCaseId");
            tcLists.add(testCaseId);
        }
        PTApiUtil.setUpTestRunInTestCycle(testCaseId, "Skip");
    }

    @Override
    public void onStart(ITestContext testContext) {
        log.info("===============onStart");
        log.info("============== based uri：" + PTConstant.getPTBaseURI());
        // setup basedURI - PASSED BY MAIN Branch
        RestAssured.baseURI = PTConstant.getPTBaseURI();
        PTApiConfig priorTestApiConfig = new PTApiConfig();

        // below code to setup testCycle
        if (priorTestApiConfig.getConnectPTAPI()) {
            String testCycleTitle = priorTestApiConfig.getTestCycleTitle();
            if (testCycleTitle == null ||  testCycleTitle.isEmpty()) {
                log.warn(testCycleTitle + "is empty will you default test cycle title");
                testCycleTitle = "version_platform_env";
            }
            log.info("============== Start setup testCycle：" + testCycleTitle);
            PTApiUtil.setUpTestCycle(testCycleTitle);
            log.info("============== End setup testCycle：" + testCycleTitle);
        }
    }

    @Override
    public void onFinish(ITestContext testContext) {
        log.info("============== Finished- remove extra tc from testCycle");
        //PTApiUtil.removeTCsFromTestCycle(tcLists);
    }

    @Override
    public void onTestFailure(ITestResult tr) {
        TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
        String testCaseId = null;
        if (annotation != null) {
            testCaseId = annotation.testCaseId();
            String feature = annotation.feature();
            log.info("==========onTestFailure======== " + feature +"_"+ testCaseId);
            tcLists.add(testCaseId);
        }
        PTApiUtil.setUpTestRunInTestCycle(testCaseId, "Fail");
        PTApiUtil.createIssue();
    }

}