package com.priortest.config;

public class PTApiFieldSetup {

    private static String testPriority;
    private static String testSeverity;
    private static String testStatus;
    private static String testCategory;
    private static String failureMessage;
    private static String verificationResult;
    private static Long runDuration;
    private static String caseTitle;
    private static String caseFeature;
    private static String caseModule;
    private static String runCaseId;

    public static void setSeverity(String severity) {
        testSeverity = severity;
    }

    public static void setPriority(String priority) {
        testPriority = priority;
    }

    public static void setStatus(String status) {
        testStatus = status;
    }

    public static void setTitle(String title) {
        caseTitle = title;
    }

    public static void setFeature(String feature) {
        caseFeature = feature;
    }

    public static void setModule(String module) {
        caseModule = module;
    }


    public static void setFailureMessage(String message) {
        failureMessage = message;
    }
    public static String getFailureMessage() {
        return failureMessage;
    }


    public static void setVerificationResult(String result) {
        verificationResult = result;
    }
    public static String getVerificationResult() {
        return verificationResult;
    }

    public static String getStatus() {
        return testStatus;
    }

    public static String getTitle() {
        return caseTitle;
    }

    public static String getPriority() {
        return testPriority;
    }

    public static String getFeature() {
        return caseFeature;
    }

    public static String getModule() {
        return caseModule;
    }

    public static String getSeverity() {
        return testSeverity;
    }
    public static void setCategory(String category) {
        testCategory = category;
    }

    public static String getCategory() {
        return testCategory;
    }


    public static void setRunDuration( long duration) {
        runDuration = duration;

    }
    public static long getRunDuration() {
        return runDuration;
    }

    public static void setRunCaseId(String runTCIdSearched) {
        runCaseId = runTCIdSearched;
    }

    public static String getRunCaseId() {
        return runCaseId;
    }
}
