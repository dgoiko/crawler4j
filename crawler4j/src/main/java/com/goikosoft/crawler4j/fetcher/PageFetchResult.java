/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.goikosoft.crawler4j.fetcher;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.goikosoft.crawler4j.crawler.Page;
import com.goikosoft.crawler4j.url.WebURL;

/**
 * Modified by Dario Goikoetxea to represent the URL as WebURL
 * @author Yasser Ganjisaffar
 */
public class PageFetchResult implements PageFetchResultInterface {

    protected static final Logger logger = LoggerFactory.getLogger(PageFetchResult.class);

    private boolean haltOnError;
    protected int statusCode;
    protected HttpEntity entity = null;
    protected Header[] responseHeaders = null;
    protected String fetchedUrl = null;
    protected WebURL fetchedWebUrl = null;
    protected String movedToUrl = null;

    public PageFetchResult(boolean haltOnError) {
        this.haltOnError = haltOnError;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public HttpEntity getEntity() {
        return entity;
    }

    public void setEntity(HttpEntity entity) {
        this.entity = entity;
    }

    public Header[] getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Header[] responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    @Deprecated
    public String getFetchedUrl() {
        return fetchedUrl;
    }

    @Deprecated
    public void setFetchedUrl(String fetchedUrl) {
        WebURL fetchedWebURL = new WebURL();
        fetchedWebURL.setURL(fetchedUrl);
        setFetchedWebUrl(fetchedWebURL);
    }

    public WebURL getFetchedWebUrl() {
        return fetchedWebUrl;
    }

    public void setFetchedWebUrl(WebURL fetchedWebUrl) {
        this.fetchedWebUrl = fetchedWebUrl;
        // Compatibility until deprecated methods are deleted
        if (fetchedWebUrl != null) {
            this.fetchedUrl = fetchedWebUrl.getURL();
        } else {
            this.fetchedUrl = null;
        }
    }

    public boolean fetchContent(Page page, int maxBytes) throws SocketTimeoutException, IOException {
        try {
            page.setFetchResponseHeaders(responseHeaders);
            page.load(entity, maxBytes);
            return true;
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            if (haltOnError) {
                throw e;
            } else {
                logger.info("Exception while fetching content for: {} [{}]", page.getWebURL().getURL(),
                            e.getMessage());
            }
        }
        return false;
    }

    public void discardContentIfNotConsumed() {
        try {
            if (entity != null) {
                EntityUtils.consume(entity);
            }
        } catch (IOException ignored) {
            // We can EOFException (extends IOException) exception. It can happen on compressed
            // streams which are not
            // repeatable
            // We can ignore this exception. It can happen if the stream is closed.
        } catch (RuntimeException e) {
            if (haltOnError) {
                throw e;
            } else {
                logger.warn("Unexpected error occurred while trying to discard content", e);
            }
        }
    }

    public String getMovedToUrl() {
        return movedToUrl;
    }

    public void setMovedToUrl(String movedToUrl) {
        this.movedToUrl = movedToUrl;
    }
}
