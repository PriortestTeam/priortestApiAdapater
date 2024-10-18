package com.priortest.model;

import java.util.Map;

public class ConfigManager {

    private static ConfigManager instance;
    private Map<String, String> params;

    // Private constructor to restrict instantiation
    private ConfigManager() {}

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    // Method to set parameters
    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    // Methods to get individual parameters
    public String getParam(String key) {
        return params.get(key);
    }

    public boolean getBooleanParam(String key) {
        return Boolean.parseBoolean(params.get(key));
    }

    public int getIntParam(String key) {
        return Integer.parseInt(params.get(key));
    }

}
