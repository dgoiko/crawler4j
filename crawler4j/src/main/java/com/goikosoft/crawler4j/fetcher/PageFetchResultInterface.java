package com.goikosoft.crawler4j.fetcher;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import com.goikosoft.crawler4j.crawler.Page;
import com.goikosoft.crawler4j.url.WebURL;

public interface PageFetchResultInterface {

    int getStatusCode();

    void setStatusCode(int statusCode);

    WebURL getFetchedWebUrl();

    void setFetchedWebUrl(WebURL fetchedWebUrl);

    @Deprecated
    String getFetchedUrl();

    @Deprecated
    void setFetchedUrl(String fetchedUrl);

    boolean fetchContent(Page page, int maxBytes) throws SocketTimeoutException, IOException;

    void discardContentIfNotConsumed();

    String getMovedToUrl();

    void setMovedToUrl(String movedToUrl);

    HttpEntity getEntity();

    Header[] getResponseHeaders();

}