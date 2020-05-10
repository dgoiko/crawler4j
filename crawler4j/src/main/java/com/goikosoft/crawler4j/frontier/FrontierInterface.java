package com.goikosoft.crawler4j.frontier;

import java.util.List;

import com.goikosoft.crawler4j.url.WebURL;

public interface FrontierInterface {

    void scheduleAll(List<WebURL> urls);

    void schedule(WebURL url);

    void getNextURLs(int max, List<WebURL> result);

    void setProcessed(WebURL webURL);

    long getQueueLength();

    long getNumberOfAssignedPages();

    long getNumberOfProcessedPages();

    long getNumberOfScheduledPages();

    boolean isFinished();

    void close();

    void finish();

}