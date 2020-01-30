package com.goikosoft.crawler4j.fetcher;

import java.io.IOException;

import com.goikosoft.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import com.goikosoft.crawler4j.url.WebURL;

/**
 * Common interface for all custom PageFetchers
 * @author Dario Goikoetxea
 *
 */
public interface PageFetcherInterface {

    PageFetchResult fetchPage(WebURL webUrl) throws InterruptedException, IOException,
                                                    PageBiggerThanMaxSizeException;

    void shutDown();

}