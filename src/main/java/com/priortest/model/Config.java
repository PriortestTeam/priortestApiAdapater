package com.priortest.model;

public class Config {

    private String baseUrl;
    private String userToken;
    private String userEmail;
    private String projectId;

    // Constructor
    public Config(String projectId, String baseUrl, String userToken, String userEmail) {
        this.projectId = projectId;
        this.baseUrl = baseUrl;
        this.userToken = userToken;
        this.userEmail = userEmail;
    }

    // Getters
    public String getProjectId() {
        return projectId;
    }

    // Setters
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

}
