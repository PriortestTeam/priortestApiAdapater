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

    private static String planFixDate;
    private static String userImpact;
    private static String fixCategory;
    private static String rootcauseCategory;
    private static String rootCause;
    private static String frequency;
    private static String issueSource;
    private static String issueDuration;
    private static String issueTitle;
    private static String testBrowser;

    public static String getFailureMessage() {
        return failureMessage;
    }

    public static void setFailureMessage(String message) {
        failureMessage = message;
    }

    public static String getPlanFixDate() {
        return planFixDate;
    }

    public static void setPlanFixDate(String date) {
        planFixDate = date;
    }

    public static String getVerificationResult() {
        return verificationResult;
    }

    public static void setVerificationResult(String result) {
        verificationResult = result;
    }

    public static String getStatus() {
        return testStatus;
    }

    public static void setStatus(String status) {
        testStatus = status;
    }

    public static String getTitle() {
        return caseTitle;
    }

    public static void setTitle(String title) {
        caseTitle = title;
    }

    public static String getIssueTitle() {
        return issueTitle;
    }

    public static void setIssueTitle(String title) {
        issueTitle = title;
    }


    public static String getPriority() {
        return testPriority;
    }

    public static void setPriority(String priority) {
        testPriority = priority;
    }

    public static String getFeature() {
        return caseFeature;
    }

    public static void setFeature(String feature) {
        caseFeature = feature;
    }

    public static String getModule() {
        return caseModule;
    }

    public static void setModule(String module) {
        caseModule = module;
    }

    public static String getSeverity() {
        return testSeverity;
    }

    public static void setSeverity(String severity) {
        testSeverity = severity;
    }

    public static void setIssueSource(String source) {
        issueSource = source;
    }

    public static String getCategory() {
        return testCategory;
    }

    public static void setCategory(String category) {
        testCategory = category;
    }

    public static long getRunDuration() {
        return runDuration;
    }

    public static void setRunDuration(long duration) {
        runDuration = duration;

    }

    public static String getRunCaseId() {
        return runCaseId;
    }

    public static void setRunCaseId(String runTCIdSearched) {
        runCaseId = runTCIdSearched;
    }

    public static String getUserImpact() {
        return userImpact;
    }

    public static void setUserImpact(String impact) {
        userImpact = userImpact;
    }

    public static String getFixCategory() {
        return fixCategory;
    }

    public static void setFixCategory(String category) {
        fixCategory = category;
    }

    public static String getRootcauseCategory() {
        return rootcauseCategory;
    }

    public static void setRootcauseCategory(String causeCategory) {
        rootcauseCategory = causeCategory;
    }

    public static String getRootCause() {
        return rootCause;
    }

    public static void setRootCause(String cause) {
        rootCause = cause;
    }

    public static String getFrequency() {
        return frequency;
    }

    public static void setFrequency(String freq) {
        frequency = freq;
    }

    public static String getIssueSource() {
        return issueSource;
    }


    public static String getDuration() {
        return issueDuration;
    }

    public static void setDuration(String duration) {
        issueDuration = duration;
    }
}
