package com.priortest.step;

import java.util.ArrayList;
import java.util.List;

public class StepResultTracker
{

    private static final ThreadLocal<List<StepResult>> stepResults = ThreadLocal.withInitial(() -> new ArrayList<StepResult>());

    public static List<StepResult> getStepResults() {
        return stepResults.get();
    }

    public static void addStepResult(String stepDesc, boolean status, String errorMessage) {
        stepResults.get().add(new StepResult(stepDesc, status, errorMessage));
    }

    public static void clearStepResults() {
        stepResults.remove();
    }
}
