package com.priortest.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.PACKAGE, ElementType.TYPE})
public @interface TestStepApi {
    String stepDesc();
    String issueId();
}
