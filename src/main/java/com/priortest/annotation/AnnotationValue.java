package com.priortest.annotation;

public class AnnotationValue {

    public static void printAnnotationValues(Class<?> clazz) {
        try {
            TestCaseApi annotation = clazz.getMethod("testLogin").getAnnotation(TestCaseApi.class);
            if (annotation != null) {
                String testCaseId = annotation.testCaseId();
                String feature = annotation.feature();
                System.out.println("testCaseId: " + testCaseId);
                System.out.println("feature: " + feature);
            } else {
                System.out.println("@testAPI annotation not found on method.");
            }
        } catch (NoSuchMethodException e) {
            System.err.println("Method testLogin not found.");
        }
    }

}
