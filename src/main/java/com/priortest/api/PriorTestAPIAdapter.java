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


public class PriorTestAPIAdapter extends TestListenerAdapter {
    private static final Set<String> failedStepsSet = ConcurrentHashMap.newKeySet(); // Thread-safe set to track failed steps
    private static final Logger log = LogManager.getLogger(PriorTestAPIAdapter.class);
    private static final ThreadLocal<Set<String>> calledStepMethods = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Map<String, Boolean>> stepStatusMap = ThreadLocal.withInitial(HashMap::new);
    ArrayList<String> tcLists = new ArrayList<String>();
    private long startTime;
    private String stepDesc;
    private boolean testStepPassed;
    private boolean stepStatus;

    @Override
    public void onTestStart(ITestResult tr) {
        startTime = System.currentTimeMillis();
        calledStepMethods.get().clear();
        stepResults.set(new ArrayList<>());
        PTApiConfig.setIsTestCaseNewCreation(false);
    }

    private void processAssertionResults(ITestResult result) {
        List<CustomSoftAssert.AssertionResult> results = CustomSoftAssert.getAssertionResults();

        if (results != null && !results.isEmpty()) {
            System.out.println("Assertion results for test: " + result.getMethod().getMethodName());
            for (CustomSoftAssert.AssertionResult assertionResult : results) {
                String status = assertionResult.getStatus() ? "PASS" : "FAIL";
                System.out.println("Step: " + assertionResult.getMessage() + " Status: " + status);
            }
        } else {
            System.out.println("No assertions were tracked for this test.");
        }

        // Clear results after processing to avoid cross-test contamination
        CustomSoftAssert.clearResults();
    }

    public void trackStep_b(String methodName, boolean status) {
        log.debug("Tracking step: " + methodName + " with status: " + status);
        // Only set to true if it has not previously been set to false
        stepStatusMap.get().merge(methodName, status, (oldStatus, newStatus) -> oldStatus && newStatus);
        calledStepMethods.get().add(methodName); // Add the method name to the called steps set
    }


    private static ThreadLocal<List<StepResult>> stepResults = ThreadLocal.withInitial(ArrayList::new);
    public void trackStep(Object testInstance, String methodName, boolean status) {
        List<StepResult> results = stepResults.get();
        try {
            // Retrieve the method and its annotation details
            Method method = testInstance.getClass().getDeclaredMethod(methodName);
            if (method.isAnnotationPresent(TestStepApi.class)) {
                TestStepApi annotation = method.getAnnotation(TestStepApi.class);

                // Extract step description and linked issue ID from annotation
                String stepDesc = annotation.stepDesc();
                String linkedIssueId = annotation.issueId();  // Adjust this if `issueId` is the right attribute name

                log.info("Tracking Step: { " + methodName + " } With Status: " + status + " , Description: " + stepDesc);

                // Add step result to the list for ordered processing
                results.add(new StepResult(stepDesc, status, linkedIssueId));
                log.info("---------------------"+  results.size());

            } else {
                log.warn("No TestStepApi Annotation Found On Method: " + methodName);
            }
        } catch (NoSuchMethodException e) {
            log.error("Method Not Found: " + methodName, e);
        }

    }
    public Map<String, Boolean> getStepStatusMap() {
        return stepStatusMap.get();
    }

    public void clearStepTracking() {
        stepStatusMap.get().clear();
        calledStepMethods.get().clear();
    }

    private void printExecutedSteps(ITestResult result) {
        // Iterate through the called step methods and print their descriptions
        for (String methodName : calledStepMethods.get()) {
            try {
                Method stepMethod = result.getInstance().getClass().getDeclaredMethod(methodName);
                if (stepMethod.isAnnotationPresent(TestStepApi.class)) {
                    TestStepApi annotation = stepMethod.getAnnotation(TestStepApi.class);
                    stepDesc = annotation.stepDesc();
                    log.info("-----------" + getStepStatusMap().get(methodName));
                    if (Boolean.TRUE.equals(getStepStatusMap().get(methodName))) {
                        stepStatus = true;
                        log.info("Steps: " + "\"" + stepDesc + "\"" + " Executed Successfully in testCase: " + result.getMethod().getMethodName() + ":");
                    } else {
                        log.warn("Step \"" + stepDesc + "\" Failed in testCase: " + result.getMethod().getMethodName() + ":");
                    }
                }
            } catch (NoSuchMethodException e) {
                log.error("Error Retrieving Method: " + methodName);
                e.printStackTrace();
            } catch (Exception e) {
                log.error("Error During Execution Of Step Method: " + methodName);
                e.printStackTrace();
            }
        }
        clearStepTracking();
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        try {
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
           // printExecutedSteps(tr);
            String testCaseId;
            String automationId = null;
            String[] issueId = null;
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

                issueId = annotation.issueId();
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
            testCaseId = PTApiUtil.setUpTestCaseId(externalTcId);

            // put testCase Into testCycle
            // Return runCase Id
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            PTApiFieldSetup.setRunDuration(duration);
            PTApiUtil.setUpTestRunInTestCycle(testCaseId, "PASS");

            Map<String, Object> issueList = PTApiUtil.isIssueOfRunCaseIdPresent();

            // To Close Issue
            if (issueList != null) {
                log.info("Existing Issue Present, Going to Close Issue ");
                issueId = getIssueIds(issueList);
                PTApiUtil.updateAndCloseIssue(issueId, PTApiFieldSetup.getRunCaseId());
            } else {
                PTApiUtil.updateAndCloseIssue(issueId, PTApiFieldSetup.getRunCaseId());
                log.info("No Issue To Be Closed For " + tr.getName());
            }
            // Add runCase ids For Removing None Executed testCase After Execution
            PTApiUtil.addTestCaseId(testCaseId);
            stepResults.remove();

            // Set issue to empty and remove issue ids
            String methodName = tr.getMethod().getMethodName();
            failedTestCases.put(methodName,new String[]{});
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
            log.info("============== Start Setup testCycle：" + testCycleTitle);
            PTApiUtil.setUpTestCycle(testCycleTitle);
            log.info("============== End Setup testCycle：" + testCycleTitle);
        }
    }

    private boolean isBlocked(ITestResult result) {
        return "BLOCKED".contains(result.getThrowable().getMessage());
    }

    @Override
    public void onFinish(ITestContext testContext) {
        log.info("============== Start - Remove Extra TCs From testCycle");
        PTApiUtil.removeTCsFromTestCycle();
        log.info("============== Finished- Remove Extra TCs From testCycle");
        try {
            // Process failed test cases
            for (String methodName : failedTestCases.keySet()) {
                // Get the class name of the failed test method
                String className = testContext.getAllTestMethods()[0].getTestClass().getName();
                // Convert class name to file path
                String javaFilePath = "src" + File.separator + "test" + File.separator + "java" + File.separator +
                        className.replace('.', File.separatorChar) + ".java";

                File javaFile = new File(javaFilePath);
                String absolutePath = javaFile.getAbsolutePath();

                if (javaFile.exists()) {
                    log.info("Start Update Issue in File : " + absolutePath);
                    // Update the Java file with the issues
                    String[] issues = failedTestCases.get(methodName);
                    UpdateIssueOnFinish.updateIssueIds(new File(javaFilePath), methodName, issues);
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


    @Override
    public void onTestFailure(ITestResult tr) {
        try {
            //stepResults.clear();
            //stepDesc = null; // Reset stepDesc to prevent carry-over
            //printExecutedSteps(tr); // Get TestStep Description of current testCase
            TestCaseApi annotation = tr.getMethod().getConstructorOrMethod().getMethod().getAnnotation(TestCaseApi.class);
            String testCaseId;
            String automationId = null;
            String[] issueId = null;
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
                PTApiFieldSetup.setTitle(testName);
                PTApiFieldSetup.setFeature(feature);

                priority = annotation.priority();
                severity = annotation.severity();
                caseCategory = annotation.caseCategory();

                PTApiFieldSetup.setCategory(caseCategory);
                PTApiFieldSetup.setSeverity(severity);
                PTApiFieldSetup.setPriority(priority);

                externalTcId = feature + "_" + automationId;
                issueId = annotation.issueId();
                log.info("==========onTestFailure========== " + externalTcId + " TestCaseName: " + testName);
            }
            processAssertionResults(tr);
            // setUpTestCaseId - Retrieve testCase id As Per Given automationId
            // Create testCase And Return The Created Id
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

            // Update or Close existing issues
            PTApiFieldSetup.setTitle(tr.getName());
            Map<String, Object> issueList = PTApiUtil.isIssueOfRunCaseIdPresent();
            List<StepResult> results = stepResults.get();
            boolean issueCreated = false;
            String newIssueId=null;
            if (results != null && !results.isEmpty()) {
                for (StepResult stepResult : results) {
                    // deal with step fails
                    if (!stepResult.getStatus()){
                        log.info("Step: { " + stepResult.getStepDesc() + " } Status: " + stepResult.getStatus());
                        newIssueId = handleIssueCreation(issueList, failedStepIdentifier + stepResult.getStepDesc(), tr);

                        issueCreated = true;
                    }else {
                       if (issueList!=null){
                           log.info("Step: { " + stepResult.getStepDesc() + "} Status: " + stepResult.getStatus());
                           handleIssueCloseForStepPass(issueList,stepResult.getStepDesc());
                       }
                    }
                }
            }
            // deal with test case level fails
            if (!issueCreated){
                log.warn("Create Issue for This Case " +failedStepIdentifier);
                newIssueId = handleIssueCreation(issueList, failedStepIdentifier, tr);
            }
            PTApiUtil.addTestCaseId(testCaseId);
            stepResults.remove();


            String[] newIssueList = new String[]{newIssueId};
            String[] combinedIssueList = new String[0];
            if (issueId.length!=0) {
                newIssueList = new String[issueId.length + newIssueList.length];
            }

            String methodName = tr.getMethod().getMethodName();
            failedTestCases.put(methodName, newIssueList);
            System.out.println(newIssueList.length + newIssueList.toString());

        } catch (Exception e) {
            log.error("An Error Occurred In onTestFailure: " + e.getMessage(), e);
        }
    }
    private final Map<String, String[]> failedTestCases = new HashMap<>();


    private void handleIssueCloseForStepPass(Map<String, Object> issueList, String failedStepIdentifier) {
        log.info("Check Existing Issue size:" + issueList.size());
        log.info("Check Existing Issue failedStepIdentifier:" + failedStepIdentifier);
        if (isFailedStepIdentifierPresent(issueList, failedStepIdentifier)) {
                String[] issueId = getIssueIdsWithFailedStepIdentifier(issueList, failedStepIdentifier);
                PTApiUtil.updateAndCloseIssue(issueId, PTApiFieldSetup.getRunCaseId());
            }
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
        return false;  // Ke
        // yword not found in any title
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