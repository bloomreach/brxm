/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;

/**
 * Extra message exchange hint representation for backend services.
 */
public interface ExchangeHint {

    /**
     * Return method name hint. e.g, "POST", "PUT", "DELETE", "GET", etc. for http-based backends.
     * @return method name hint. e.g, "POST", "PUT", "DELETE", "GET", etc. for http-based backends
     */
    public String getMethodName();

    /**
     * Return request object representation or custom request callback instance that can be understood by the underlying
     * {@link ResourceResolver} implementation.
     * @return request object representation to be understood by the underlying {@link ResourceResolver} implementation
     * @deprecated Use {@link #getRequestHeaders()} and {@link #getRequestBody()} instead.
     */
    @Deprecated
    public Object getRequest();

    /**
     * Return request headers map.
     * @return request headers map
     */
    public Map<String, List<String>> getRequestHeaders();

    /**
     * Return request body.
     * @return request body
     */
    public Object getRequestBody();

    /**
     * Return a cache key of this hint.
     * @return a cache key of this hint
     */
    public Object getCacheKey();

    /**
     * Return response status code.
     * @return response status code
     */
    public int getResponseStatusCode();

    /**
     * Set response status code.
     * @param responseStatusCode response status code
     */
    public void setResponseStatusCode(int responseStatusCode);

    /**
     * Return response headers map.
     * @return response headers map
     */
    public Map<String, List<String>> getResponseHeaders();

    /**
     * Set response headers map.
     * @param  responseHeaders response headers map
     */
    public void setResponseHeaders(Map<String, List<String>> responseHeaders);

    /**
     * Return response body. Null if the underlying {@link ResourceResolver} doesn't set it.
     * The type of response body depends on the underlying {@link ResourceResolver} implementation.
     * It could be a {@link Resource} object (mostly likely when the backend produces the same content type for
     * both normal response and error response),  byte[] or String, for example.
     * <p>
     * <em>Note:</em> This method can be used mostly when the underlying {@link ResourceResolver} throws an exception
     * instead of returning a result.
     * @return response body
     */
    public Object getResponseBody();

    /**
     * Set response body. The type of response body depends on the underlying {@link ResourceResolver} implementation.
     * It could be a {@link Resource} object (mostly likely when the backend produces the same content type for
     * both normal response and error response),  byte[] or String, for example.
     * <p>
     * <em>Note:</em> This method can be used by the underlying {@link ResourceResolver} when a call on the backend
     * returns an error, instead of a normal result.
     * @param responseBody response body
     */
    public void setResponseBody(Object responseBody);

}
