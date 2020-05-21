package com.goikosoft.crawler4j.tests.crawler;

import com.goikosoft.crawler4j.crawler.CrawlController;
import com.goikosoft.crawler4j.crawler.WebCrawler;
import com.goikosoft.crawler4j.robotstxt.RobotstxtConfig;
import com.goikosoft.crawler4j.robotstxt.RobotstxtServer;
import com.goikosoft.crawler4j.selenium.PageFetcherSelenium;
import com.goikosoft.crawler4j.selenium.ParserSelenium;
import com.goikosoft.crawler4j.selenium.SeleniumCrawlConfig;
import com.goikosoft.crawler4j.selenium.SeleniumDrivers;
import com.goikosoft.crawler4j.url.TLDList;
import com.goikosoft.crawler4j.url.WebURL;

public class SimpleCrawler {

    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "/data/crawl/root";
        final int numberOfCrawlers = 1;

        SeleniumCrawlConfig config = new SeleniumCrawlConfig();
        config.setThreadMonitoringDelaySeconds(1);
        config.setCleanupDelaySeconds(1);
        config.setThreadShutdownDelaySeconds(1);
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setDefaultToSelenium(true);
        config.setCookiesSelemiun(true);
        config.setUserAgentString("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:76.0) Gecko/20100101 Firefox/76.0");
        config.setIncludeHttpsPages(true);
        config.setDriver(SeleniumDrivers.FIREFOX);
        config.setDriverPath("C:\\geckodriver.exe");
        //config.setBrowserPath("firefox"); // Not needed if firefox is in PATH
        // Instantiate the controller for this crawl.
        TLDList tldList = new TLDList(config);
        ParserSelenium parser = new ParserSelenium(config, tldList);
        PageFetcherSelenium pageFetcher = new PageFetcherSelenium(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, parser, robotstxtServer, tldList);
        robotstxtConfig.setEnabled(false);
        // For each crawl, you need to add some seed urls. These are the first
        // URLs that are fetched and then the crawler starts following links
        // which are found in these pages
        WebURL url = new WebURL();
        url.setURL("https://www.hipercor.es/supermercado/drogueria-y-limpieza");
        url.setSelenium(true);
        controller.addSeed(url);

        // The factory which creates instances of crawlers.
        CrawlController.WebCrawlerFactory<WebCrawler> factory = WebCrawler::new;

        // Start the crawl. This is a blocking operation, meaning that your code
        // will reach the line after this only when crawling is finished.
        controller.start(factory, numberOfCrawlers);
    }
}
