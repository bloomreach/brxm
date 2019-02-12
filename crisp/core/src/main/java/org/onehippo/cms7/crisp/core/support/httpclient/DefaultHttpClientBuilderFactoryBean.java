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

import org.apache.http.client.HttpClient;
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

    @Override
    protected HttpClientBuilder createInstance() throws Exception {
        HttpClientBuilder builder = HttpClientBuilder.create();

        if (useSystemProperties) {
            builder = builder.useSystemProperties();
        }

        if (maxConnTotal > 0) {
            builder = builder.setMaxConnTotal(maxConnTotal);
        }

        if (maxConnPerRoute > 0) {
            builder.setMaxConnPerRoute(maxConnPerRoute);
        }

        return builder;
    }

    @Override
    public Class<?> getObjectType() {
        return HttpClientBuilder.class;
    }

}
