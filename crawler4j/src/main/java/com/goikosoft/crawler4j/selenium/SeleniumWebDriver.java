package com.goikosoft.crawler4j.selenium;

import java.util.List;
import java.util.Set;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.goikosoft.crawler4j.url.WebURL;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;

/**
 * Very basic Selenium WebDriver implementation. Must improve it for customization
 * @author Dario
 *
 */
public class SeleniumWebDriver implements WebDriver {

    private WebDriver driver;

    protected final CookieStore cookieStore;

    private final SeleniumDrivers driverType;

    public SeleniumWebDriver(SeleniumCrawlConfig config, CookieStore cookieStore) {
        super();
        this.cookieStore = cookieStore;
        if (config.getDriver() == null) {
            driverType = SeleniumDrivers.JBROWSER;
        } else {
            driverType = config.getDriver();
        }
        switch (driverType) {
            case FIREFOX:
                this.driver = new FirefoxDriver(config.getSeleniumFirefoxConfig());
                break;
            case JBROWSER:
                this.driver = new JBrowserDriver(config.getSeleniumConfig());
                break;
        }
    }

    public SeleniumWebDriver(SeleniumCrawlConfig config) {
        this(config, null);
    }

    public int getStatusCode() {
        if (driver instanceof JBrowserDriver) {
            return ((JBrowserDriver) driver).getStatusCode();
        }
        try {
            driver.getPageSource();
            // Best guess.
            return 200;
        } catch (RuntimeException e) {
            return 499;
        }
    }

    public void get(WebURL url) {
        if (cookieStore != null) {
            importCookies(url.getDomain());
        }
        driver.get(url.getURL());
    }

    @Override
    public void get(String url) {
        if (cookieStore != null) {
            int domainStartIdx = url.indexOf("//") + 2;
            int domainEndIdx = url.indexOf('/', domainStartIdx);
            domainEndIdx = (domainEndIdx > domainStartIdx) ? domainEndIdx : url.length();
            String domain = url.substring(domainStartIdx, domainEndIdx);
            importCookies(domain);
        }
        driver.get(url);
    }

    @Override
    public void quit() {
        if (cookieStore != null) {
            for (Cookie cookie : manage().getCookies()) {
                BasicClientCookie newCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
                newCookie.setDomain(cookie.getDomain());
                newCookie.setPath(cookie.getPath());
                newCookie.setExpiryDate(cookie.getExpiry());
                newCookie.setSecure(cookie.isSecure());
                cookieStore.addCookie(newCookie);
            }
        }
        driver.quit();
    }

    @Override
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }

    @Override
    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    @Override
    public String getPageSource() {
        if (driverType != SeleniumDrivers.JBROWSER) {
            // Other drivers do not guarantee page load. We enforce if here.
            // Please, note that this DOES NOT guarantee that all JS has been executed.
            new WebDriverWait(driver, 3000).until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        }
        return driver.getPageSource();
    }

    @Override
    public void close() {
        driver.close();
    }

    @Override
    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return driver.switchTo();
    }

    @Override
    public Navigation navigate() {
        return driver.navigate();
    }

    @Override
    public Options manage() {
        return driver.manage();
    }

    private void importCookies(String domain) {
        Options options = manage();
        for (org.apache.http.cookie.Cookie cookie : cookieStore.getCookies()) {
            /*
            if (cookie.getDomain().equals(domain)) {
                options.addCookie(new Cookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(),
                                                cookie.getExpiryDate(), cookie.isSecure()));
            }
            */
            options.addCookie(new Cookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(),
                                            cookie.getExpiryDate(), cookie.isSecure()));
        }
    }
}
