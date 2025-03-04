package com.priortest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TestCaseApi {

    String feature() default "";

    String testName() default "";

    String automationId();

    String[] issueId();

    String priority();

    String caseCategory();
    String severity();


}

