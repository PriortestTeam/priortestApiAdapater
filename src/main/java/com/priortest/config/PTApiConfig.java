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

    public static String getTestCycleId() {
        return testCycleId;
    }

    public static void setTestCycleId(String id) {
        testCycleId = id;
    }

    public static void setTestCaseId(String tcId) {
        testCaseId = tcId;
    }

    public static String getTestCaseId() {
        return testCaseId;
    }

    public static void setFeature(String feature) {
       featureTitle =  feature;
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

    public static void setCategory(String category) {
        testCategory = category;
    }

    public static String getCategory() {
        return testCategory;
    }

    public static void setSeverity(String severity) {
        testSeverity = severity;
    }

    public static void setPriority(String priority) {
        testPriority = priority;
    }


    public static String getPriority() {
        return testPriority;
    }

    public static String getSeverity() {
        return testSeverity;
    }


    public boolean getConnectPTAPI() {
        return isConnectPriorTestAPI;
    }

    public static void setConnectPTAPI(boolean connection) {
        isConnectPriorTestAPI = connection;
    }

    public String getTestCycleTitle() {
        return testCycleTitle;

    }

    public static void setTestCycleTitle(String title) {
        testCycleTitle = title;
    }

    public static void setRunCaseId(String caseId) {
         testCaseRunId = caseId;
    }
    public static String getRunCaseId() {
        return testCaseRunId;

    }

}
