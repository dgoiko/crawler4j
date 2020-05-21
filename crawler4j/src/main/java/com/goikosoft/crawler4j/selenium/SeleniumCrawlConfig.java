package com.goikosoft.crawler4j.selenium;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.firefox.FirefoxOptions;

import com.goikosoft.crawler4j.crawler.CrawlConfig;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Settings.Builder;

public class SeleniumCrawlConfig extends CrawlConfig {

    public static final String GECKO_PROPERTY = "webdriver.gecko.driver";
    public static final String FIREFOX_BIN_PROPERTY = "webdriver.firefox.bin";

    private SeleniumDrivers driver = SeleniumDrivers.JBROWSER;

    /**
     * Gecko driver path. In the future, will hold the path of chrome drivers and others.
     */
    private String driverPath = null;

    /**
     * Path to the browser. If null, system will assume Selenium's default (which is
     * normaly the estandard browser executable name, located under PATH
     */
    private String browserPath = null;

    /**
     * If true, selenium will be used when an URL does not match inclussion / exclussion patterns
     */
    private boolean defaultToSelenium = false;

    /**
     * List of regular expressions. If a URL matches any of them, it will never be processed by selenium
     */
    private List<String> seleniumExcludes;

    /**
     * List of regular expressions. If a URL matches any of them and doesn't match any exclussion,
     * it will be processed with Selenium
     */
    private List<String> seleniumIncludes;

    /**
     * Store and load cookies with selenium. Share cookies between selenium and non-selenium.
     *
     * Currently ONLY WORKS WITH JBROWSER
     */
    private boolean cookiesSelemiun;

    /**
     *  Confguration for Selenium. Will override any config if exists. Will be created from this options if not
     */
    private Settings seleniumConfig;

    /**
     *  Confguration for Selenium's firefox web driver. Will override any config if exists.
     *  Will be created from this options if not
     */
    private FirefoxOptions seleniumFirefoxConfig;

    @Override
    public SeleniumCrawlConfig clone() {
        // TODO Auto-generated method stub
        SeleniumCrawlConfig temp = (SeleniumCrawlConfig) super.clone();
        temp.cookiesSelemiun = cookiesSelemiun;
        temp.defaultToSelenium = defaultToSelenium;
        if (seleniumExcludes != null) {
            temp.seleniumExcludes = new ArrayList<String>();
            temp.seleniumExcludes.addAll(seleniumExcludes);
        }
        if (seleniumIncludes != null) {
            temp.seleniumIncludes = new ArrayList<String>();
            temp.seleniumIncludes.addAll(seleniumIncludes);
        }
        return temp;
    }

    protected SeleniumCrawlConfig createInstance() {
        return new SeleniumCrawlConfig();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Crawl storage folder: " + getCrawlStorageFolder() + "\n");
        sb.append("Resumable crawling: " + isResumableCrawling() + "\n");
        sb.append("Max depth of crawl: " + getMaxDepthOfCrawling() + "\n");
        sb.append("Max pages to fetch: " + getMaxPagesToFetch() + "\n");
        sb.append("User agent string: " + getUserAgentString() + "\n");
        sb.append("Include https pages: " + isIncludeHttpsPages() + "\n");
        sb.append("Include binary content: " + isIncludeBinaryContentInCrawling() + "\n");
        sb.append("Max connections per host: " + getMaxConnectionsPerHost() + "\n");
        sb.append("Max total connections: " + getMaxTotalConnections() + "\n");
        sb.append("Socket timeout: " + getSocketTimeout() + "\n");
        sb.append("Max total connections: " + getMaxTotalConnections() + "\n");
        sb.append("Max outgoing links to follow: " + getMaxOutgoingLinksToFollow() + "\n");
        sb.append("Max download size: " + getMaxDownloadSize() + "\n");
        sb.append("Should follow redirects?: " + isFollowRedirects() + "\n");
        sb.append("Proxy host: " + getProxyHost() + "\n");
        sb.append("Proxy port: " + getProxyPort() + "\n");
        sb.append("Proxy username: " + getProxyUsername() + "\n");
        sb.append("Thread monitoring delay: " + getThreadMonitoringDelaySeconds() + "\n");
        sb.append("Thread shutdown delay: " + getThreadShutdownDelaySeconds() + "\n");
        sb.append("Cleanup delay: " + getCleanupDelaySeconds() + "\n");
        sb.append("Cookie policy: " + getCookiePolicy() + "\n");
        sb.append("Respect nofollow: " + isRespectNoFollow() + "\n");
        sb.append("Respect noindex: " + isRespectNoIndex() + "\n");
        sb.append("Halt on error: " + isHaltOnError() + "\n");
        sb.append("Allow single level domain:" + isAllowSingleLevelDomain() + "\n");
        sb.append("Batch read size: " + getBatchReadSize() + "\n");
        sb.append("Default to selenium: " + isDefaultToSelenium() + "\n");
        sb.append("Cookies to selenium: " + isCookiesSelemiun() + "\n");
        sb.append("Selenium inclussion patterns: " + getSeleniumIncludes() + "\n");
        sb.append("Selenium exclussion patterns: " + getSeleniumExcludes() + "\n");
        return sb.toString();
    }

    public boolean isDefaultToSelenium() {
        return defaultToSelenium;
    }

    public void setDefaultToSelenium(boolean defaultToSelenium) {
        this.defaultToSelenium = defaultToSelenium;
    }

    public List<String> getSeleniumExcludes() {
        return seleniumExcludes;
    }

    public void setSeleniumExcludes(List<String> seleniumExcludes) {
        this.seleniumExcludes = seleniumExcludes;
    }

    public List<String> getSeleniumIncludes() {
        return seleniumIncludes;
    }

    public void setSeleniumIncludes(List<String> seleniumIncludes) {
        this.seleniumIncludes = seleniumIncludes;
    }

    public boolean isCookiesSelemiun() {
        return cookiesSelemiun;
    }

    public void setCookiesSelemiun(boolean cookiesSelemiun) {
        this.cookiesSelemiun = cookiesSelemiun;
    }

    public SeleniumDrivers getDriver() {
        return driver;
    }

    public void setDriver(SeleniumDrivers driver) {
        this.driver = driver;
    }

    public Settings getSeleniumConfig() {
        if (seleniumConfig == null) {
            Builder options = new Settings.Builder();
            options.headless(true);
            options.javascript(true);
            // Approximation.
            options.maxConnections(this.getMaxConnectionsPerHost());
            /*
            if(this.getDefaultHeaders() != null && !this.getDefaultHeaders().isEmpty()) {
                RequestHeaders headers = new RequestHeaders();
                options.requestHeaders();

            }*/
            options.socketTimeout(this.getSocketTimeout());
            //options.userAgent(this.getUserAgentString());
            options.ssl("trustanything");
            return options.build();
        }
        return seleniumConfig;
    }

    public void setSeleniumConfig(Settings seleniumConfig) {
        this.seleniumConfig = seleniumConfig;
    }

    public FirefoxOptions getSeleniumFirefoxConfig() {
        if (seleniumFirefoxConfig == null) {
            FirefoxOptions options = new FirefoxOptions();
            options.setHeadless(true);
            return options;
        }
        return seleniumFirefoxConfig;
    }

    public void setSeleniumFirefoxConfig(FirefoxOptions seleniumFirefoxConfig) {
        this.seleniumFirefoxConfig = seleniumFirefoxConfig;
    }

    public String getDriverPath() {
        return driverPath;
    }

    public void setDriverPath(String driverPath) {
        this.driverPath = driverPath;
    }

    public String getBrowserPath() {
        return browserPath;
    }

    public void setBrowserPath(String browserPath) {
        this.browserPath = browserPath;
    }
}
