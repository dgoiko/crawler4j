package com.goikosoft.crawler4j.parser;

import com.goikosoft.crawler4j.crawler.Page;
import com.goikosoft.crawler4j.crawler.exceptions.ParseException;

/**
 * Custom interface for all parsers.
 * @author Dario Goikoetxea
 *
 */
public interface ParserInterface {

    void parse(Page page, String contextURL) throws NotAllowedContentException, ParseException;

}