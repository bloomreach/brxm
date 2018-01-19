/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.api.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link ExchangeHintBuilder} implementation.
 */
class DefaultExchangeHintBuilder extends ExchangeHintBuilder {

    /**
     * Method name hint for the backend.
     */
    private String methodName = "GET";

    /**
     * Request object representation than can be understood by the backend.
     * The request object representation can be an implementation or backend specific object such as <code>HttpEntity</code>
     * to include HTTP header or body data in the request.
     * @deprecated
     */
    @Deprecated
    private Object request;

    private Map<String, List<String>> requestHeaders;

    private Map<String, List<String>> unmodifiableRequestHeaders = Collections.emptyMap();

    private Object requestBody;

    DefaultExchangeHintBuilder() {
    }

    @Override
    public ExchangeHintBuilder methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    @Override
    public String methodName() {
        return methodName;
    }

    @Override
    public ExchangeHintBuilder request(Object request) {
        this.request = request;
        return this;
    }

    @Override
    public Object request() {
        return request;
    }

    @Override
    public ExchangeHintBuilder requestHeader(String headerName, String ... headerValues) {
        if (headerValues == null || headerValues.length == 0) {
            return this;
        }

        if (requestHeaders == null) {
            requestHeaders = new LinkedHashMap<>();
            unmodifiableRequestHeaders = Collections.unmodifiableMap(this.requestHeaders);
        }

        List<String> valueList = requestHeaders.get(headerName);

        if (valueList == null) {
            valueList = new ArrayList<>();
        }

        for (String value : headerValues) {
            valueList.add(value);
        }

        requestHeaders.put(headerName, valueList);

        return this;
    }

    @Override
    public ExchangeHintBuilder requestHeaders(Map<String, List<String>> requestHeaders) {
        if (this.requestHeaders == null) {
            this.requestHeaders = new LinkedHashMap<>();
            unmodifiableRequestHeaders = Collections.unmodifiableMap(this.requestHeaders);
        } else {
            this.requestHeaders.clear();
        }

        if (requestHeaders != null) {
            this.requestHeaders.putAll(requestHeaders);
        }

        return this;
    }

    @Override
    public Map<String, List<String>> requestHeaders() {
        return unmodifiableRequestHeaders;
    }

    @Override
    public ExchangeHintBuilder requestBody(Object requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    @Override
    public Object requestBody() {
        return requestBody;
    }

    @Override
    public ExchangeHint build() {
        DefaultExchangeHint exchangeHint = new DefaultExchangeHint();
        exchangeHint.setMethodName(methodName());
        exchangeHint.setRequest(request());
        exchangeHint.setRequestHeaders(requestHeaders);
        exchangeHint.setRequestBody(requestBody);
        return exchangeHint;
    }

}
