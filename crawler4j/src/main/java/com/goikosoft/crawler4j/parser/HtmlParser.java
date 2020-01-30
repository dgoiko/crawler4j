package com.goikosoft.crawler4j.parser;

import com.goikosoft.crawler4j.crawler.Page;
import com.goikosoft.crawler4j.crawler.exceptions.ParseException;

public interface HtmlParser {

    HtmlParseData parse(Page page, String contextURL) throws ParseException;

}
