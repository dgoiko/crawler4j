package com.goikosoft.crawler4j.tests.crawler;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class TestSelenium {

    public static void main(String[] args) throws InterruptedException {
        // declaration and instantiation of objects/variables
        WebDriver driver;

        System.setProperty("webdriver.gecko.driver", "C:\\geckodriver.exe");
        //System.setProperty("webdriver.chrome.driver","G:\\chromedriver.exe");
        FirefoxOptions options = new FirefoxOptions();
        options.setHeadless(true);
        driver = new FirefoxDriver(options);

        //driver = new JBrowserDriver();
        //WebDriver driver = new ChromeDriver();

        //String baseUrl = "https://www.hipercor.es/supermercado/drogueria-y-limpieza";
        String baseUrl = "https://www.borm.es/";
        // launch Fire fox and direct it to the Base URL
        driver.get(baseUrl);

        //new WebDriverWait(driver, 3000).until(webDriver ->
        //      ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        //Thread.sleep(3000);
        String contents = driver.getPageSource();
        System.out.println(contents);

        //close Fire fox
        //driver.close();
        driver.quit();

    }
}
