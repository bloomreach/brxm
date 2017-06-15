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
package org.onehippo.cms7.crisp.core.resource;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.crisp.api.resource.AbstractResourceResolver;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Abstract {@link ResourceResolver} base class enabling HTTP based communication with backends.
 */
public abstract class AbstractHttpRequestResourceResolver extends AbstractResourceResolver {

    /**
     * HTTP request factory.
     */
    private ClientHttpRequestFactory clientHttpRequestFactory;

    /**
     * Base URI of HTTP requests by this base resolver.
     */
    private String baseUri;

    /**
     * Default constructor.
     */
    public AbstractHttpRequestResourceResolver() {
        super();
    }

    /**
     * Returns the HTTP request factory.
     * @return the HTTP request factory
     */
    public ClientHttpRequestFactory getClientHttpRequestFactory() {
        return clientHttpRequestFactory;
    }

    /**
     * Sets the HTTP request factory.
     * @param clientHttpRequestFactory the HTTP request factory
     */
    public void setClientHttpRequestFactory(ClientHttpRequestFactory clientHttpRequestFactory) {
        this.clientHttpRequestFactory = clientHttpRequestFactory;
    }

    /**
     * Returns the base URI of HTTP requests by this base resolver.
     * @return the base URI of HTTP requests by this base resolver
     */
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Sets the base URI of HTTP requests by this base resolver.
     * @param baseUri the base URI of HTTP requests by this base resolver
     */
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    /**
     * Returns the base resource URI by prepending the {@link baseUri} to {@code absPath}, if set.
     * @param absPath relative resource path
     * @return the base resource URI by prepending the {@link baseUri} to {@code absPath}, if set.
     */
    protected String getBaseResourceURI(final String absPath) {
        StringBuilder sb = new StringBuilder(80);

        if (StringUtils.isNotEmpty(baseUri)) {
            sb.append(baseUri);
        }

        if (StringUtils.isNotEmpty(absPath)) {
            if (StringUtils.endsWith(baseUri, "/") && StringUtils.startsWith(absPath, "/")) {
                sb.append(absPath.substring(1));
            } else {
                sb.append(absPath);
            }
        }

        return sb.toString();
    }

}
