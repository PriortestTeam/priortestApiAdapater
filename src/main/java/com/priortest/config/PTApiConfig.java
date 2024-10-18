package com.priortest.config;


public class PTApiConfig {

    static boolean isConnectPriorTestAPI;
    static String testCycleTitle;
    static String testCycleId;
    static String testCaseRunId;
    static String testCaseId;
    static String featureTitle;
    static String testCaseTitle;
    static String testPriority;
    static String testSeverity;
    static String testCategory;
    private static String testStatus;
    private static int releaseStatus;
    private static int currentReleaseStatus;
    private static boolean newCreationOrNotStatus;
    private static boolean isPayloadFromTestCase;

    public static String getTestCycleId() {
        return testCycleId;
    }

    public static void setTestCycleId(String id) {
        testCycleId = id;
    }

    public static void setTestCaseId(String tcId) {
        testCaseId = tcId;
    }
    public static void setIsTestCaseNewCreation(boolean newCreationOrNot) {
        newCreationOrNotStatus = newCreationOrNot;
    }


    public static boolean getIsTestCaseNewCreation() {
        return newCreationOrNotStatus;
    }

    public static String getTestCaseId() {
        return testCaseId;
    }

    public static void setFeature(String feature) {
       featureTitle =  feature;
    }

    public static void setIsTestCasePayloadFromTestCase(boolean isFromTestCaseOrNot) {
        isPayloadFromTestCase =  isFromTestCaseOrNot;
    }



    public static boolean getIsTestCasePayloadFromTestCase() {
        return isPayloadFromTestCase;
    }

    public static String getFeature() {
        return featureTitle;
    }

    public static String getTestName() {
        return testCaseTitle;
    }

    public static void setTestName(String testName) {
        testCaseTitle = testName;
    }

    public static boolean getConnectPTAPI() {
        return isConnectPriorTestAPI;
    }

    public static String getTestCycleTitle() {
        return testCycleTitle;
    }

    public static void setTestCycleTitle(String title) {
        testCycleTitle = title;
    }


    public static void setConnectPTAPI(boolean connection) {
        isConnectPriorTestAPI = connection;
    }

    public static void setRunCaseId(String caseId) {
         testCaseRunId = caseId;
    }
    public static String getRunCaseId() {
        return testCaseRunId;
    }

    public static void setPriorTestRelease(int release) {
        releaseStatus = release;
    }
    public static void setPriorTestCurrentRelease(int currentRelease) {
        currentReleaseStatus = currentRelease;
    }
    public static int getPriorTestRelease(int release) {
        return releaseStatus;
    }

    public static int getPriorTestCurrentRelease(int currentRelease) {
        return currentReleaseStatus;
    }

}
