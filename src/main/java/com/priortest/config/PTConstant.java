package com.priortest.config;

public class PTConstant {

    private static String PT_ENV;
    private static String PT_PLATFORM;
    private static String PT_BASED_URI;
    private static String PT_PROJECT_ID;
    private static String PT_TOKEN;
    private static String PT_EMAIL;
    private static boolean PT_SIGN_OFF;
    private static String PT_VERSION;
    private static boolean PT_ISSUE_DEAL_WITH;

    public static boolean PT_TEST_CYCLE_CREATION =false;
    public static void setPTBasedURI(String URI) {
        PT_BASED_URI = URI;
    }

    public static String getPTBaseURI() {
        return PT_BASED_URI + PT_PROJECT_ID;
    }


    public static String getPTToken() {
        return PT_TOKEN;
    }

    public static void setPTToken(String token) {
        PT_TOKEN = token;
    }

    public static String getPTEmail() {
        return PT_EMAIL;
    }

    public static void setPTEmail(String email) {
        PT_EMAIL = email;
    }

    public static void setPTSignOff(boolean signOff) {
        PT_SIGN_OFF = signOff;
    }

    public static boolean getPTSignOff(boolean signOff) {
        return PT_SIGN_OFF = signOff;
    }

    public static String getPTProjectId() {
        return PT_PROJECT_ID;
    }

    public static void setPTProjectId(String id) {
        PT_PROJECT_ID = id;
    }

    public static String getPTPlatform() {
        return PT_PLATFORM;
    }

    public static void setPTPlatform(String platform) {
        PT_PLATFORM = platform;
    }

    public static String getPTEnv() {
        return PT_ENV;
    }

    public static void setPTEnv(String env) {
        PT_ENV = env;
    }

    public static boolean getPTIssueCreation() {
        return PT_ISSUE_DEAL_WITH;
    }

    public static void setPTIssueCreation(boolean issueDealWith) {
        PT_ISSUE_DEAL_WITH = issueDealWith;
    }

    public static String getPTVersion() {
        return PT_VERSION;
    }

    public static void setPTVersion(String version) {
        PT_VERSION = version;
    }


}
