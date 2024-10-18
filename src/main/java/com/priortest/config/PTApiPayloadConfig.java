package com.priortest.config;

public class PTApiPayloadConfig {

    private static String casePayload;
    private String testCasePayload;

    public static String getTestCyclePayload(String testCyclePayload){
        return testCyclePayload;
    }


    public static void setTestCasePayload(String testCasePayload){
        casePayload =testCasePayload;
    }

    public static String getTestCasePayload(){
        return casePayload;
    }

    public static  String getIssuePayload(String issuePayload){
        return issuePayload;
    }
}
