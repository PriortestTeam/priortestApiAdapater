package com.priortest.generator.generatorAPI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FeatureFileParser {

    static class Step {
        String keyword;
        String description;

        Step(String keyword, String description) {
            this.keyword = keyword;
            this.description = description;
        }

        @Override
        public String toString() {
            return keyword + " " + description;
        }
    }

    static class Scenario {
        String name;
        List<Step> steps = new ArrayList<>();

        Scenario(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Scenario: " + name + "\nSteps: " + steps;
        }
    }

    static class Feature {
        String name;
        String description;
        List<Scenario> scenarios = new ArrayList<>();

        Feature(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public String toString() {
            return "Feature: " + name + "\nDescription: " + description + "\nScenarios: " + scenarios;
        }
    }

    public static Feature parseFeatureFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        Feature feature = null;
        Scenario currentScenario = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("Feature:")) {
                String featureName = line.substring(8).trim();
                feature = new Feature(featureName, "");
            } else if (line.startsWith("Scenario:")) {
                String scenarioName = line.substring(9).trim();
                currentScenario = new Scenario(scenarioName);
                if (feature != null) feature.scenarios.add(currentScenario);
            } else if (currentScenario != null && isStep(line)) {
                String keyword = line.split(" ")[0];
                String description = line.substring(keyword.length()).trim();
                currentScenario.steps.add(new Step(keyword, description));
            }
        }
        reader.close();
        return feature;
    }

    private static boolean isStep(String line) {
        return line.startsWith("Given") || line.startsWith("When") ||
                line.startsWith("Then") || line.startsWith("And");
    }

    public static void generateTestCaseFile(Feature feature, String outputPath) throws IOException {
        String className = feature.name.replaceAll("\\s+", "") + "Test";
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath + "/" + className + ".java"))) {
            // Write package and imports
            writer.println("import org.testng.annotations.Test;;");

            // Write class declaration
            writer.println("public class " + className + " {\n");

            // Write scenario methods
            for (Scenario scenario : feature.scenarios) {
                String methodName = scenario.name.replaceAll("\\s+", "");
                writer.println("    @Test");
                writer.println("    public void " + methodName + "() {");
                for (Step step : scenario.steps) {
                    writer.println("        // " + step.keyword + " " + step.description);
                    writer.println("        " + getMethodCall(step) + ";");
                }
                writer.println("    }\n");
            }

            writer.println("}");
        }
    }

    private static String getMethodCall(Step step) {
        // Generate a placeholder method call based on the step keyword and description
        String methodName = step.description.replaceAll("[^a-zA-Z0-9]", "");
        return "perform" + step.keyword + methodName;
    }

    public static void main(String[] args) {
        try {
            final String INPUT_FILE_PATH = "D:/testCaseGenerator/testCaseGenerator/src/main/resources/LoginTest.feature";
            final String OUTPUT_DIR_PATH = "D:\\testCaseGenerator\\testCaseGenerator\\src\\test\\java\\";

            //String featureFilePath = "LoginTest.feature";
            //String outputDirectory = "output"; // Directory to save the generated test case file

            //new File(outputDirectory).mkdirs();

            Feature feature = parseFeatureFile(INPUT_FILE_PATH);
            System.out.println(feature); // For verification
            generateTestCaseFile(feature, OUTPUT_DIR_PATH);

            System.out.println("Test case file generated at: " + OUTPUT_DIR_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
