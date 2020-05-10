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

import org.apache.http.Header;
import org.apache.http.message.BasicNameValuePair;

import com.goikosoft.crawler4j.url.WebURL;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * Modified by Dario Goikoetxea to include POST capabilities
 * @author Yasser Ganjisaffar
 */
public class WebURLTupleBinding extends TupleBinding<WebURL> {

    @Override
    public WebURL entryToObject(TupleInput input) {
        WebURL webURL = createInstance();
        webURL.setURL(input.readString());
        webURL.setDocid(input.readInt());
        webURL.setParentDocid(input.readInt());
        webURL.setParentUrl(input.readString());
        webURL.setDepth(input.readShort());
        webURL.setPriority(input.readByte());
        webURL.setAnchor(input.readString());
        webURL.setFailedFetches(input.readShort());
        webURL.setFollowRedirectsInmediatly(input.readBoolean());
        webURL.setMaxInmediateRedirects(input.readShort());
        webURL.setPost(input.readBoolean());
        webURL.setSelenium(input.readBoolean());

        short numHeaders = input.readShort();
        for (short i = 0; i < numHeaders; i++) {
            String name = input.readString();
            String value = input.readString();
            webURL.addHeader(name, value);
        }

        if (webURL.isPost()) {
            short numParams = input.readShort();
            for (short i = 0; i < numParams; i++) {
                String name = input.readString();
                String value = input.readString();
                webURL.addPostParameter(name, value);
            }
        }
        return webURL;
    }

    protected WebURL createInstance() {
        return new WebURL();
    }

    @Override
    public void objectToEntry(WebURL url, TupleOutput output) {
        output.writeString(url.getURL());
        output.writeInt(url.getDocid());
        output.writeInt(url.getParentDocid());
        output.writeString(url.getParentUrl());
        output.writeShort(url.getDepth());
        output.writeByte(url.getPriority());
        output.writeString(url.getAnchor());
        output.writeShort(url.getFailedFetches());
        output.writeBoolean(url.isFollowRedirectsInmediatly());
        output.writeShort(url.getMaxInmediateRedirects());
        output.writeBoolean(url.isPost());
        output.writeBoolean(url.isSelenium());

        List<Header> headers = url.getHeaders();
        if (headers != null) {
            output.writeShort(headers.size());
            for (Header header : headers) {
                output.writeString(header.getName());
                output.writeString(header.getValue());
            }
        } else {
            output.writeShort(0);
        }

        if (url.isPost()) {
            if (url.getParamsPost() != null) {
                List<BasicNameValuePair> params = url.getParamsPost().getAsList();
                output.writeShort(params.size());
                for (BasicNameValuePair param : params) {
                    output.writeString(param.getName());
                    output.writeString(param.getValue());
                }
            } else {
                output.writeShort(0);
            }
        }
    }
}