package com.goikosoft.crawler4j.frontier;

import com.goikosoft.crawler4j.url.WebURL;

public interface DocIDServerInterface {

    /**
     * Returns the docid of an already seen url.
     *
     * @param url the URL for which the docid is returned.
     * @return the docid of the url if it is seen before. Otherwise -1 is returned.
     */
    int getDocId(WebURL url);

    @Deprecated
    int getDocId(String url);

    int getNewDocID(WebURL url);

    @Deprecated
    int getNewDocID(String url);

    void addUrlAndDocId(WebURL url);

    @Deprecated
    void addUrlAndDocId(String url, int docId);

    boolean isSeenBefore(WebURL url);

    @Deprecated
    boolean isSeenBefore(String url);

    int getDocCount();

    void close();

}