package com.priortest.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.List;

public class CustomSoftAssert extends SoftAssert {

    private static ThreadLocal<List<AssertionResult>> assertionResults = ThreadLocal.withInitial(() -> new ArrayList<>());
    private static final Logger log = LogManager.getLogger(CustomSoftAssert.class);
    @Override
    public void onAssertSuccess(IAssert<?> assertCommand) {
        super.onAssertSuccess(assertCommand);
        assertionResults.get().add(new AssertionResult(true, assertCommand.getMessage()));
        log.info ("PASS:   " + assertCommand.getMessage());
    }

    @Override
    public void onAssertFailure(IAssert<?> assertCommand, AssertionError ex) {
        super.onAssertFailure(assertCommand, ex);
        assertionResults.get().add(new AssertionResult(false, assertCommand.getMessage()));
        log.info ("FAIL: " + assertCommand.getMessage() + " - " + ex.getMessage());
    }

    public static List<AssertionResult> getAssertionResults() {
        return assertionResults.get();
    }

    public static void clearResults() {
        assertionResults.remove(); // Clears the thread-local results
    }

    public static class AssertionResult {
        private final boolean status;
        private final String message;

        public AssertionResult(boolean status, String message) {
            this.status = status;
            this.message = message;
        }

        public boolean getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
