package com.priortest.generator.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;
public class WebDriverSingleton {

    private static WebDriver driver;
    public enum BrowserType {
        CHROME,
        FIREFOX,
        SAFARI,
        EDGE
    }

    public static void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
    public static WebDriver getDriver(BrowserType browserType) {
        if (driver == null) {
            switch (browserType) {
                case CHROME:
                    //System.setProperty("webdriver.chrome.driver", "path/to/chromedriver"); // Set path to ChromeDriver
                    // Ensure ChromeDriver is resolved by Selenium Manager or set the path explicitly
                    driver = new ChromeDriver();
                    break;
                case FIREFOX:
                    //System.setProperty("webdriver.gecko.driver", "path/to/geckodriver"); // Set path to GeckoDriver
                    // Ensure GeckoDriver is resolved by Selenium Manager or set the path explicitly
                    driver = new FirefoxDriver();
                    break;
                case SAFARI:
                    // SafariDriver works only on macOS and requires Safari's 'Remote Automation' enabled
                    driver = new SafariDriver();
                    break;
                case EDGE:
                    // EdgeDriver requires Selenium Manager or explicit msedgedriver path
                    driver = new EdgeDriver();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported browser type: " + browserType);
            }
        }
        return driver;
    }


}
