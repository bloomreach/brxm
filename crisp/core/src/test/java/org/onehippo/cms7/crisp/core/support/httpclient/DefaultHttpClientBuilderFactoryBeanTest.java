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

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultHttpClientBuilderFactoryBeanTest {

    private static Logger log = LoggerFactory.getLogger(DefaultHttpClientBuilderFactoryBeanTest.class);

    private ClassPathXmlApplicationContext appCtx;
    private HttpClientBuilder builderWithDefaults;
    private HttpClientBuilder builderCustomized;

    @Before
    public void setUp() throws Exception {
        appCtx = new ClassPathXmlApplicationContext();
        appCtx.setConfigLocation(DefaultHttpClientBuilderFactoryBeanTest.class.getName().replace(".", "/") + ".xml");
        appCtx.refresh();

        builderWithDefaults = appCtx.getBean("httpClientBuilderWithDefaults", HttpClientBuilder.class);
        builderCustomized = appCtx.getBean("httpClientBuilderCustomized", HttpClientBuilder.class);

        assertNotNull(builderWithDefaults);
        assertNotNull(builderCustomized);
        assertNotEquals(builderWithDefaults, builderCustomized);
    }

    @After
    public void tearDown() throws Exception {
        appCtx.stop();
        appCtx.close();
    }

    @Test
    public void testBuilderWithDefaults() throws Exception {
        final Boolean systemProperties = findInternalSystemPropertiesFieldValue(builderWithDefaults);
        assertNotNull(systemProperties);
        assertTrue(systemProperties);

        final CloseableHttpClient httpClient = builderWithDefaults.build();
        final PoolingHttpClientConnectionManager connManager = findInternalPoolingHttpClientConnectionManagerFieldValue(
                httpClient);
        assertEquals(DefaultHttpClientBuilderFactoryBean.DEFAULT_MAX_CONN_TOTAL, connManager.getMaxTotal());
        assertEquals(DefaultHttpClientBuilderFactoryBean.DEFAULT_MAX_CONN_PER_ROUTE,
                connManager.getDefaultMaxPerRoute());

        httpClient.close();
    }

    @Test
    public void testBuilderCustomized() throws Exception {
        final Boolean systemProperties = findInternalSystemPropertiesFieldValue(builderWithDefaults);
        assertNotNull(systemProperties);
        assertTrue(systemProperties);

        final CloseableHttpClient httpClient = builderCustomized.build();
        final PoolingHttpClientConnectionManager connManager = findInternalPoolingHttpClientConnectionManagerFieldValue(
                httpClient);
        assertEquals(250, connManager.getMaxTotal());
        assertEquals(125, connManager.getDefaultMaxPerRoute());

        httpClient.close();
    }

    // Internal field access to return the internal systemProperties field value only for validating the configurations.
    private Boolean findInternalSystemPropertiesFieldValue(final HttpClientBuilder httpClientBuilder) {
        try {
            final Boolean systemProperties = (Boolean) FieldUtils
                    .getField(HttpClientBuilder.class, "systemProperties", true).get(httpClientBuilder);
            return systemProperties;
        } catch (Exception e) {
            log.error("Failed to find the internal systemProperties field value from httpClientBuilder: {}",
                    httpClientBuilder, e);
            fail("Failed to find the internal systemProperties field value from httpClientBuilder: "
                    + httpClientBuilder);
        }

        return null;
    }

    // Internal field access to return the internal PoolingHttpClientConnectionManager field value only for validating
    // the configurations such as max count in total, max count per route, etc.
    private PoolingHttpClientConnectionManager findInternalPoolingHttpClientConnectionManagerFieldValue(
            final CloseableHttpClient httpClient) {
        try {
            final Class<?> internalHttpClientClazz = ClassUtils
                    .getClass("org.apache.http.impl.client.InternalHttpClient");
            assertNotNull(internalHttpClientClazz);
            final PoolingHttpClientConnectionManager connManager = (PoolingHttpClientConnectionManager) FieldUtils
                    .getField(internalHttpClientClazz, "connManager", true).get(httpClient);
            return connManager;
        } catch (Exception e) {
            log.error("Failed to find the internal PoolingHttpClientConnectionManager field value from httpClient: {}",
                    httpClient, e);
            fail("Failed to find the internal PoolingHttpClientConnectionManager field value from httpClient: "
                    + httpClient);
        }

        return null;
    }
}
