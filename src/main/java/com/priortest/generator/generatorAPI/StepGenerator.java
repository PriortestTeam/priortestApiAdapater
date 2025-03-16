package com.priortest.generator.generatorAPI;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StepGenerator {


    public static void stepGenerateAPI(String stepDefinitionPath, String outputStepLocation ){
        try {
            // Read JSON file
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(stepDefinitionPath)));
            JSONObject root = new JSONObject(content);

            // Process each page (e.g., logonPage, welcomePage, etc.)
            root.keySet().forEach(pageKey -> {
                JSONObject page = root.getJSONObject(pageKey);
                // Create or overwrite file for the page
                String filePath = outputStepLocation + capitalizeFirstLetter(pageKey) + ".java";
                File file = new File(filePath);

                try (FileWriter writer = new FileWriter(file, false)) { // Open FileWriter in overwrite mode
                    // Write package declaration and imports
                    writer.append("package stepFiles;\n\n");
                    writer.append("import com.priortest.step.StepResultTracker;\n\n");
                    writer.append("import io.cucumber.java.en.And;\n\n");


                    writer.append("import org.json.JSONObject;\n\n");
                    writer.append("import org.openqa.selenium.WebDriver;\n");
                    writer.append("import com.priortest.annotation.TestStepApi;\n");
                    writer.append("import org.testng.Assert;\n\n");
                    writer.append("import org.apache.logging.log4j.LogManager;\n");
                    writer.append("import org.apache.logging.log4j.Logger;\n\n");

                    // Write Class declaration
                    writer.append("public class " + capitalizeFirstLetter(pageKey) + " {\n\n");
                    writer.append("\tprivate static final Logger log = LogManager.getLogger(" + capitalizeFirstLetter(pageKey) + ".class);\n");
                    // Add CoreActions as a global instance
                    writer.append("\timport com.priortest.generator.coreAction.CoreActions;\n\n");
                    writer.append("\tpublic " + capitalizeFirstLetter(pageKey) + "(WebDriver driver) {\n");
                    writer.append("\t\tthis.coreAction = new CoreActions(driver);\n");
                    writer.append("\t}\n\n");

                    Set<String> generatedMethods = new HashSet<>(); // Track generated methods to avoid duplicates

                    // Process each step in the page
                    page.keySet().forEach(stepKey -> {
                        JSONObject step = page.getJSONObject(stepKey);
                        String methodName = "step" + capitalizeFirstLetter(stepKey);

                        // Check if the method has already been generated for this class
                        if (!generatedMethods.contains(methodName)) {
                            generateStepMethod(writer, stepKey, step);
                            generatedMethods.add(methodName); // Mark this method as generated
                        }
                    });

                    writer.append("}\n\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        String currentWorkingDirectory = System.getProperty("user.dir");

        String INPUT_FILE_PATH = currentWorkingDirectory+"/src/main/resources/step-definitions.json";
        String OUTPUT_DIR_PATH = currentWorkingDirectory+"/src/test/java/stepFiles/";
        stepGenerateAPI(INPUT_FILE_PATH,OUTPUT_DIR_PATH);
    }

    private static void generateStepMethods(FileWriter writer,String methodName, JSONArray methods, String stepDesc){
        // deal with group method
        StringBuilder methodSignature = new StringBuilder("\tpublic void " + methodName + "(JSONObject inputData");
        methodSignature.append(") {");
        StringBuilder methodBody = new StringBuilder();
        methodBody.append("\t\tboolean stepSuccess = false;\n");
        methodBody.append("\t\tString errorMessage = null;\n");

        methodBody.append("\t\ttry {\n");
        for (Object methodItem : methods) {
            String methodCall = methodItem.toString(); // Get method string, e.g., enterPassword(password)
            int paramStart = methodCall.indexOf("(");
            int paramEnd = methodCall.indexOf(")");
            if (paramStart != -1 && paramEnd != -1 && paramEnd > paramStart + 1) {
                // Method has parameters
                String paramName = methodCall.substring(paramStart + 1, paramEnd).trim(); // Extract parameter name

                methodBody.append("\t\t\tif (inputData.has(\"" + paramName + "\") && inputData.get(\"" + paramName + "\") != null) {\n");
                methodBody.append("\t\t\t\tlog.info(\" Performing Step :  " + methodCall+ "\");\n");
                methodBody.append("\t\t\t\t" + "step"+methodCall.replace(paramName, "(String)inputData.get(\"" + paramName + "\")") + ";\n");
                methodBody.append("\t\t\t}\n");

            } else {
                // Method without parameters
                methodBody.append("\t\t\t" + methodCall + ";\n");
            }
        }

        // End of method body
        methodBody.append("\t\t\tstepSuccess = true;\n");
        methodBody.append("\t\t}\n");
        methodBody.append("\t\tcatch (Exception e) {\n");
        methodBody.append("\t\t\tlog.info(\"An error occurred: \" + e.getMessage());\n");
        methodBody.append( "\t\t\tthrow e;\n")
        .append("\t\t} finally {\n")
        //.append("\t\tif (!stepSuccess) {\n")
                .append("\t\t\tStepResultTracker.addStepResult(\""+stepDesc+"\", stepSuccess, errorMessage);")
                .append("\n\t\t}\n\t}\n");

        try {
            // Add annotations and method definition
            writer.append("\t@TestStepApi(stepDesc = \"" + stepDesc + "\", issueId=\" \")\n");
            writer.append("\t@And(\"" + stepDesc + "\")\n"); // Any keywords: And, Given, Then, When
            writer.append(methodSignature + "\n");
            writer.append(methodBody.toString());
            writer.append("\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private static void generateStepMethod(FileWriter writer, String stepName, JSONObject step) {
        // Prepare the method names and parameters
        String methodName = "step" + capitalizeFirstLetter(stepName);
        String stepDesc = step.getString("desc");
        String method;
        JSONArray methods;
        if (step.toString().contains("methods")){
            methods = step.getJSONArray("methods");
            generateStepMethods(writer,methodName,methods,stepDesc);
        }else {
            method = step.getString("method");
            JSONArray parameters = step.optJSONArray("parameters"); // Use optJSONArray to handle null gracefully
            JSONObject element = step.getJSONObject("element");

            // Generate method signature
            StringBuilder methodSignature = new StringBuilder("\tpublic void " + methodName + "(");

            // If parameters are provided, add them to the method signature
            if (parameters != null) {
                parameters.forEach(param -> {
                    String type = ((JSONObject) param).getString("type");
                    methodSignature.append(type).append(" param, ");
                });
                if (methodSignature.toString().endsWith(", ")) {
                    methodSignature.setLength(methodSignature.length() - 2); // Remove trailing comma
                }
            }

            methodSignature.append(") {");

            // Generate method body
            StringBuilder methodBody = new StringBuilder();
            methodBody.append("\t\tboolean stepSuccess = false;\n");
            methodBody.append("\t\tString errorMessage = null;\n");
            methodBody.append("\t\ttry {\n");
            if (method.toLowerCase().contains("verify")) {
                if (element.isEmpty()) {
                    methodBody.append("\t\t\t boolean result = coreAction." + method + "(");
                } else {
                    // Add CoreActions method call and assertion for verification steps
                    methodBody.append("\t\t\tboolean result = coreAction." + method + "(\"" + element.getString("type") + "\", \"" + element.getString("value") + "\",");
                }
                // Add parameters to the method call
                if (parameters != null) {
                    parameters.forEach(param -> {
                        methodBody.append("param"); // Add parameter variable
                    });
                }
                methodBody.append(");\n");
                methodBody.append("\t\t\t\tAssert.assertTrue(result, \"Verification failed for " + stepName + "\");\n");

            } else if (element.isEmpty()) {
                methodBody.append("\t\t\t\tcoreAction." + method + "(");
                // Add parameters to the method call
                if (parameters != null) {
                    parameters.forEach(param -> {
                        methodBody.append("param"); // Add parameter variable
                    });
                }
                methodBody.append(");\n");

            } else {
                // Add CoreActions method call for actions like click, input, etc.
                methodBody.append("\t\t\t coreAction." + method + "(\"" + element.getString("type") + "\", \"" + element.getString("value") + "\"");

                // Add parameters to the method call
                if (parameters != null) {
                    parameters.forEach(param -> {
                        methodBody.append(", param"); // Add parameter variable
                    });
                }
                methodBody.append(");\n");
            }
            methodBody.append("\t\t\t stepSuccess = true;\n");
            methodBody.append("\t\t\t} catch (Exception e) {\n");
            methodBody.append("\t\t\t errorMessage =e.getMessage();\n");
            methodBody.append("\t\t\tthrow new RuntimeException(e.getMessage());\n");
            methodBody.append("\t\t} finally {\n");
             methodBody.append("\t\t\tStepResultTracker.addStepResult(\""+stepDesc+"\", stepSuccess, errorMessage);");
            //methodBody.append("\t\t\tif (!stepSuccess) {\n");
           // methodBody.append("\t\t\t\tthrow new RuntimeException(\"Step Failed: " + methodName);
            //if (parameters != null) {
           //     parameters.forEach(param -> {
           //         methodBody.append(" with parameter: \" + param"); // Add parameter variable
           //     });
           // } else {
           //     methodBody.append("\"");
           // }

            //methodBody.append("\t\t\t\t}\n");
            methodBody.append("\n\t\t\t}\n");
            methodBody.append("\t\t}\n");

            // Write method to file
            try {
                writer.append("\t@TestStepApi(stepDesc = \"" + stepDesc + "\" ,issueId=\" \")\n");
                writer.append("\t@And(\"" + stepDesc + "\")\n"); // any keywords And, Given, Then, When
                writer.append(methodSignature + "\n");
                writer.append(methodBody.toString());
                writer.append("\n\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String capitalizeFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
