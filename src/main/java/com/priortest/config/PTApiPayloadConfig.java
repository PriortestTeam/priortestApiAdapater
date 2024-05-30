package com.priortest.config;

public class PTApiPayloadConfig {

    private String testCasePayload;

    public static String getTestCyclePayload(String testCyclePayload){
        return testCyclePayload;
    }


    public void setTestCasePayload(String testCasePayload){
        this.testCasePayload =testCasePayload;
    }

    public String getTestCasePayload(){
        return this.testCasePayload;
    }

    public static  String getIssuePayload(String issuePayload){
        return issuePayload;
    }
}
