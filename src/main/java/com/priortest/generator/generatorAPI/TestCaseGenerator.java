package com.priortest.generator.generatorAPI;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCaseGenerator {

    private static final Logger log = LogManager.getLogger(TestCaseGenerator.class);
    private final ArrayList<String> classNames = new ArrayList<>(); // Initialize the list;
    private final Set<String> importedClasses = new HashSet<>();
    private final StringBuilder imports = new StringBuilder();
    private StringBuilder classLevel;
    private String param;
    private int paramInt;
    private boolean stepParamIndicator;
    private String[] paramArray;
    private float paramFloat;

    public static void main(String[] args) {

        String currentWorkingDirectory = System.getProperty("user.dir");
        log.info("current Working Directory: " + currentWorkingDirectory);

        String[] stepDefinitionJsonsString = new String[]{currentWorkingDirectory+"/src/main/resources/step-definitions.json"};
        String featureFilePath = currentWorkingDirectory+"/src/test/resources/testCaseFeature/loginTest.feature";
        String outputFileLocation = currentWorkingDirectory+"/src/test/java/testCases/";
        generateAPI(stepDefinitionJsonsString,featureFilePath,outputFileLocation);
    }

    public static void generateAPI(String[] stepDefinitionJsonsString, String featureFilePath, String outputFileLocation){
        // get generated java file as per given feature file name
        String featureFileName = Paths.get(featureFilePath).getFileName().toString();
        featureFileName = featureFileName.substring(0, featureFileName.lastIndexOf('.')); // used for class name
        featureFileName = featureFileName.substring(0, 1).toUpperCase() + featureFileName.substring(1);

        String generatedTestCaseFileName = featureFileName + "FeatureTestCase.java";
        String generatedJavaTestFileFullPath = outputFileLocation + generatedTestCaseFileName;

        log.info("Create Java Test Case File " + generatedJavaTestFileFullPath);
        log.info("For Feature File " + featureFilePath);

        TestCaseGenerator generator = new TestCaseGenerator();
        generator.generateTestCases(featureFilePath, stepDefinitionJsonsString, generatedJavaTestFileFullPath, featureFileName);
    }

    public static JSONObject convertExamplesToJson(Example example) {
        JSONObject jsonObject = new JSONObject();

        // Use reflection to get all fields of the Example class
        Field[] fields = example.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true); // Allow access to private fields
            try {
                // Use field name as the key and its value as the value
                String key = field.getName();
                Object value = field.get(example);

                // If the value is not null, add it to the JSONObject
                if (value != null) {
                    // If the value is a complex object (e.g., another Example), recursively convert it to JSON
                    if (value instanceof Example) {
                        jsonObject.put(key, convertExamplesToJson((Example) value));
                    } else if (value instanceof Map) {
                        // If the value is a Map, convert it to JSONObject
                        jsonObject.put(key, new JSONObject((Map<?, ?>) value));
                    } else if (value instanceof Iterable) {
                        // If the value is an Iterable (like List), convert it to a JSON array
                        jsonObject.put(key, new org.json.JSONArray((Iterable<?>) value));
                    } else {
                        // Otherwise, just add the value (convert to String if necessary)
                        jsonObject.put(key, value.toString());
                    }
                } else {
                    // Ensure null values are handled correctly
                    jsonObject.put(key, JSONObject.NULL);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }


        // Extract only the 'parameters' part of the JSON
        if (jsonObject.has("parameters")) {
            return jsonObject.getJSONObject("parameters");
        }
        return new JSONObject(); // Return an empty JSON object if 'parameters' are not found
    }

    private JSONObject mergeJsonFiles(String[] filePaths) {
        JSONObject mergedJson = new JSONObject();
        for (String filePath : filePaths) {
            String fileContent = readFileContent(new String[]{filePath});
            JSONObject fileJson = new JSONObject(fileContent);

            for (String key : fileJson.keySet()) {
                // If the key already exists, merge the objects
                if (mergedJson.has(key)) {
                    JSONObject existingObject = mergedJson.getJSONObject(key);
                    JSONObject newObject = fileJson.getJSONObject(key);
                    for (String subKey : newObject.keySet()) {
                        existingObject.put(subKey, newObject.get(subKey));
                    }
                } else {
                    // Add new key-value pair to mergedJson
                    mergedJson.put(key, fileJson.get(key));
                }
            }
        }
        return mergedJson;
    }


    public void generateTestCases(String featureFilePath, String[] stepDefinitionJsonPath, String generatedJavaTestFileFullPath, String featureFileName) {
        // Read the step definition JSON file content
        //String stepDefContent = readFileContent(stepDefinitionJsonPath);
        JSONObject stepDef = mergeJsonFiles(stepDefinitionJsonPath);

        // Parse the step definition JSON
        //JSONObject stepDef = new JSONObject(stepDefContent);

        // Parse the feature file using Cucumber or regular parsing method
        List<Scenario> scenarios = getScenariosFromFeature(featureFilePath);

        // Create a test case file based on the Feature line
        String featureName = getFeatureNameFromFile(featureFilePath);
        StringBuilder testCase = new StringBuilder();

        // Initialize classLevel to hold instance variables (class-level setup)
        classLevel = new StringBuilder();

        // Start the Java file with necessary
        testCase.append("import config.BasicSetup;\n").append("import org.testng.annotations.Test;\n")
                .append("import org.testng.annotations.BeforeClass;\n")
                .append("import org.testng.annotations.*;\n")
                .append("import org.json.JSONObject;\n")
                .append("import static org.testng.Assert.*;\n\n")
                .append("import org.openqa.selenium.WebDriver;\n\n")
                .append("import utils.WebDriverSingleton;\n\n")
                .append("import com.priortest.annotation.TestCaseApi;\n\n")
                .append("import com.priortest.api.PriorTestAPIAdapter;\n\n")
                .append("import com.priortest.config.PTApiConfig;\n\n")
                .append("import com.priortest.config.PTApiFieldSetup;\n\n");

        // Start the public class (named after the feature name)
        testCase.append("@Listeners({ PriorTestAPIAdapter.class })");
        testCase.append("\n");
        testCase.append("public class ").append(featureFileName + "FeatureTestCase").append(" extends BasicSetup {\n\n");

        testCase.append("\tprivate WebDriver driver;\n\n");

        // Loop through scenarios and generate test cases
        for (Scenario scenario : scenarios) {
            if (scenario instanceof ScenarioOutline) {
                ScenarioOutline scenarioOutline = (ScenarioOutline) scenario;
                int i = 0;
                for (Example example : scenarioOutline.getExamples()) {

                    String testCaseName = scenarioOutline.getName().replace(" ", "") + "_" + example.getTestCaseName();
                    generateTestCase(testCase, scenarioOutline, example, testCaseName, featureName, stepDef);  // Pass stepDef here
                }
            }
        }

        // Before class
        testCase.append("\t@BeforeClass\n");
        testCase.append("\tpublic void setUp(){");
        testCase.append("\n");
        testCase.append("\t\tString browser = PTApiConfig.getBrowser();");
        testCase.append("\n");
        testCase.append("\t\tdriver = WebDriverSingleton.getDriver(WebDriverSingleton.BrowserType.valueOf(browser));");
        testCase.append("\n");
        ArrayList<String> importedClass = getSetupClassName();
        for (String name : importedClass) {
            testCase.append("\t\t" + getInstanceName(name) + "= new " + name + "(driver);");
            testCase.append("\n");
        }

        testCase.append("\n\t\t// Set Case Module");
        testCase.append("\n");

        testCase.append("\t\tPTApiFieldSetup.setModule(\""+featureName+"\");");
        // closed Before class
        testCase.append("\n\t}\n\n");

        testCase.append("\t@AfterClass\n");
        testCase.append("\tpublic void setDown(){");
        testCase.append("\n");
        testCase.append("\t\tWebDriverSingleton.quitDriver();");
        testCase.append("\n\t}\n\n");

        testCase.insert(0, imports);
        testCase.insert(0, "package testCases;\n");
        testCase.append(classLevel.toString());
        // Close the class
        testCase.append("\n}\n");

        // Write the generated test case content to a Java file
        writeToFile(generatedJavaTestFileFullPath, testCase.toString());
    }

    private String readFileContent(String[] filePaths) {
        StringBuilder content = new StringBuilder();
        for (String filePath : filePaths) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + filePath);
                e.printStackTrace();
            }
        }
        return content.toString();
    }


    private List<Scenario> getScenariosFromFeature(String featureFilePath) {
        List<Scenario> scenarios = new ArrayList<>();
        Scenario currentScenario = null;
        List<String> examples = new ArrayList<>();

        Step lastStep = null; // Track the last step for JSON association
        boolean isJsonBlock = false;
        StringBuilder jsonBuilder = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(featureFilePath));
            String line;


            while ((line = reader.readLine()) != null) {

                line = line.trim();
                if (line.startsWith("Scenario Outline:")) {
                    // Start of Scenario Outline
                    if (currentScenario != null) {
                        if (currentScenario instanceof ScenarioOutline) {
                            ((ScenarioOutline) currentScenario).setExamples(parseExamples(examples));
                        }
                        scenarios.add(currentScenario);
                    }
                    currentScenario = new ScenarioOutline(line.substring(17).trim());
                    examples.clear();
                } else if (line.startsWith("Scenario:")) {
                    // Start of regular Scenario
                    if (currentScenario != null) {
                        if (currentScenario instanceof ScenarioOutline) {
                            ((ScenarioOutline) currentScenario).setExamples(parseExamples(examples));
                        }
                        scenarios.add(currentScenario);
                    }
                    currentScenario = new Scenario(line.substring(9).trim());

                } else if (line.startsWith("Examples:")) {
                    // Start of Examples block
                    examples.clear();
                    continue;
                } else if (line.startsWith("|")) {
                    examples.add(line);
                } else if (line.startsWith("\"\"\"")) {
                    // Start or end of a JSON block
                    if (!isJsonBlock) {
                        isJsonBlock = true;
                        jsonBuilder.setLength(0); // Clear the builder
                    } else {
                        isJsonBlock = false;
                        // Attach JSON content to the last step
                        if (lastStep != null) {
                            lastStep.setJsonData(jsonBuilder.toString().trim());
                        }
                    }
                } else if (isJsonBlock) {
                    // Inside a JSON block
                    jsonBuilder.append(line).append(System.lineSeparator());

                } else if (line.startsWith("When") || line.startsWith("Given") || line.startsWith("And") || line.startsWith("Then")) {
                    // Regular step line
                    if (currentScenario != null) {
                        Step step = new Step();
                        step.setDescription(line);
                        currentScenario.addStep(step);
                        lastStep = step; // Update the last step reference
                    }
                }
            }
            if (currentScenario != null) {
                if (currentScenario instanceof ScenarioOutline) {
                    ((ScenarioOutline) currentScenario).setExamples(parseExamples(examples));
                }
                scenarios.add(currentScenario);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return scenarios;
    }

    private List<Example> parseExamples(List<String> examples) {
        List<Example> parsedExamples = new ArrayList<>();
        if (!examples.isEmpty()) {
            String headerLine = examples.get(0); // The first line is the header for the examples
            String[] headers = headerLine.substring(1, headerLine.length() - 1).split("\\|");

            for (int i = 1; i < examples.size(); i++) {
                String exampleLine = examples.get(i).substring(1, examples.get(i).length() - 1).trim();
                String[] values = exampleLine.split("\\|");
                Example example = new Example();
                for (int j = 0; j < headers.length; j++) {
                    example.addParameter(headers[j].trim(), values[j].trim());
                }
                example.setTestCaseName(example.getParameter("testCaseName"));
                parsedExamples.add(example);
            }
        }
        return parsedExamples;
    }

    private String formatJsonAsJavaString(String json) {
        return json.trim().replace("\\", "\\\\")  // Escape backslashes
                .replace("\"", "\\\"")  // Escape double quotes
                .replace("\n", "\\n")   // Escape newlines
                .replace("\r", "\\r");  // Escape carriage returns
    }

    private void generateTestCase(StringBuilder testCase, ScenarioOutline scenarioOutline, Example example, String testCaseName, String featureName, JSONObject stepDef) {
        String methodName = "testCase_" + testCaseName;
        String feature = scenarioOutline.getName().replaceAll("\\s+", "");
        String priority = example.getParameter("priority");
        String severity = example.getParameter("severity");
        String caseCategory = example.getParameter("caseCategory");
        String automationId = feature + "_" + example.getParameter("automationId");
        // Add @Test and @TestCaseApi annotation to the method
        testCase.append("\t@Test\n");
        testCase.append("\t@TestCaseApi(feature = \"" + featureName).append("\", priority = \"").append(priority).append("\", severity = \"").append(severity).append("\", caseCategory = \"").append(caseCategory).append("\", automationId = \"").append(automationId).append("\", issueId = {})");
        testCase.append("\n");
        // Define the test method with the generated name
        log.info("===== Added Test Case " + methodName);
        testCase.append("\tpublic void ").append(methodName).append("() {\n");

        // Loop through each step in the scenario outline
        for (Step step : scenarioOutline.getSteps()) {
            String stepDesc = step.getDescription();
            String stepJson = step.getJsonData();

            // Clean up the step description (remove keywords like Given, When, etc.)
            stepDesc = removeCucumberKeywords(stepDesc);
            stepDesc = removeAndExtractedParameter(stepDesc);  // Remove parameters enclosed in < > or {} [] ""

            if (stepDesc.isEmpty()) {
                continue; // Skip this step if it's empty after removing keywords and parameters
            }
            if (stepDesc.startsWith("Wait")) {
                String numberString = stepDesc.replaceAll("\\D+", ""); // This removes all non-digit characters
                int waitTimeInSeconds = Integer.parseInt(numberString);
                // Create Hard Code Wait
                testCase.append("\t\t// Create Hard Code Wait\n");
                testCase.append("\t\ttry { Thread.sleep(" + waitTimeInSeconds * 1000 + ");} catch (InterruptedException e) {throw new RuntimeException(e);}");
            }
            // Look up the step definition using the description
            JSONObject stepDefMethod = findStepDefinition(stepDesc, stepDef);

            // step definition has found
            if (stepDefMethod != null) {
                log.info("Step Definition Found For: " + stepDesc);
                String classNameInDef = stepDefMethod.getString("class");
                String stepMethod = stepDefMethod.getString("stepMethod");
                String instanceName = classNameInDef.substring(0, 1).toLowerCase() + classNameInDef.substring(1);
                String methodNameInDef = "step" + stepDefMethod.getString("stepMethod");
                handleClassImport(classNameInDef); // Call modified import handler

                if (stepJson != null) { // deal with step with Json Parameter
                    String formattedJson = formatJsonAsJavaString(stepJson);
                    testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("(new JSONObject(\"").append(formattedJson).append("\"));\n");

                } else if (getStepParamIndicator()) { // Step with parameter directly
                    // Check the type of the parameter and get the correct value
                    if (getStepParamString() != null && !getStepParamString().isEmpty()) {
                        String paramValue = "\"" + getStepParamString() + "\"";  // Add quotes for String
                        testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("(").append(paramValue).append(");\n");
                    } else if (getStepParamFloat() != 0) {
                        float paramValue = getStepParamFloat();  // Use the integer value
                        testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("(").append(paramValue).append(");\n");
                    } else if (getStepParamInt() != 0) {
                        int paramValue = getStepParamInt();  // Use the integer value
                        testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("(").append(paramValue).append(");\n");
                    } else if (getStepParamArray() != null && getStepParamArray().length > 0) {
                        // Join array values with commas, but without combining them into a single string
                        String paramValue = String.join(",", Arrays.stream(getStepParamArray()).map(param -> "\"" + param + "\"")  // Add quotes to each parameter
                                .toArray(String[]::new)  // Convert to array of strings with quotes
                        );
                        // Append method call with the properly formatted parameters
                        testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("(").append(paramValue).append(");\n");
                    }
                } else if (stepMethod.toLowerCase().contains("group")) { // Step definition is Group
                    JSONObject dataTable = convertExamplesToJson(example);
                    testCase.append("\t\tJSONObject inputData = new JSONObject();");
                    testCase.append("\n");
                    for (String key : dataTable.keySet()) {
                        Object value = dataTable.get(key);
                        testCase.append("\t\tinputData.put(\"" + key + "\", \"" + value.toString() + "\");\n");
                    }
                    testCase.append("\n");
                    testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("(").append("inputData").append(");\n");
                } else { // Step with parameter in Example
                    // Handle parameters from the example for the step
                    String param = example.getParameterForStep(step);
                    // Generate the method call with the parameter if required
                    if (param != null && !param.isEmpty()) {
                        testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("(").append("\"" + param + "\"").append(");\n");
                    } else {
                        // If no parameter is needed, just call the method without parameters
                        testCase.append("\t\t").append(instanceName).append(".").append(methodNameInDef).append("();\n");
                    }
                }
            } else {
                // If no step definition is found, add a placeholder
                log.warn("Step Definition Not Found For: " + stepDesc);
                testCase.append("\n\t\t// Auto-generated method: ").append(stepDesc).append("\n").append("\t\t// Placeholder for method: ").append(generateMethodName(stepDesc)).append("();\n");
            }
        }

        // Close the test method
        testCase.append("\t}\n\n");
    }

    private void handleClassImport(String className) {
        // Assuming the package is provided as part of the class name (e.g., "com.example.LoginPage")
        String classNameWithPackage = "stepFiles." + className; // Modify as per actual package name

        // Check if the class is already imported
        if (!importedClasses.contains(className)) {
            // Add import statement
            imports.append("import ").append(classNameWithPackage).append(";\n");

            // Track imported class
            importedClasses.add(className);

            // Add code to instantiate the class
            // This will create a line in the test case like: private ClassName className = new ClassName();
            // Note: Using className.toLowerCase() as the variable name.
            String instanceName = className.substring(0, 1).toLowerCase() + className.substring(1);
            classLevel.append("\t\tprivate ").append(className).append(" ").append(instanceName).append(";").append("\n");
            setupClassName(className);
        }
    }

    private String getInstanceName(String className) {
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    private void setupClassName(String className) {
        classNames.add(className); // Add the className to the list
    }

    private ArrayList<String> getSetupClassName() {
        return classNames; // Return the list
    }

    // Removes Cucumber keywords (Given, When, Then, And)
    private String removeCucumberKeywords(String stepDesc) {
        return stepDesc.replaceAll("^(Given|When|Then|And)\\s+", "").trim();
    }

    // Removes parameters inside angle brackets, e.g., <url> becomes an empty string
    private String removeAndExtractedParameter(String stepDesc) {
        boolean stepParamIndicator = false;
        setStepParamString(null);
        setStepParamInt(0);
        setStepParamArray(null);

        if (stepDesc.contains("<")) {
            stepDesc = stepDesc.replaceAll("<[^>]*>", "").trim();
            setStepParamIndicator(stepParamIndicator);
        } else if (stepDesc.contains("\"")) { // consider as String
            String parameter = stepDesc.replaceAll(".*\"([^\"]*)\".*", "$1"); // Extract content inside "..."
            setStepParamString(parameter);
            stepDesc = stepDesc.replaceAll("\"[^\"]*\"", "").trim(); // Remove "..."
            setStepParamIndicator(true);
            log.info("==== Step Param String " + parameter);
        } else if (stepDesc.contains("{")) { // consider as int or float
            try {
                int parameter = Integer.parseInt(stepDesc.replaceAll(".*\\{([^}]*)}.*", "$1").trim());  // Extract integer content inside {...}
                setStepParamInt(parameter);  // Set parameter as integer
                stepDesc = stepDesc.replaceAll("\\{[^}]*}", "").trim();  // Remove {...}
                setStepParamIndicator(true);
                log.info("==== Step Param Int: " + parameter);
            } catch (NumberFormatException e) {
                log.warn("Error Parsing Integer Parameter From: , Try To Parsing Into Float: " + stepDesc, e);
                try {
                    float floatParameter = Float.parseFloat(stepDesc.replaceAll(".*\\{([^}]*)}.*", "$1").trim());  // Extract float content inside {...}
                    setStepParamFloat(floatParameter);  // Set parameter as float
                    stepDesc = stepDesc.replaceAll("\\{[^}]*}", "").trim();  // Remove {...}
                    setStepParamIndicator(true);
                    log.info("==== Step Param Float: " + floatParameter);
                } catch (NumberFormatException ex) {
                    log.error("Error Parsing Float Parameter From: " + stepDesc, ex);
                }
            }
        } else if (stepDesc.contains("[")) { // consider as multiple
            String[] parameter = stepDesc.replaceAll(".*\\[([^]]*)].*", "$1").split(",");  // Extract and split content inside [...]
            setStepParamArray(parameter);
            setStepParamIndicator(true);
            stepDesc = stepDesc.replaceAll("\\[[^]]*]", "").trim(); // Remove [...]
            log.info("==== Step Param String Array " + parameter);
        } else {
            stepDesc = stepDesc.trim();
            setStepParamIndicator(stepParamIndicator);
        }
        return stepDesc;
    }

    String[] getStepParamArray() {
        return this.paramArray;
    }

    void setStepParamArray(String[] param) {
        this.paramArray = param;
    }

    boolean getStepParamIndicator() {
        return this.stepParamIndicator;
    }

    void setStepParamIndicator(boolean indicator) {
        this.stepParamIndicator = indicator;
    }

    float getStepParamFloat() {
        return this.paramFloat;
    }

    void setStepParamFloat(float param) {
        this.paramFloat = param;
    }

    String getStepParamString() {
        return this.param;
    }

    void setStepParamString(String param) {
        this.param = param;
    }

    int getStepParamInt() {
        return this.paramInt;
    }

    void setStepParamInt(int param) {
        this.paramInt = param;
    }

    private JSONObject findStepDefinition(String stepDesc, JSONObject stepDef) {
        for (String className : stepDef.keySet()) {
            JSONObject classDef = stepDef.getJSONObject(className);
            for (String stepMethod : classDef.keySet()) {
                JSONObject stepDetails = classDef.getJSONObject(stepMethod);
                // Match the step description with the step definition
                if (stepDetails.getString("desc").equalsIgnoreCase(stepDesc)) {
                    stepDetails.put("stepMethod", stepMethod);  // Add stepMethod to the step definition
                    stepDetails.put("class", className);  // Add class name to the step definition
                    return stepDetails;
                }
            }
        }
        return null; // No matching step found
    }

    private String generateMethodName(String stepDesc) {
        // Convert step description to camel case for method name
        String[] words = stepDesc.split(" ");
        StringBuilder methodName = new StringBuilder();
        for (String word : words) {
            methodName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return methodName.toString();
    }

    private void writeToFile(String fileName, String content) {
        try {
            File file = new File(fileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFeatureNameFromFile(String featureFilePath) {
        String featureName = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(featureFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Feature:")) {
                    featureName = line.substring(8).trim();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return featureName;
    }

    // Placeholder classes for the feature parsing logic
    static class Scenario {
        String name;
        List<Step> steps = new ArrayList<>();

        public Scenario(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<Step> getSteps() {
            return steps;
        }

        public void addStep(Step step) {
            steps.add(step);
        }
    }

    static class ScenarioOutline extends Scenario {
        List<Example> examples = new ArrayList<>();

        public ScenarioOutline(String name) {
            super(name);
        }

        public List<Example> getExamples() {
            return examples;
        }

        public void setExamples(List<Example> examples) {
            this.examples = examples;
        }
    }

    static class Step {
        String description;
        String jsonData;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getJsonData() {
            return jsonData;
        }

        public void setJsonData(String jsonData) {
            this.jsonData = jsonData;
        }

    }

    static class Example {
        Map<String, String> parameters = new HashMap<>();
        String testCaseName;
        private String parameterValue;

        public void addParameter(String name, String value) {
            parameters.put(name, value);
        }

        public String getParameter(String name) {
            return parameters.get(name);
        }

        public String getTestCaseName() {
            return testCaseName;
        }

        public void setTestCaseName(String testCaseName) {
            this.testCaseName = testCaseName;
        }

        public String getParameterForStep(Step step) {
            parameterValue = null;
            String description = step.getDescription();
            Pattern pattern = Pattern.compile("<(.*?)>");
            Matcher matcher = pattern.matcher(description);
            if (matcher.find()) {
                String key = matcher.group(1); // Extract the key, e.g., "password"
                parameterValue = parameters.getOrDefault(key, "");
            }
            return parameterValue;
        }
    }
}
