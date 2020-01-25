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

package edu.uci.ics.crawler4j.crawler;


import edu.uci.ics.crawler4j.fetcher.PageFetcherInterface;
import edu.uci.ics.crawler4j.parser.Parser;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.TLDList;

/**
 * The controller that manages a crawling session. This class creates the
 * crawler threads and monitors their progress.
 *
 * @author Yasser Ganjisaffar
 */
public class CrawlController {

    static final Logger logger = LoggerFactory.getLogger(CrawlController.class);
    private final CrawlConfig config;

    /**
     * The 'customData' object can be used for passing custom crawl-related
     * configurations to different components of the crawler.
     */
    protected Object customData;

    /**
     * Once the crawling session finishes the controller collects the local data
     * of the crawler threads and stores them in this List.
     */
    protected List<Object> crawlersLocalData = new ArrayList<>();

    /**
     * Is the crawling of this session finished?
     */
    protected boolean finished;
    private Throwable error;

    /**
     * Is the crawling session set to 'shutdown'. Crawler threads monitor this
     * flag and when it is set they will no longer process new pages.
     */
    protected boolean shuttingDown;

    protected PageFetcher pageFetcher;
    protected RobotstxtServer robotstxtServer;
    protected Frontier frontier;
    protected DocIDServer docIdServer;
    protected TLDList tldList;

    protected final Object waitingLock = new Object();
    protected final Environment env;

    protected Parser parser;

    public CrawlController(CrawlConfig config, PageFetcher pageFetcher,
                           RobotstxtServer robotstxtServer) throws Exception {
        this(config, pageFetcher, null, robotstxtServer, null);
    }

    public CrawlController(CrawlConfig config, PageFetcher pageFetcher,
            RobotstxtServer robotstxtServer, TLDList tldList) throws Exception {
        this(config, pageFetcher, null, robotstxtServer, tldList);
    }

    public CrawlController(CrawlConfig config, PageFetcher pageFetcher, Parser parser,
                           RobotstxtServer robotstxtServer, TLDList tldList) throws Exception {
        config.validate();
        this.config = config;

        File folder = new File(config.getCrawlStorageFolder());
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                logger.debug("Created folder: " + folder.getAbsolutePath());
            } else {
                throw new Exception(
                    "couldn't create the storage folder: " + folder.getAbsolutePath() +
                    " does it already exist ?");
            }
        }

        this.tldList = tldList == null ? new TLDList(config) : tldList;
        URLCanonicalizer.setHaltOnError(config.isHaltOnError());

        boolean resumable = config.isResumableCrawling();

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(resumable);
        envConfig.setLocking(resumable);
        envConfig.setLockTimeout(config.getDbLockTimeout(), TimeUnit.MILLISECONDS);

        File envHome = new File(config.getCrawlStorageFolder() + "/frontier");
        if (!envHome.exists()) {
            if (envHome.mkdir()) {
                logger.debug("Created folder: " + envHome.getAbsolutePath());
            } else {
                throw new Exception(
                    "Failed creating the frontier folder: " + envHome.getAbsolutePath());
            }
        }

        if (!resumable) {
            IO.deleteFolderContents(envHome);
            logger.info("Deleted contents of: " + envHome +
                        " ( as you have configured resumable crawling to false )");
        }

        env = new Environment(envHome, envConfig);
        docIdServer = new DocIDServer(env, config);
        frontier = createFrontier(config);

        this.pageFetcher = pageFetcher;
        this.parser = parser == null ? new Parser(config, tldList) : parser;
        this.robotstxtServer = robotstxtServer;

        finished = false;
        shuttingDown = false;

        robotstxtServer.setCrawlConfig(config);
    }

    /**
     * Creates the Frontier for this instance. Subclasses can create custom Frontiers
     * @param config configuration procided to the CrawlController
     * @return
     */
    protected Frontier createFrontier(CrawlConfig config) {
        return new Frontier(env, config);
    }

    public Parser getParser() {
        return parser;
    }

    public interface WebCrawlerFactory<T extends WebCrawler> {
        T newInstance() throws Exception;
    }

    private static class SingleInstanceFactory<T extends WebCrawler>
        implements WebCrawlerFactory<T> {

        final T instance;

        SingleInstanceFactory(T instance) {
            this.instance = instance;
        }

        @Override
        public T newInstance() throws Exception {
            return this.instance;
        }
    }

    private static class DefaultWebCrawlerFactory<T extends WebCrawler>
        implements WebCrawlerFactory<T> {
        final Class<T> clazz;

        DefaultWebCrawlerFactory(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T newInstance() throws Exception {
            try {
                return clazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw e;
            }
        }
    }

    /**
     * Start the crawling session and wait for it to finish.
     * This method utilizes default crawler factory that creates new crawler using Java reflection
     *
     * @param clazz
     *            the class that implements the logic for crawler threads
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(Class<T> clazz, int numberOfCrawlers) {
        this.start(new DefaultWebCrawlerFactory<>(clazz), numberOfCrawlers, true);
    }

    /**
     * Start the crawling session and wait for it to finish.
     * This method depends on a single instance of a crawler. Only that instance will be used for crawling.
     *
     * @param instance
     *            the instance of a class that implements the logic for crawler threads
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(T instance) {
        this.start(new SingleInstanceFactory<>(instance), 1, true);
    }

    /**
     * Start the crawling session and wait for it to finish.
     *
     * @param crawlerFactory
     *            factory to create crawlers on demand for each thread
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(WebCrawlerFactory<T> crawlerFactory,
                                             int numberOfCrawlers) {
        this.start(crawlerFactory, numberOfCrawlers, true);
    }

    /**
     * Start the crawling session and return immediately.
     *
     * @param crawlerFactory
     *            factory to create crawlers on demand for each thread
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void startNonBlocking(WebCrawlerFactory<T> crawlerFactory,
                                                        final int numberOfCrawlers) {
        this.start(crawlerFactory, numberOfCrawlers, false);
    }

    /**
     * Start the crawling session and return immediately.
     * This method utilizes default crawler factory that creates new crawler using Java reflection
     *
     * @param clazz
     *            the class that implements the logic for crawler threads
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void startNonBlocking(Class<T> clazz, int numberOfCrawlers) {
        start(new DefaultWebCrawlerFactory<>(clazz), numberOfCrawlers, false);
    }

    protected <T extends WebCrawler> void start(final WebCrawlerFactory<T> crawlerFactory,
                                                final int numberOfCrawlers, boolean isBlocking) {
        try {
            finished = false;
            setError(null);
            crawlersLocalData.clear();
            final List<Thread> threads = new ArrayList<>();
            final List<T> crawlers = new ArrayList<>();

public class CrawlController extends GenericCrawlController<WebCrawler> {

    public CrawlController(CrawlConfig config, PageFetcherInterface pageFetcher, Parser parser,
            RobotstxtServer robotstxtServer, TLDList tldList, String docIdDbName, String pendingDbName,
            String inProcessDbName) throws Exception {
        super(config, pageFetcher, parser, robotstxtServer, tldList, docIdDbName, pendingDbName, inProcessDbName);
    }

    public CrawlController(CrawlConfig config, PageFetcherInterface pageFetcher, Parser parser,
            RobotstxtServer robotstxtServer, TLDList tldList, String docIdDbName, String pendingDbName)
            throws Exception {
        super(config, pageFetcher, parser, robotstxtServer, tldList, docIdDbName, pendingDbName);
    }

    /**
     * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
     * to extract new URLs in it and follow them for crawling.
     *
     * @param pageUrl
     *            the URL of the seed
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void addSeed(String pageUrl) throws IOException, InterruptedException {
        addSeed(pageUrl, -1);
    }

    /**
     * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
     * to extract new URLs in it and follow them for crawling. You can also
     * specify a specific document id to be assigned to this seed URL. This
     * document id needs to be unique. Also, note that if you add three seeds
     * with document ids 1,2, and 7. Then the next URL that is found during the
     * crawl will get a doc id of 8. Also you need to ensure to add seeds in
     * increasing order of document ids.
     *
     * Specifying doc ids is mainly useful when you have had a previous crawl
     * and have stored the results and want to start a new crawl with seeds
     * which get the same document ids as the previous crawl.
     *
     * @param pageUrl
     *            the URL of the seed
     * @param docId
     *            the document id that you want to be assigned to this seed URL.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    public void addSeed(String pageUrl, int docId) throws IOException, InterruptedException {
        String canonicalUrl = URLCanonicalizer.getCanonicalURL(pageUrl);
        if (canonicalUrl == null) {
            logger.error("Invalid seed URL: {}", pageUrl);
        } else {
            if (docId < 0) {
                docId = docIdServer.getDocId(canonicalUrl);
                if (docId > 0) {
                    logger.trace("This URL is already seen.");
                    return;
                }
                docId = docIdServer.getNewDocID(canonicalUrl);
            } else {
                try {
                    docIdServer.addUrlAndDocId(canonicalUrl, docId);
                } catch (RuntimeException e) {
                    if (config.isHaltOnError()) {
                        throw e;
                    } else {
                        logger.error("Could not add seed: {}", e.getMessage());
                    }
                }
            }

            WebURL webUrl = createEmptyWebURL(pageUrl);
            webUrl.setTldList(tldList);
            webUrl.setURL(canonicalUrl);
            webUrl.setDocid(docId);
            webUrl.setDepth((short) 0);
            if (robotstxtServer.allows(webUrl)) {
                frontier.schedule(webUrl);
            } else {
                // using the WARN level here, as the user specifically asked to add this seed
                logger.warn("Robots.txt does not allow this seed: {}", pageUrl);
            }
        }
    }

    /**
     * Creates an empty WebURL. Subclases can override this to create subclases of WebURL instead.
     * @param nonCanonicalString url before being transformed into canonical. It is ignored in default implementation
     * @return
     */
    protected WebURL createEmptyWebURL(String nonCanonicalString) {
        return new WebURL();
    }

    /**
     * This function can called to assign a specific document id to a url. This
     * feature is useful when you have had a previous crawl and have stored the
     * Urls and their associated document ids and want to have a new crawl which
     * is aware of the previously seen Urls and won't re-crawl them.
     *
     * Note that if you add three seen Urls with document ids 1,2, and 7. Then
     * the next URL that is found during the crawl will get a doc id of 8. Also
     * you need to ensure to add seen Urls in increasing order of document ids.
     *
     * @param url
     *            the URL of the page
     * @param docId
     *            the document id that you want to be assigned to this URL.
     * @throws UnsupportedEncodingException
     *
     */
    public void addSeenUrl(String url, int docId) throws UnsupportedEncodingException {
        String canonicalUrl = URLCanonicalizer.getCanonicalURL(url);
        if (canonicalUrl == null) {
            logger.error("Invalid Url: {} (can't cannonicalize it!)", url);
        } else {
            try {
                docIdServer.addUrlAndDocId(canonicalUrl, docId);
            } catch (RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    logger.error("Could not add seen url: {}", e.getMessage());
                }
            }
        }
    }

    public PageFetcher getPageFetcher() {
        return pageFetcher;
    }

    public void setPageFetcher(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }

    public RobotstxtServer getRobotstxtServer() {
        return robotstxtServer;
    }

    public void setRobotstxtServer(RobotstxtServer robotstxtServer) {
        this.robotstxtServer = robotstxtServer;
    }

    public Frontier getFrontier() {
        return frontier;
    }

    public void setFrontier(Frontier frontier) {
        this.frontier = frontier;
    }

    public DocIDServer getDocIdServer() {
        return docIdServer;
    }

    public void setDocIdServer(DocIDServer docIdServer) {
        this.docIdServer = docIdServer;
    }

    /**
     * @deprecated implements a factory {@link WebCrawlerFactory} and inject your cutom data as
     * shown <a href="https://github.com/yasserg/crawler4j#using-a-factory">here</a> .
     */
    @Deprecated
    public Object getCustomData() {
        return customData;
    }

    /**
     * @deprecated implements a factory {@link WebCrawlerFactory} and inject your cutom data as
     * shown <a href="https://github.com/yasserg/crawler4j#using-a-factory">here</a> .
     */

    @Deprecated
    public void setCustomData(Object customData) {
        this.customData = customData;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Set the current crawling session set to 'shutdown'. Crawler threads
     * monitor the shutdown flag and when it is set to true, they will no longer
     * process new pages.
     */
    public void shutdown() {
        logger.info("Shutting down...");
        this.shuttingDown = true;
        pageFetcher.shutDown();
        frontier.finish();
    }

    public CrawlController(CrawlConfig config, PageFetcherInterface pageFetcher, RobotstxtServer robotstxtServer,
            TLDList tldList) throws Exception {
        super(config, pageFetcher, robotstxtServer, tldList);
    }

    public CrawlController(CrawlConfig config, PageFetcherInterface pageFetcher, RobotstxtServer robotstxtServer)
            throws Exception {
        super(config, pageFetcher, robotstxtServer);
    }


}
