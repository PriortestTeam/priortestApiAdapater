package com.priortest.step;

public class StepResult {
    private String stepDesc;
    private boolean status;
    private String linkedIssueId;

    // Constructor
    public StepResult(String stepDesc, boolean status, String linkedIssueId) {
        this.stepDesc = stepDesc;
        this.status = status;
        this.linkedIssueId = linkedIssueId;
    }

    // Getters
    public String getStepDesc() {
        return stepDesc;
    }

    public boolean getStatus() {
        return status;
    }

    public String getLinkedIssueId() {
        return linkedIssueId;
    }
}

