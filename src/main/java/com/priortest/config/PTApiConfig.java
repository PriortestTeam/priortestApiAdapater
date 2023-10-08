package com.priortest.config;


public class PTApiConfig {

    static boolean isConnectPriorTestAPI;
    static String testCycleTitle;
    static String testCycleId;

    public static String getTestCycleId() {
        return testCycleId;
    }

    public static void setTestCycleId(String id) {
        testCycleId = id;
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


}
