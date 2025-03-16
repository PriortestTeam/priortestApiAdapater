package com.priortest.generator.generatorAPI;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CoreActionGenerator {

    // Method to build parameters from the JSON object
    private static String buildParameters(String parameterName, boolean includeTypeAndLocator) {
        StringBuilder parameters = new StringBuilder();
        try {
            JSONObject param = new JSONObject(parameterName);
            String paramType = param.getString("type");
            int paramCount = param.getInt("number");

            // Optionally include type and locator
            if (includeTypeAndLocator) {
                parameters.append("String type, String locator");
            }

            // Add dynamic parameters based on the number field
            for (int j = 0; j < paramCount; j++) {
                if (parameters.length() > 0) {
                    parameters.append(", "); // Add comma for separating parameters
                }
                parameters.append(paramType).append(" param").append(j + 1);
            }
        } catch (Exception e) {
            if (includeTypeAndLocator) {
                parameters.append("String type, String locator"); // Fallback for invalid JSON
            }
        }
        return parameters.toString();
    }

    static void coreActionGeneratorAPI(String coreActionFilePath, String outPutCoreActionFilePath) {
        try {
            // Read the JSON file
            String content = new String(Files.readAllBytes(Paths.get(coreActionFilePath)));
            JSONArray jsonArray = new JSONArray(content);

            System.out.println("Number of methods read from JSON: " + jsonArray.length());

            // Generate CoreAction.java
            String generatedCode = generateCoreActionClass(jsonArray);

            String[] generatedMethods = generatedCode.split("public ");
            System.out.println("Number of methods generated: " + (generatedMethods.length - 3));

            for (int i = 1; i < generatedMethods.length; i++) {
                System.out.println("Method " + i + ": " + generatedMethods[i].split("\\{")[0].trim());
            }

            // Write to the output file
            writeToFile(outPutCoreActionFilePath, generatedCode);

            System.out.println("CoreAction.java has been generated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String currentWorkingDirectory = System.getProperty("user.dir");
        String INPUT_FILE_PATH = currentWorkingDirectory + "/src/main/java/com/priortest/generator/resource/coreActions.json";
        String OUTPUT_DIR_PATH = currentWorkingDirectory + "/src/main/java/com/priortest/generator/coreAction/CoreActions.java";
        coreActionGeneratorAPI(INPUT_FILE_PATH, OUTPUT_DIR_PATH);

    }

    private static String generateCoreActionClass(JSONArray jsonArray) {
        StringBuilder classBuilder = new StringBuilder();
        // Class Header
        classBuilder.append("package com.priortest.generator.coreAction;\n")
                .append("import org.openqa.selenium.*;\n")
                .append("import org.openqa.selenium.support.ui.Select;\n").append("import java.util.List;\n\n").append("import java.util.Set;\n")
                .append("import org.openqa.selenium.interactions.Actions;\n\n")

                .append("public class CoreActions {\n\n")
                .append("\tprivate WebDriver driver;\n\n")
                .append("\tpublic CoreActions(WebDriver driver) {\n").append("\t\tthis.driver = driver;\n").append("\t}\n\n")
                .append("\tprivate WebElement findElement(String type, String value) {\n")
                .append("\t\t switch (type) {\n").append("\t\t\tcase \"id\":\n").
                append("\t\t\t\treturn driver.findElement(By.id(value));\n")
                .append("\t\t\tcase \"className\":\n").append("\t\t\t\treturn driver.findElement(By.className(value));\n")
                .append("\t\t\tcase \"name\":\n")
                .append("\t\t\t\treturn driver.findElement(By.name(value));\n").append("\t\t\tcase \"partialLinkText\":\n")
                .append("\t\t\t\treturn driver.findElement(By.partialLinkText(value));\n")
                .append("\t\t\t\tcase \"tagName\":\n")
                .append("\t\t\t\treturn driver.findElement(By.tagName(value));\n")
                .append("\t\t\tcase \"xpath\":\n").append("\t\t\t\treturn driver.findElement(By.xpath(value));\n")
                .append("\t\t\tcase \"cssSelector\":\n").append("\t\t\t\treturn driver.findElement(By.cssSelector(value));\n")
                .append("\t\t\tcase \"linkText\":\n").append("\t\t\t\treturn driver.findElement(By.linkText(value));\n")
                .append("\t\t\tdefault:\n").append("\t\t\t\t\tthrow new IllegalArgumentException(\"Invalid locator type: \" + type);\n")
                .append("\t\t}\n").append("\t}\n\n").append("\tprivate List<WebElement> findElements(String type, String value) {\n")
                .append("\t\tswitch (type) {\n").append("\t\t\tcase \"id\":\n")
                .append("\t\t\t\t\treturn driver.findElements(By.id(value));\n")
                .append("\t\t\tcase \"className\":\n").append("\t\t\t\t\treturn driver.findElements(By.className(value));\n")
                .append("\t\t\tcase \"name\":\n").append("\t\t\t\t\treturn driver.findElements(By.name(value));\n").append("\t\t\tcase \"partialLinkText\":\n")
                .append("\t\t\t\treturn driver.findElements(By.partialLinkText(value));\n").append("\t\t\tcase \"tagName\":\n")
                .append("\t\t\t\treturn driver.findElements(By.tagName(value));\n").append("\t\t\tcase \"xpath\":\n")
                .append("\t\t\t\treturn driver.findElements(By.xpath(value));\n").append("\t\t\tcase \"cssSelector\":\n")
                .append("\t\t\t\treturn driver.findElements(By.cssSelector(value));\n").append("\t\t\tcase \"linkText\":\n")
                .append("\t\t\t\treturn driver.findElements(By.linkText(value));\n").append("\t\t\t\tdefault:\n")
                .append("\t\t\t\tthrow new IllegalArgumentException(\"Invalid locator type: \" + type);\n").append("        }\n")
                .append("\t}\n\n");

        // Generate Methods
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            String methodName = jsonObject.getString("methodName");
            String seleniumAction = jsonObject.getString("seleniumAction");
            String parameterName = jsonObject.optString("parameterName", null);
            String description = jsonObject.getString("description");
            String returnType = jsonObject.optString("return", "void"); // Default to "void" if not provided

            // Append JavaDoc for the method
            classBuilder.append("    /**\n").append("     * ").append(description).append("\n").append("     */\n");

            // Method Signature
            if (returnType.equalsIgnoreCase("string")) {
                classBuilder.append("    public String ").append(methodName).append("(");
            } else if (returnType.equalsIgnoreCase("boolean")) {
                classBuilder.append("    public boolean ").append(methodName).append("(");
            } else if (returnType.equalsIgnoreCase("int")) {
                classBuilder.append("    public int ").append(methodName).append("(");
            } else {
                classBuilder.append("    public void ").append(methodName).append("(");
            }

            // Handle method parameters based on "parameterName"
            // Main logic for handling parameter generation
            if ("basic".equals(parameterName)) {
                classBuilder.append("String type, String locator");
            } else if ("ImplicitWait".equals(methodName)) {
                // Special case for ImplicitWait: No type and locator
                classBuilder.append(buildParameters(parameterName, false));
            } else if ("openWebPage".equals(methodName) || "getVerifyPageTitle".equals(methodName)) {
                classBuilder.append("String param1");
                // no basic String type, and String locator for openWebPage
            } else if (parameterName != null) {
                // General case: Include type and locator
                classBuilder.append(buildParameters(parameterName, true));
            }
            // Closing the method signature
            classBuilder.append(") {\n");

            // Method Body based on seleniumAction
            switchToGenerateMethodBody(classBuilder, seleniumAction);

            classBuilder.append("    }\n\n");
        }

        // Class Footer
        classBuilder.append("}\n");

        return classBuilder.toString();
    }

    public static void switchToGenerateMethodBody(StringBuilder classBuilder, String seleniumAction) {
        switch (seleniumAction) {

            case "switchToNewPopupWindow":
                classBuilder.append("\t\tString mainWindowHandle = driver.getWindowHandle();\n\n")
                        .append("\t\tSet<String> allWindowHandles = driver.getWindowHandles();\n")
                        .append("\n").append("\t\t\tfor (String handle : allWindowHandles) {\n")
                        .append("\t\t\t\tif (!handle.equals(mainWindowHandle)) {\n")
                        .append("\t\t\t\t\tdriver.switchTo().window(handle);\n")
                        .append("\t\t\t\t\tbreak;").append("\n\t\t\t}\n\t\t\t\t}");
                break;
            case "switchToMainWin":
                classBuilder.append("\t\tString mainWindowHandle = driver.getWindowHandle();\n").append("\t\tdriver.switchTo().window(mainWindowHandle);");
                break;
            case "scrollToBottom":
                classBuilder.append("\t\tJavascriptExecutor js = (JavascriptExecutor) driver;")
                        .append("\n").append("\t\tjs.executeScript(\"window.scrollTo(0, document.body.scrollHeight);\");\n");
                break;
            case "scrollToTop":
                classBuilder.append("\t\tJavascriptExecutor js = (JavascriptExecutor) driver;")
                        .append("\n").append("\t\tjs.executeScript(\"window.scrollTo(0, 0);\");\n");
                break;
            case "scrollToElement":
                classBuilder.append("\t\tWebElement element = findElement(type,locator);\n")
                        .append("\t\tJavascriptExecutor js = (JavascriptExecutor) driver;\n").append("js.executeScript(\"arguments[0].scrollIntoView(true);\", element);\n");
                break;
            case "scrollToRight":
                classBuilder.append("\t\tJavascriptExecutor js = (JavascriptExecutor) driver;").append("\n").append("\t\tjs.executeScript(\"window.scrollBy(500, 0);\");\n");
                break;
            case "scrollToLeft":
                classBuilder.append("\t\tJavascriptExecutor js = (JavascriptExecutor) driver;").append("\n").append("\t\tjs.executeScript(\"window.scrollBy(-500, 0);\");\n");
                break;
            case "get":
                classBuilder.append("\t\tdriver.get(param1);\n");
                break;
            case "getCurrentUrl":
                classBuilder.append("\t\treturn driver.getCurrentUrl();\n");
                break;
            case "verifiedPageTitle":
                classBuilder.append("\t\treturn driver.getTitle().contains(param1);\n");
                break;
            case "getElementTagName":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\treturn element.getTagName();\n");
                break;
            case "getElementText":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\treturn element.getText();\n");
                break;
            case "sendKeys":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n")
                        .append("\t\tfor (char c : param1.toCharArray()) {\n" + "\t\t\telement.sendKeys(String.valueOf(c));\n" + "\t\t} element.sendKeys(Keys.TAB);\n");
                break;
            case "click":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\telement.click();\n");
                break;
            case "submit":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\telement.submit();\n");
                break;
            case "clear":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\telement.clear();\n");
                break;
            case "verifiedHyperLink":
                classBuilder.append("\t\tWebElement linkElement = findElement(type, locator);\n").append("\t\treturn linkElement.getTagName().equalsIgnoreCase(\"a\") && linkElement.getAttribute(\"href\") != null;\n");
                break;
            case "verifiedPartialText":
                classBuilder.append("\t\tWebElement linkElement = findElement(type, locator);\n").append("\t\treturn linkElement.getTagName().equalsIgnoreCase(\"a\") && linkElement.getAttribute(\"href\") != null;\n");
                break;
            case "verifiedText":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tString actualText = element.getText();\n").append("        return actualText.contentEquals(param1) ;\n");
                break;
            case "verifiedImage":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\treturn !driver.findElements((By) element).isEmpty() ;\n");
                break;
            case "verifiedMultipleText":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tString actualText = element.getText();\n").append("        for (String expectedText : param1) {\n").append("           if (actualText.contentEquals(expectedText)) {\n").append("               System.out.println(\"actualText does not match expectedText\" ); \n").append("                return false;\n").append("            }\n").append("        }\n").append("        return true;\n");
                break;
            case "ImplicitWait":
                classBuilder.append("\t\tdriver.manage().timeouts().implicitlyWait(param1, java.util.concurrent.TimeUnit.SECONDS);\n");
                break;
            case "quit":
                classBuilder.append("\t\tdriver.quit();\n");
                break;
            case "close":
                classBuilder.append("\t\tdriver.close();\n");
                break;
            case "getAttribute":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\treturn element.getAttribute(param1);\n");
                break;
            case "isDisplayed":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\treturn element.isDisplayed();\n");
                break;
            case "isSelected":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\treturn element.isSelected();\n");
                break;
            case "isEnabled":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\treturn element.isEnabled();\n");
                break;
            case "waitForElement":
                classBuilder.append("\t\tWebDriverWait wait = new WebDriverWait(driver, 10);\n").append("\t\tWebElement element = wait.until(driver -> driver.findElement(By.xpath(locator)));\n").append("        return element;\n");
                break;

            case "deSelectAllCheckBox":
                classBuilder.append("\t\tList<WebElement> checkBoxes = findElements(type, locator);\n").append("\t\tfor (WebElement checkBox  : checkBoxes) {\n").append("            if (checkBox.isSelected()) {\n").append("                checkBox.click();\n").append("            }\n").append("        }\n");
                break;

            case "selectAllCheckBox":
                classBuilder.append("\t\tList<WebElement> checkBoxes = findElements(type, locator);\n").append("\t\tfor (WebElement checkBox  : checkBoxes) {\n").append("            if (!checkBox.isSelected()) {\n").append("                checkBox.click();\n").append("            }\n").append("        }\n");
                break;

            case "selectAllOptionsFromDropDown":
                classBuilder.append("\t\tWebElement selectElement = findElement(type, locator);\n").append("\t\tSelect select = new Select(selectElement);\n").append("        List<WebElement> options = select.getOptions();\n").append("        for (WebElement option : options) {\n").append("            if (!option.isSelected()) {\n").append("                option.click();\n").append("            }\n").append("        }\n");
                break;
            case "selectOptionByText":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tnew Select(element).selectByVisibleText(param1);\n");
                break;
            case "selectOptionById":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tnew Select(element).selectByIndex(param1);\n");
                break;
            case "selectOptionByValue":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tnew Select(element).selectByValue(param1);\n");
                break;
            case "unSelectOptionByText":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tSelect select = new Select(element);\n").append("        select.deselectByVisibleText(param1);\n");
                break;
            case "unSelectAllOptions":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tSelect select = new Select(element);\n").append("        select.deselectAll();\n");
                break;
            case "unSelectOptionById":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tSelect select = new Select(element);\n").append("        select.deselectByIndex(param1);\n");
                break;
            case "unSelectOptionByValue":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tSelect select = new Select(element);\n").append("        select.deselectByValue(param1);\n");
                break;
            case "handOver":
                classBuilder.append("\t\tWebElement element = findElement(type, locator);\n").append("\t\tActions actions = new Actions(driver);\n").append("            actions.moveToElement(element).perform();\n").append("        try {\n" + "            Thread.sleep(10000);\n" + "        } catch (InterruptedException e) {\n" + "            throw new RuntimeException(e);\n" + "        \n}");
                break;

            default:
                classBuilder.append("\t\t// Add custom logic for seleniumAction: ").append(seleniumAction).append("\n");
                break;
        }
    }

    private static void writeToFile(String filePath, String content) throws IOException {
        File file = new File(filePath);
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.close();
    }
}
