package com.priortest.config;

public class PTEndPoint {

    // Test Cycle
    // need to create a new API - api-apiAdpater-controller
    public static String createTestCycle = "testCycle/saveTestCycle";



    // need to create a new API - api-apiAdpater-controller


    // api-apiAdpater-controller

    public static String addTCsIntoTestCycle = "testCycle/instance/saveInstance";
    public static  String updateIssue ="Issue/udpateIssue";
    public static String createIssue = "Issue/createIssue";
    public static String closeIssue = "";

    public static String getTestCaseInProject = "retrieveTestcase";

    public static String updateIssueStatusByIssueId= "/issue/statusUpdate";
    public static String getIssueStatusByIssueId= "retrieveIssueStatusAsPerIssueId";
    public static String getIssueListByTestCase= "retrieveIssueAsPerTestCaseId"; //need to change api name


    public static String getTestCaseInProjectByAutomationId = "retrieveTestcaseByExternalId";
    public static String updateTestCaseStatusInTestCycle = "testCycle/runCaseStatusUpdate";

    public static String retrieveAllTCsInTestCycle = "testCycle/instance/removeTCsFromTestCycle";


    // move to adpater api - Start
    public static String retrieveTCInTestCycle = "testRun/retrieveTCInTestCycle/getCaseId";
   // response code need to config while token is invalid
    public static String retrieveTestCycleAsTitle = "testCycle/retrieveTestCycleAsTitle/getId";

    // move to adpater api - End

    public static String createTCinProj = "createTestCase";

    public static String removeTCFromTestCycle = "";
    public static String removeTCsFromTestCycle = "";

    // Sign off
    public static String signOff = "";

    // Issues




}
