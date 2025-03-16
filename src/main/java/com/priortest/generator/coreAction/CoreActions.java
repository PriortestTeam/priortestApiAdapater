package com.priortest.generator.coreAction;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;
import java.util.List;

import java.util.Set;
import org.openqa.selenium.interactions.Actions;

public class CoreActions {

	private WebDriver driver;

	public CoreActions(WebDriver driver) {
		this.driver = driver;
	}

	private WebElement findElement(String type, String value) {
		 switch (type) {
			case "id":
				return driver.findElement(By.id(value));
			case "className":
				return driver.findElement(By.className(value));
			case "name":
				return driver.findElement(By.name(value));
			case "partialLinkText":
				return driver.findElement(By.partialLinkText(value));
				case "tagName":
				return driver.findElement(By.tagName(value));
			case "xpath":
				return driver.findElement(By.xpath(value));
			case "cssSelector":
				return driver.findElement(By.cssSelector(value));
			case "linkText":
				return driver.findElement(By.linkText(value));
			default:
					throw new IllegalArgumentException("Invalid locator type: " + type);
		}
	}

	private List<WebElement> findElements(String type, String value) {
		switch (type) {
			case "id":
					return driver.findElements(By.id(value));
			case "className":
					return driver.findElements(By.className(value));
			case "name":
					return driver.findElements(By.name(value));
			case "partialLinkText":
				return driver.findElements(By.partialLinkText(value));
			case "tagName":
				return driver.findElements(By.tagName(value));
			case "xpath":
				return driver.findElements(By.xpath(value));
			case "cssSelector":
				return driver.findElements(By.cssSelector(value));
			case "linkText":
				return driver.findElements(By.linkText(value));
				default:
				throw new IllegalArgumentException("Invalid locator type: " + type);
        }
	}

    /**
     * switch to new pop up window 
     */
    public void switchToNewPopupWindow() {
		String mainWindowHandle = driver.getWindowHandle();

		Set<String> allWindowHandles = driver.getWindowHandles();

			for (String handle : allWindowHandles) {
				if (!handle.equals(mainWindowHandle)) {
					driver.switchTo().window(handle);
					break;
			}
				}    }

    /**
     * switch to main window 
     */
    public void switchToMainWin() {
		String mainWindowHandle = driver.getWindowHandle();
		driver.switchTo().window(mainWindowHandle);    }

    /**
     * Scroll to the Bottom of the Page
     */
    public void scrollToBottom() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    /**
     * Scroll to the Top  of the Page
     */
    public void scrollToTop() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollTo(0, 0);");
    }

    /**
     * Scroll to left
     */
    public void scrollToLeft() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollBy(-500, 0);");
    }

    /**
     * Scroll to right Element
     */
    public void scrollToRight() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollBy(500, 0);");
    }

    /**
     * Scroll to Specific Element
     */
    public void scrollToElement(String type, String locator) {
		WebElement element = findElement(type,locator);
		JavascriptExecutor js = (JavascriptExecutor) driver;
js.executeScript("arguments[0].scrollIntoView(true);", element);
    }

    /**
     * type text into a field
     */
    public void enterText(String type, String locator, String param1) {
		WebElement element = findElement(type, locator);
		for (char c : param1.toCharArray()) {
			element.sendKeys(String.valueOf(c));
		} element.sendKeys(Keys.TAB);
    }

    /**
     * Open Webpage as given url
     */
    public void openWebPage(String param1) {
		driver.get(param1);
    }

    /**
     * Return current page url
     */
    public String getCurrentUrl() {
		return driver.getCurrentUrl();
    }

    /**
     * Return current element text
     */
    public String getElementText(String type, String locator) {
		WebElement element = findElement(type, locator);
		return element.getText();
    }

    /**
     * Return current element tag name
     */
    public String getElementTagName(String type, String locator) {
		WebElement element = findElement(type, locator);
		return element.getTagName();
    }

    /**
     * Quit all driver sessions
     */
    public void driverQuit() {
		driver.quit();
    }

    /**
     * Quit current driver session
     */
    public void driverClose() {
		driver.close();
    }

    /**
     * Submit a form
     */
    public void submitForm(String type, String locator) {
		WebElement element = findElement(type, locator);
		element.submit();
    }

    /**
     * Click Action
     */
    public void clickAction(String type, String locator) {
		WebElement element = findElement(type, locator);
		element.click();
    }

    /**
     * Select All Check Box
     */
    public void selectAllCheckBox(String type, String locator) {
		List<WebElement> checkBoxes = findElements(type, locator);
		for (WebElement checkBox  : checkBoxes) {
            if (!checkBox.isSelected()) {
                checkBox.click();
            }
        }
    }

    /**
     * deSelect All Check Box
     */
    public void deSelectAllCheckBox(String type, String locator) {
		List<WebElement> checkBoxes = findElements(type, locator);
		for (WebElement checkBox  : checkBoxes) {
            if (checkBox.isSelected()) {
                checkBox.click();
            }
        }
    }

    /**
     * Verify the visibility of a given text as hyperLink
     */
    public boolean getVerifyHyperLinkPresent(String type, String locator) {
		WebElement linkElement = findElement(type, locator);
		return linkElement.getTagName().equalsIgnoreCase("a") && linkElement.getAttribute("href") != null;
    }

    /**
     * Return the visibility of a given text as partialText
     */
    public boolean getVerifyHyperLinkPresentByPartialText(String type, String locator) {
		WebElement linkElement = findElement(type, locator);
		return linkElement.getTagName().equalsIgnoreCase("a") && linkElement.getAttribute("href") != null;
    }

    /**
     * Return the visibility of given text
     */
    public boolean getVerifyTextPresent(String type, String locator, String param1) {
		WebElement element = findElement(type, locator);
		String actualText = element.getText();
        return actualText.contentEquals(param1) ;
    }

    /**
     * Return the visibility of multiple given texts
     */
    public boolean getVerifyMultipleTextPresent(String type, String locator, String[] param1) {
		WebElement element = findElement(type, locator);
		String actualText = element.getText();
        for (String expectedText : param1) {
           if (actualText.contentEquals(expectedText)) {
               System.out.println("actualText does not match expectedText" ); 
                return false;
            }
        }
        return true;
    }

    /**
     * Return the visibility of given Image
     */
    public boolean getVerifyImagePresent(String type, String locator) {
		WebElement element = findElement(type, locator);
		return !driver.findElements((By) element).isEmpty() ;
    }

    /**
     * Return that the page title matches the expected value.
     */
    public boolean getVerifyPageTitle(String param1) {
		return driver.getTitle().contains(param1);
    }

    /**
     * Select an option based on the given index
     */
    public void selectOptionById(String type, String locator, int param1) {
		WebElement element = findElement(type, locator);
		new Select(element).selectByIndex(param1);
    }

    /**
     * Select all options from a list
     */
    public void selectAllOptionsFromDropDown(String type, String locator) {
		WebElement selectElement = findElement(type, locator);
		Select select = new Select(selectElement);
        List<WebElement> options = select.getOptions();
        for (WebElement option : options) {
            if (!option.isSelected()) {
                option.click();
            }
        }
    }

    /**
     * Select an option based on the given text
     */
    public void selectOptionByTextFromList(String type, String locator, String param1) {
		WebElement element = findElement(type, locator);
		new Select(element).selectByVisibleText(param1);
    }

    /**
     * Select an option based on the given text
     */
    public void selectOptionByValue(String type, String locator, String param1) {
		WebElement element = findElement(type, locator);
		new Select(element).selectByValue(param1);
    }

    /**
     * Unselect an option based on the given text
     */
    public void unSelectOptionByTextFromList(String type, String locator, String param1) {
		WebElement element = findElement(type, locator);
		Select select = new Select(element);
        select.deselectByVisibleText(param1);
    }

    /**
     * Unselect an option based on the given value
     */
    public void unSelectOptionByValue(String type, String locator, String param1) {
		WebElement element = findElement(type, locator);
		Select select = new Select(element);
        select.deselectByValue(param1);
    }

    /**
     * Unselect all selected checkboxes
     */
    public void unSelectAllOptions(String type, String locator) {
		WebElement element = findElement(type, locator);
		Select select = new Select(element);
        select.deselectAll();
    }

    /**
     * Hand over on an element
     */
    public void handOver(String type, String locator) {
		WebElement element = findElement(type, locator);
		Actions actions = new Actions(driver);
            actions.moveToElement(element).perform();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        
}    }

    /**
     * Unselect a checkbox based on the given index
     */
    public void unSelectOptionByIdFromList(String type, String locator, int param1) {
		WebElement element = findElement(type, locator);
		Select select = new Select(element);
        select.deselectByIndex(param1);
    }

    /**
     * Return an option is selected
     */
    public boolean getCheckBoxIsSelected(String type, String locator) {
		WebElement element = findElement(type, locator);
		return element.isSelected();
    }

    /**
     * Return given element is displayed
     */
    public boolean getElementIsDisplayed(String type, String locator) {
		WebElement element = findElement(type, locator);
		return element.isDisplayed();
    }

    /**
     * Return button is enabled
     */
    public boolean getButtonIsEnabled(String type, String locator) {
		WebElement element = findElement(type, locator);
		return element.isEnabled();
    }

    /**
     * Return specific attribute of a given element
     */
    public String getAttribute(String type, String locator, String param1) {
		WebElement element = findElement(type, locator);
		return element.getAttribute(param1);
    }

    /**
     * Clear selected or entered information
     */
    public void clear(String type, String locator) {
		WebElement element = findElement(type, locator);
		element.clear();
    }

    /**
     * Apply an implicit wait
     */
    public void ImplicitWait(int param1) {
		driver.manage().timeouts().implicitlyWait(param1, java.util.concurrent.TimeUnit.SECONDS);
    }

}
