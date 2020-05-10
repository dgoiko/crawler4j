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

package com.goikosoft.crawler4j.frontier;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.goikosoft.crawler4j.crawler.CrawlConfig;
import com.goikosoft.crawler4j.url.WebURL;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;

/**
 * Modified by Dario Goikoetxea to make it easier to create subclases and use custom DB names
 * @author Yasser Ganjisaffar
 */

public class Frontier implements FrontierInterface {
    protected static final Logger logger = LoggerFactory.getLogger(Frontier.class);

    private static final String DATABASE_NAME = "PendingURLsDB";
    private static final int IN_PROCESS_RESCHEDULE_BATCH_SIZE = 100;
    private final CrawlConfig config;
    protected WorkQueues workQueues;

    protected InProcessPagesDB inProcessPages;

    protected final Object mutex = new Object();
    protected final Object waitingList = new Object();

    protected boolean isFinished = false;

    protected long scheduledPages;

    protected Counters counters;

    public Frontier(Environment env, CrawlConfig config) {
        this(env, config, null);
    }

    public Frontier(Environment env, CrawlConfig config, String dbName) {
        this(env, config, dbName, null);
    }

    public Frontier(Environment env, CrawlConfig config, String dbName, String inProcessDbName) {
        this.config = config;
        this.counters = new Counters(env, config);
        try {
            if (dbName == null) {
                workQueues = createWorkQueues(env, config, DATABASE_NAME);
            } else {
                workQueues = createWorkQueues(env, config, dbName);
            }
            if (config.isResumableCrawling()) {
                scheduledPages = counters.getValue(Counters.ReservedCounterNames.SCHEDULED_PAGES);
                if (inProcessDbName == null) {
                    inProcessPages = new InProcessPagesDB(env);
                } else {
                    inProcessPages = new InProcessPagesDB(env, inProcessDbName);
                }
                long numPreviouslyInProcessPages = inProcessPages.getLength();
                if (numPreviouslyInProcessPages > 0) {
                    logger.info("Rescheduling {} URLs from previous crawl.",
                                numPreviouslyInProcessPages);
                    scheduledPages -= numPreviouslyInProcessPages;

                    List<WebURL> urls = inProcessPages.get(IN_PROCESS_RESCHEDULE_BATCH_SIZE);
                    while (!urls.isEmpty()) {
                        scheduleAll(urls);
                        inProcessPages.delete(urls.size());
                        urls = inProcessPages.get(IN_PROCESS_RESCHEDULE_BATCH_SIZE);
                    }
                }
            } else {
                inProcessPages = null;
                scheduledPages = 0;
            }
        } catch (DatabaseException e) {
            logger.error("Error while initializing the Frontier", e);
            workQueues = null;
        }
    }

    @Override
    public void scheduleAll(List<WebURL> urls) {
        int maxPagesToFetch = config.getMaxPagesToFetch();
        synchronized (mutex) {
            int newScheduledPage = 0;
            for (WebURL url : urls) {
                if ((maxPagesToFetch > 0) &&
                    ((scheduledPages + newScheduledPage) >= maxPagesToFetch)) {
                    break;
                }

                try {
                    workQueues.put(url);
                    newScheduledPage++;
                } catch (DatabaseException e) {
                    logger.error("Error while putting the url in the work queue", e);
                }
            }
            if (newScheduledPage > 0) {
                scheduledPages += newScheduledPage;
                counters.increment(Counters.ReservedCounterNames.SCHEDULED_PAGES, newScheduledPage);
            }
            synchronized (waitingList) {
                waitingList.notifyAll();
            }
        }
    }

    @Override
    public void schedule(WebURL url) {
        int maxPagesToFetch = config.getMaxPagesToFetch();
        synchronized (mutex) {
            try {
                if (maxPagesToFetch < 0 || scheduledPages < maxPagesToFetch) {
                    workQueues.put(url);
                    scheduledPages++;
                    counters.increment(Counters.ReservedCounterNames.SCHEDULED_PAGES);
                }
            } catch (DatabaseException e) {
                logger.error("Error while putting the url in the work queue", e);
            }
        }
    }

    @Override
    public void getNextURLs(int max, List<WebURL> result) {
        while (true) {
            synchronized (mutex) {
                if (isFinished) {
                    return;
                }
                try {
                    List<WebURL> curResults = workQueues.get(max);
                    workQueues.delete(curResults.size());
                    if (inProcessPages != null) {
                        for (WebURL curPage : curResults) {
                            inProcessPages.put(curPage);
                        }
                    }
                    result.addAll(curResults);
                } catch (DatabaseException e) {
                    logger.error("Error while getting next urls", e);
                }

                if (result.size() > 0) {
                    return;
                }
            }

            try {
                synchronized (waitingList) {
                    waitingList.wait();
                }
            } catch (InterruptedException ignored) {
                // Do nothing
            }
            if (isFinished) {
                return;
            }
        }
    }

    @Override
    public void setProcessed(WebURL webURL) {
        counters.increment(Counters.ReservedCounterNames.PROCESSED_PAGES);
        if (inProcessPages != null) {
            if (!inProcessPages.removeURL(webURL)) {
                logger.warn("Could not remove: {} from list of processed pages.", webURL.getURL());
            }
        }
    }

    @Override
    public long getQueueLength() {
        return workQueues.getLength();
    }

    @Override
    public long getNumberOfAssignedPages() {
        if (inProcessPages != null) {
            return inProcessPages.getLength();
        } else {
            return 0;
        }
    }

    @Override
    public long getNumberOfProcessedPages() {
        return counters.getValue(Counters.ReservedCounterNames.PROCESSED_PAGES);
    }

    @Override
    public long getNumberOfScheduledPages() {
        return counters.getValue(Counters.ReservedCounterNames.SCHEDULED_PAGES);
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void close() {
        workQueues.close();
        counters.close();
        if (inProcessPages != null) {
            inProcessPages.close();
        }
    }

    @Override
    public void finish() {
        isFinished = true;
        synchronized (waitingList) {
            waitingList.notifyAll();
        }
    }

    /**
     * Creates the WorkQueues for this frontier. Can be overriden to create
     * subclases of WorkQueues instead
     * @return new instance of WorkQueues
     * @see Environment#openDatabase
     */
    protected WorkQueues createWorkQueues(Environment env, CrawlConfig config, String databaseName) {
        return new WorkQueues(env, databaseName, config.isResumableCrawling());
    }
}
