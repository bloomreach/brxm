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
     * to include HTTP header or body data in the request
     */
    private Object request;

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
    public ExchangeHint build() {
        DefaultExchangeHint exchangeHint = new DefaultExchangeHint();
        exchangeHint.setMethodName(methodName());
        exchangeHint.setRequest(request());
        return exchangeHint;
    }

}
