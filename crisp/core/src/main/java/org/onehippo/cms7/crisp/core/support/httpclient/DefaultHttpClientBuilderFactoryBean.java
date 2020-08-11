/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.support.httpclient;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Simple {@link FactotyBean} to support creating a {@link HttpClientBuilder}, equivalently to {@link HttpClients#createSystem()},
 * allowing to customize {@code useSystemProperties}, {@code maxConnTotal}, {@code maxConnPerRoute}, etc.
 * By default, 500 max connections in total and 250 max connection per route.
 * This can be helpful especially when creating an {@link HttpClient} bean through XML bean configurations.
 * <p>
 * In most use cases, simple customization on {@link HttpClientBuilder} is a lot easier than customizing all the detail
 * components such as {@link org.apache.http.conn.HttpClientConnectionManager} because the default (system) {@link HttpClientBuilder}
 * configures a lot automatically by default, without having to worry about the details under the hood.
 * <p>
 * Therefore, this class is provided as the default implementation, good enough for most use cases, and also as an
 * example implementation that people can extend from for their specific use cases.
 */
public class DefaultHttpClientBuilderFactoryBean extends AbstractFactoryBean<HttpClientBuilder> {

    static final boolean DEFAULT_USE_SYSTEM_PROPERTIES = true;

    static final int DEFAULT_MAX_CONN_TOTAL = 500;

    static final int DEFAULT_MAX_CONN_PER_ROUTE = 250;

    /**
     * User Agent request header value. Used if set to a non-empty string.
     */
    private String userAgent;

    /**
     * Default request headers. Used if set to a non-empty collection.
     */
    private Collection<? extends Header> defaultHeaders;

    /**
     * Whether to invoke {@link HttpClientBuilder#useSystemProperties()}.
     */
    private boolean useSystemProperties = DEFAULT_USE_SYSTEM_PROPERTIES;

    /**
     * Max connection in total, to be passed to {@link HttpClientBuilder#setMaxConnTotal(int)}.
     */
    private int maxConnTotal = DEFAULT_MAX_CONN_TOTAL;

    /**
     * Max connection per route, to be passed to {@link HttpClientBuilder#setMaxConnPerRoute(int)}.
     */
    private int maxConnPerRoute = DEFAULT_MAX_CONN_PER_ROUTE;

    /**
     * Max time milliseconds to live for persistent connections.
     * <p>
     * If this value is less than or equal to zero, it means no expiration for connections.
     * Refer to {@link org.apache.http.pool.PoolEntry} for detail.
     * <p>
     * By default, <code>HttpClientBuilder<code> is set to -1.
     */
    private Long connectionTimeMillisToLive;

    /**
     * {@link HostnameVerifier} instance. Used if set to non-null instance.
     */
    private HostnameVerifier hostnameVerifier;

    /**
     * {@link RedirectStrategy} instance. Used if set to non-null instance.
     */
    private RedirectStrategy redirectStrategy;

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Collection<? extends Header> getDefaultHeaders() {
        return defaultHeaders;
    }

    public void setDefaultHeaders(Collection<? extends Header> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
    }

    public boolean isUseSystemProperties() {
        return useSystemProperties;
    }

    public void setUseSystemProperties(boolean useSystemProperties) {
        this.useSystemProperties = useSystemProperties;
    }

    public int getMaxConnTotal() {
        return maxConnTotal;
    }

    public void setMaxConnTotal(int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
    }

    public int getMaxConnPerRoute() {
        return maxConnPerRoute;
    }

    public void setMaxConnPerRoute(int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
    }

    public Long getConnectionTimeMillisToLive() {
        return connectionTimeMillisToLive;
    }

    public void setConnectionTimeMillisToLive(Long connectionTimeMillisToLive) {
        this.connectionTimeMillisToLive = connectionTimeMillisToLive;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public RedirectStrategy getRedirectStrategy() {
        return redirectStrategy;
    }

    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }

    @Override
    protected HttpClientBuilder createInstance() throws Exception {
        HttpClientBuilder builder = HttpClientBuilder.create();

        if (userAgent != null && !userAgent.isEmpty()) {
            builder = builder.setUserAgent(userAgent);
        }

        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            builder = builder.setDefaultHeaders(defaultHeaders);
        }

        if (useSystemProperties) {
            builder = builder.useSystemProperties();
        }

        if (maxConnTotal > 0) {
            builder = builder.setMaxConnTotal(maxConnTotal);
        }

        if (maxConnPerRoute > 0) {
            builder = builder.setMaxConnPerRoute(maxConnPerRoute);
        }

        if (connectionTimeMillisToLive != null) {
            builder = builder.setConnectionTimeToLive(connectionTimeMillisToLive.longValue(), TimeUnit.MILLISECONDS);
        }

        if (hostnameVerifier != null) {
            builder = builder.setSSLHostnameVerifier(hostnameVerifier);
        }

        if (redirectStrategy != null) {
            builder = builder.setRedirectStrategy(redirectStrategy);
        }

        return builder;
    }

    @Override
    public Class<?> getObjectType() {
        return HttpClientBuilder.class;
    }

}
