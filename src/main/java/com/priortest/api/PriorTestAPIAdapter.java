package com.priortest.api;

import com.priortest.annotation.TestCaseApi;
import com.priortest.annotation.TestStepApi;
import com.priortest.config.CustomSoftAssert;
import com.priortest.config.PTApiConfig;
import com.priortest.config.PTApiFieldSetup;
import com.priortest.config.PTConstant;
import com.priortest.run.api.PTApiUtil;
import com.priortest.run.api.UpdateIssueOnFinish;
import com.priortest.step.StepResult;
import com.priortest.step.StepResultTracker;
import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;


public class PriorTestAPIAdapter extends TestListenerAdapter {
    private static final Set<String> failedStepsSet = ConcurrentHashMap.newKeySet(); // Thread-safe set to track failed steps
    private static final Logger log = LogManager.getLogger(PriorTestAPIAdapter.class);
    private static final ThreadLocal<Set<String>> calledStepMethods = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Map<String, Boolean>> stepStatusMap = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<List<StepResult>> stepResults = ThreadLocal.withInitial(ArrayList::new);
    private final Map<String, String[]> failedTestCases = new HashMap<>();
    ArrayList<String> tcLists = new ArrayList<String>();
    private long startTime;
    private String stepDesc;
    private boolean testStepPassed;
    private boolean stepStatus;

    private static String handleIssueCreation(Map<String, Object> issueList, String failedStepIdentifier, ITestResult tr) {
        String newIssueId = null;
        if (issueList != null) {
            log.info("Verifying failedStepIdentifier: " + failedStepIdentifier + " In " + issueList);
            if (isFailedStepIdentifierPresent(issueList, failedStepIdentifier)) {
                log.warn("Existing Matched Issue Present, No New Issue Required to Be Created");
            } else {
                /// if (failedStepsSet.contains(failedStepIdentifier)) {
                //    log.warn("Issue Already Exists For the Failed Step: " + failedStepIdentifier);
                //} else {
                // Add the step to the set and create a new issue
                failedStepsSet.add(failedStepIdentifier);
                log.info("Creating New Issue For Failed Step: " + failedStepIdentifier);
                PTApiFieldSetup.setIssueTitle(failedStepIdentifier);
                PTApiFieldSetup.setFailureMessage(failedStepIdentifier);
                PTApiConfig.setCreateIssueForStep(true);
                newIssueId = PTApiUtil.createIssue();
                PTApiConfig.setCreateIssueForStep(false);
            }
            //}
        } else {
            // Add the step to the set and create a new issue
            failedStepsSet.add(failedStepIdentifier);
            log.info("Creating New Issue For Failed Step: " + failedStepIdentifier);
            PTApiFieldSetup.setIssueTitle(failedStepIdentifier);
            PTApiFieldSetup.setFailureMessage(failedStepIdentifier + ": " + tr.getThrowable().getMessage());
            PTApiConfig.setCreateIssueForStep(true);
            newIssueId = PTApiUtil.createIssue();
            PTApiConfig.setCreateIssueForStep(false);

        }
        return newIssueId;
    }

    public static boolean isFailedStepIdentifierPresent(Map<String, Object> issueList, String failedStepIdentifier) {
        // Get the "id" array from issueList as a List of Maps
        List<Map<String, String>> idList = (List<Map<String, String>>) issueList.get("id");

        // Check if idList is not null or empty
        if (idList == null || idList.isEmpty()) {
            log.debug("No items In ID List");
            return false;
        }

        // Loop through each item in the idList and check the "title"
        for (Map<String, String> item : idList) {
            String title = item.get("title");
            if (title != null && title.contains(failedStepIdentifier)) {
                log.info("Found Issue Title Contain： " + failedStepIdentifier);
                return true;  // Keyword found
            }
        }
        return false;
    }

    @Override
    public void onTestStart(ITestResult tr) {
        startTime = System.currentTimeMillis();
        StepResultTracker.clearStepResults(); // Prevent stale data from previous tests
        PTApiConfig.setIsTestCaseNewCreation(false);
    }

    private void processAssertionResults(ITestResult result) {
        List<CustomSoftAssert.AssertionResult> results = CustomSoftAssert.getAssertionResults();

        if (results != null && !results.isEmpty()) {
            log.info("Assertion results for test: " + result.getMethod().getMethodName());
            for (CustomSoftAssert.AssertionResult assertionResult : results) {
                String status = assertionResult.getStatus() ? "PASS" : "FAIL";
                log.info("Step: " + assertionResult.getMessage() + " Status: " + status);
            }
        } else {
           log.info("No assertions were tracked for this test.");
        }

        // Clear results after processing to avoid cross-test contamination
        CustomSoftAssert.clearResults();
    }


    @Override
    public void onTestSuccess(ITestResult tr) {
        try {
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
            String testCaseId;
            String automationId = null;
            String[] issueIdInTestCase = null;
            String feature;
            String testName;
            String externalTcId = null;
            String issueIdentifier = PTApiConfig.getIssueIdentifier();

            if (annotation != null) {
                automationId = annotation.automationId();
                String caseCategory = annotation.caseCategory();
                String severity = annotation.severity();
                String priority = annotation.priority();
                feature = annotation.feature();

                PTApiFieldSetup.setFeature(feature);
                PTApiFieldSetup.setCategory(caseCategory);
                PTApiFieldSetup.setPriority(priority);
                PTApiFieldSetup.setSeverity(severity);

                issueIdInTestCase = annotation.issueId();
                testName = annotation.testName();
                if (testName.isEmpty()) {
                    testName = tr.getMethod().getMethodName();
                }
                PTApiFieldSetup.setTitle(testName);
                externalTcId = feature + "_" + automationId;
                log.info("==========onTestSuccess========== " + externalTcId + " TestCaseName: " + testName);
            }

            // setUpTestCaseId - retrieve testCase id as per given automationId
            // or create test case and return the created id
            log.info("==========Start Setup Test Case ID========== " + externalTcId);
            externalTcId = externalTcId.length() > 50 ? externalTcId.substring(0, 50) : externalTcId;
            testCaseId = PTApiUtil.setUpTestCaseId(externalTcId);

            // put testCase Into testCycle
            // Return runCase Id
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            PTApiFieldSetup.setRunDuration(duration);
            PTApiUtil.setUpTestRunInTestCycle(testCaseId, "PASS");
            // To Close Issue From Run Case
            if (issueIdInTestCase!=null){
                log.info("Issue To Be Closed For " + tr.getName());
                PTApiUtil.updateAndCloseIssue(issueIdInTestCase, PTApiFieldSetup.getRunCaseId());

                // Set issue to empty in Run Case
                String methodName = tr.getMethod().getMethodName();
                log.info("Added MethodName To failedTestCases: ");
                failedTestCases.put(methodName, new String[]{});
            }
            // To Retrieve Issue From System and Close It
            // In case issue not lined In run Case
            Map<String, Object> issueListInSystem = PTApiUtil.isIssueOfRunCaseIdPresent();
            if (issueListInSystem != null) {
                log.info("Existing Issue Present, Going to Close Issue: ");
                String[] issueIdInSystem = getIssueIds(issueListInSystem);
                PTApiUtil.updateAndCloseIssue(issueIdInSystem, PTApiFieldSetup.getRunCaseId());
            }
            // Add runCase ids For Removing None Executed testCase After Execution
            log.info("Added Test Id For Removing List: ");
            PTApiUtil.addTestCaseId(testCaseId);
            stepResults.remove();

        } catch (Exception e) {
            log.error("An error occurred in onTestSuccess: " + e.getMessage(), e);
        }
    }

    @Override
    public void onTestSkipped(ITestResult tr) {
        //printExecutedSteps(tr);
        try {
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
            String testCaseId = null;
            String externalTcId = null;
            if (annotation != null) {
                testCaseId = annotation.automationId();
                String feature = annotation.feature();
                externalTcId = feature + "_" + testCaseId;
                log.info("==========onTestSkipped========== " + externalTcId);
            }

            // search test case in test Cycle
            testCaseId = PTApiUtil.getTestCaseIdInProject(externalTcId);
            if (testCaseId == null) {
                log.info("No Action From PT API Adapter Since No testCase Found In Project");
            } else {
                log.info("Check The Case In testCycle And Update Run Status : SKIP ");
                PTApiUtil.setUpTestRunInTestCycle(testCaseId);
            }

            stepResults.remove();
        } catch (Exception e) {
            log.error("An Error In onTestSuccess: " + e.getMessage(), e);

        }
    }

    @Override
    public void onStart(ITestContext testContext) {
        log.info("============== Test Suit onStart - Base URI：" + PTConstant.getPTBaseURI());
        // setup basedURI - PASSED BY MAIN Branch
        RestAssured.baseURI = PTConstant.getPTBaseURI();

        // below code to setup testCycle
        if (PTApiConfig.getConnectPTAPI()) {
            String testCycleTitle = PTApiConfig.getTestCycleTitle();
            if (testCycleTitle == null || testCycleTitle.isEmpty()) {
                log.debug("============== " + testCycleTitle + "testCycle Title Is Not Defined, Set Default testCycle Title");
                testCycleTitle = PTConstant.getPTEnv() + "_" + PTConstant.getPTPlatform() + "_" + PTConstant.getPTVersion();
            }
            PTApiUtil.setUpTestCycle(testCycleTitle);
            log.info("============== End Setup testCycle：" + testCycleTitle);

        }
        log.info("============== Test Suit End ===========");
    }

    private boolean isBlocked(ITestResult result) {
        return "BLOCKED".contains(result.getThrowable().getMessage());
    }

    @Override
    public void onFinish(ITestContext testContext) {
        PTApiUtil.removeTCsFromTestCycle();
        log.info("============== Finished- Remove Extra TCs From testCycle");
        try {
            // Process failed test cases
            for (String methodName : failedTestCases.keySet()) {
                // Get the class name of the failed test method
                String className = testContext.getAllTestMethods()[0].getTestClass().getName();
                // Convert class name to file path
                String javaFilePath = "src" + File.separator + "test" + File.separator + "java" + File.separator + className.replace('.', File.separatorChar) + ".java";

                File javaFile = new File(javaFilePath);
                String absolutePath = javaFile.getAbsolutePath();

                if (javaFile.exists()) {
                    log.info("Start Update Issue in File : " + absolutePath);
                    // Update the Java file with the issues
                    String[] issues = failedTestCases.get(methodName);
                    if (issues.length == 0) {
                        log.info("Updated Issue To Blank In Test Case");
                        UpdateIssueOnFinish.updateIssueIds(new File(javaFilePath), methodName, issues);
                    } else {
                        UpdateIssueOnFinish.updateIssueIds(new File(javaFilePath), methodName, issues);
                        log.info("Updated Issues In Test Case");
                    }

                } else {
                    log.error("File not found: " + absolutePath);
                }
            }
        } catch (Exception e) {
            log.error("Error Processing Failed Test Cases: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private String logFailureDetails(ITestResult result) {
        Throwable throwable = result.getThrowable();
        String failureIdentifier = null;
        // Get the stack trace elements
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();

        // Extract the line number and method name from your test class
        for (StackTraceElement element : stackTraceElements) {
            // Check if the class name matches your test class
            if (element.getClassName().equals(result.getTestClass().getName())) {
                String failureLocation = element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + ")";
                //+ element.getLineNumber()
                log.warn("Failure Occurred In: " + failureLocation);
                // Create a unique failure identifier based on method and line number
                failureIdentifier = createFailureIdentifier(result, failureLocation);
                break; // Exit loop after finding the first matching class
            }
        }
        return failureIdentifier;
    }

    private String createFailureIdentifier(ITestResult result, String failureLocation) {
        return result.getName() + "_" + result.getThrowable().getMessage() + "_" + failureLocation.hashCode(); // Example identifier
    }

    private String generateFailedStepIdentifier(ITestResult tr, String failMessage, String issueIdentifier) {
        String failedStepIdentifier;
        if (stepDesc == null) {
            failedStepIdentifier = tr.getName() + "_" + failMessage + "_" + issueIdentifier;
        } else {
            log.debug("Current Execution Step: " + stepDesc);
            PTApiConfig.setCreateIssueForStep(true);
            failedStepIdentifier = stepDesc + " Of " + tr.getName() + "_" + failMessage + "_" + issueIdentifier;
        }
        return failedStepIdentifier;
    }

    @Override
    public void onTestFailure(ITestResult tr) {
        try {
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
            String testCaseId;
            String automationId = null;
            String[] issueIdsInTestCase = null;
            String feature;
            String testName;
            String priority;
            String severity;
            String caseCategory;
            String externalTcId = null;
            String issueIdentifier = PTApiConfig.getIssueIdentifier(); // Method to retrieve device or OS information

            if (annotation != null) {
                automationId = annotation.automationId();
                feature = annotation.feature();
                testName = annotation.testName();
                if (testName.isEmpty()) {
                    testName = tr.getMethod().getMethodName(); // When testCaseName Not set in TestCaseApi
                }

                PTApiFieldSetup.setFeature(feature);
                priority = annotation.priority();
                severity = annotation.severity();
                caseCategory = annotation.caseCategory();

                PTApiFieldSetup.setCategory(caseCategory);
                PTApiFieldSetup.setSeverity(severity);
                PTApiFieldSetup.setPriority(priority);
                PTApiFieldSetup.setTitle(testName);

                externalTcId = feature + "_" + automationId;
                issueIdsInTestCase = annotation.issueId();
                log.info("==========onTestFailure========== externalTcId: " + externalTcId + " TestCaseName: " + testName + " Issue：" + String.join(", ", issueIdsInTestCase));
            }
            processAssertionResults(tr);
            // setUpTestCaseId - Retrieve testCase id As Per Given automationId
            // Create testCase And Return The Created Id
            externalTcId = externalTcId.length() > 50 ? externalTcId.substring(0, 50) : externalTcId;
            testCaseId = PTApiUtil.setUpTestCaseId(externalTcId);

            // Put testCase Into a testCycle
            // Return runCase Id
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            PTApiFieldSetup.setRunDuration(duration);
            String caseUpdatedStatus = null;
            String failMessage = tr.getThrowable().getMessage();
            if (isBlocked(tr)) {
                caseUpdatedStatus = "BLOCK";

            } else {
                caseUpdatedStatus = "FAIL";
            }
            PTApiUtil.setUpTestRunInTestCycle(testCaseId, caseUpdatedStatus);

            // Include Unique Identifier for the Filed Step
            // General device/OS information
            String failedStepIdentifier;
            failedStepIdentifier = generateFailedStepIdentifier(tr, failMessage, issueIdentifier);

            // Update or Close Existing Issues
            PTApiFieldSetup.setTitle(tr.getName());
            // get Issue List From DB as per the run CaseId
            // In case missing issue in Test Case
            Map<String, Object> issueListFromSystem = PTApiUtil.isIssueOfRunCaseIdPresent();

            List<StepResult> results = StepResultTracker.getStepResults();
            log.info("adsfasdfadsf========= " + results.size());
            boolean issueCreated = false;
            String newCreatedIssueId = null;
            if (results != null && !results.isEmpty()) {
                for (StepResult stepResult : results) {
                    log.info("stepResult=======" + stepResult.getStatus());
                    // Deal with step Fails
                    if (!stepResult.getStatus()) {
                        log.info("Step: { " + stepResult.getStepDesc() + " } Status: " + stepResult.getStatus());
                        newCreatedIssueId = handleIssueCreation(issueListFromSystem, failedStepIdentifier + stepResult.getStepDesc(), tr);
                        issueCreated = true;
                    } else {
                        if (issueListFromSystem != null) {
                            log.info("Step: { " + stepResult.getStepDesc() + "} Status: " + stepResult.getStatus());
                            handleIssueCloseForStepPass(issueListFromSystem, stepResult.getStepDesc());
                        }
                    }
                }
            }
            // deal with test case level fails
            if (!issueCreated) {
                log.warn("Create Issue for This Case " + failedStepIdentifier);
                newCreatedIssueId = handleIssueCreation(issueListFromSystem, failedStepIdentifier, tr);
            }

            log.info("Added TestCase Id From Removing:");
            PTApiUtil.addTestCaseId(testCaseId);

            stepResults.remove();
            if (newCreatedIssueId != null) {
                log.info("Added MethodName And IssueList For Issue Updating In TestCase");
                String[] newIssueList = new String[]{newCreatedIssueId}; // When created new issue

                // later to add deal with append from system
                if (issueListFromSystem!=null) {
                    String[] issueFromSystem =null;
                    // Deal with append from system
                        List<String> extractedIds = new ArrayList<>();
                        if (issueListFromSystem.containsKey("id")) {
                            Object idList = issueListFromSystem.get("id");
                            if (idList instanceof List) {
                                for (Object obj : (List<?>) idList) {
                                    if (obj instanceof Map) {
                                        Map<?, ?> issue = (Map<?, ?>) obj;
                                        if (issue.containsKey("id")) {
                                            extractedIds.add(issue.get("id").toString());
                                        }
                                    }
                                }
                            }
                        }
                        // Convert List to String[]
                        issueFromSystem = extractedIds.toArray(new String[0]);
                        newIssueList = Stream
                                .concat(Arrays.stream(newIssueList), Arrays.stream(issueFromSystem))
                                .toArray(String[]::new);
                }
                String methodName = tr.getMethod().getMethodName();
                failedTestCases.put(methodName, newIssueList);
            }
        } catch (Exception e) {
            log.error("An Error Occurred In onTestFailure: " + e.getMessage(), e);
        }
    }

    private void handleIssueCloseForStepPass(Map<String, Object> issueList, String failedStepIdentifier) {
        log.info("Check Existing Issue size:" + issueList.size());
        log.info("Check Existing Issue failedStepIdentifier:" + failedStepIdentifier);
        if (isFailedStepIdentifierPresent(issueList, failedStepIdentifier)) {
            String[] issueId = getIssueIdsWithFailedStepIdentifier(issueList, failedStepIdentifier);
            PTApiUtil.updateAndCloseIssue(issueId, PTApiFieldSetup.getRunCaseId());
        }
    }

    public String[] getIssueIds(Map<String, Object> issueList) {
        // Get the "id" array from issueList as a List of Maps
        List<Map<String, String>> idList = (List<Map<String, String>>) issueList.get("id");

        // Check if idList is not null or empty
        if (idList == null || idList.isEmpty()) {
            log.debug("No items In ID List");
            return new String[0];  // Return an empty array if no items
        }

        List<String> matchingIds = new ArrayList<>();
        for (Map<String, String> item : idList) {
            String id = item.get("id");  // Each item has an "id" key
            if (id != null) {
                matchingIds.add(id);  // Add the ID to the list if it matches
            }
        }
        return matchingIds.toArray(new String[0]);  // Convert List to String array and return
    }

    public String[] getIssueIdsWithFailedStepIdentifier(Map<String, Object> issueList, String failedStepIdentifier) {
        // Get the "id" array from issueList as a List of Maps
        List<Map<String, String>> idList = (List<Map<String, String>>) issueList.get("id");

        // Check if idList is not null or empty
        if (idList == null || idList.isEmpty()) {
            log.debug("No items In ID List");
            return new String[0];  // Return an empty array if no items
        }
        // Create a list to store IDs that match the failedStepIdentifier
        List<String> matchingIds = new ArrayList<>();

        // Loop through each item in the idList and check the "title"
        for (Map<String, String> item : idList) {
            String title = item.get("title");
            String id = item.get("id");  // Each item has an "id" key
            //&& title.contains(failedStepIdentifier);
            if (title != null && title.contains(failedStepIdentifier)) {
                log.info("Found Issue Title Contain: " + failedStepIdentifier);
                if (id != null) {
                    matchingIds.add(id);  // Add the ID to the list if it matches
                }
            }
        }

        return matchingIds.toArray(new String[0]);  // Convert List to String array and return
    }

}