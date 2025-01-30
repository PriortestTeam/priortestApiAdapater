package com.priortest.step;

public class StepResult {
    private String stepDesc;
    private boolean status;
    private String errorMessage;

    // Constructor
    public StepResult(String stepDesc, boolean status, String errorMessage) {
        this.stepDesc = stepDesc;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // Getters
    public String getStepDesc() {
        return stepDesc;
    }

    public boolean getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // toString() for logging/debugging
    @Override
    public String toString() {
        return "StepResult{" +
                "stepDesc='" + stepDesc + '\'' +
                ", status=" + status +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

