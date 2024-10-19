package com.priortest.config;

public class PTApiPayloadConfig {

    private static String casePayload;
    private static String issuePayload;
    private static String testCyclePayload;

    public static String getTestCyclePayload() {
        return testCyclePayload;
    }

    public static void setTestCyclePayload(String testCyclePayloadFromTestCase) {
        testCyclePayload = testCyclePayloadFromTestCase;
    }

    public static String getTestCasePayload() {
        return casePayload;
    }

    public static void setTestCasePayload(String testCasePayloadFromTestCase) {
        casePayload = testCasePayloadFromTestCase;
    }

    public static String getIssuePayload() {
        return issuePayload;
    }

    public static void setIssuePayload(String issuePayloadFromTestCase) {
        issuePayload = issuePayloadFromTestCase;
    }

}
