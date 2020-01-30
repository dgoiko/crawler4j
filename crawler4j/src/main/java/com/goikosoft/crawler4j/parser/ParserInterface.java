package com.goikosoft.crawler4j.parser;

import com.goikosoft.crawler4j.crawler.Page;
import com.goikosoft.crawler4j.crawler.exceptions.ParseException;

public interface ParserInterface {

    void parse(Page page, String contextURL) throws NotAllowedContentException, ParseException;

}