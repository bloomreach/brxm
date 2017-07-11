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
package org.onehippo.cms7.crisp.core.broker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBrokerRequestContext;
import org.onehippo.cms7.crisp.api.resource.AbstractResourceResolver;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.core.resource.MapResourceResolverProvider;
import org.onehippo.cms7.crisp.core.resource.SpringResourceDataCache;
import org.onehippo.cms7.crisp.core.resource.jackson.JacksonResource;
import org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CacheableResourceServiceBrokerTest {

    private CacheableResourceServiceBroker broker;
    private SimpleJacksonRestTemplateResourceResolver demoResourceResolver1;
    private AbstractResourceResolver demoResourceResolver2;

    private ServletRequest request;

    private ObjectMapper objectMapper = new ObjectMapper();

    private AtomicInteger demo1CallCounter = new AtomicInteger();

    @Before
    public void setUp() throws Exception {
        demoResourceResolver1 = new SimpleJacksonRestTemplateResourceResolver();
        demoResourceResolver1.setCacheEnabled(true);
        demoResourceResolver1.setResourceDataCache(new SpringResourceDataCache(new ConcurrentMapCache("demo1Cache")));

        demoResourceResolver2 = new AbstractResourceResolver() {
            @Override
            public Resource resolve(String absPath, Map<String, Object> pathVariables) throws ResourceException {
                demo1CallCounter.incrementAndGet();
                if ("/null".equals(absPath)) {
                    return null;
                }
                ObjectNode node = objectMapper.createObjectNode();
                node.put("absPath", absPath);
                return new JacksonResource(node);
            }

            @Override
            public Resource findResources(String baseAbsPath, Map<String, Object> pathVariables)
                    throws ResourceException {
                demo1CallCounter.incrementAndGet();
                if ("/null".equals(baseAbsPath)) {
                    return null;
                }
                ObjectNode node = objectMapper.createObjectNode();
                node.put("absPath", baseAbsPath);
                return new JacksonResource(node);
            }
        };
        demoResourceResolver2.setCacheEnabled(false);

        MapResourceResolverProvider resourceResolverProvider = new MapResourceResolverProvider();
        Map<String, ResourceResolver> resourceResolverMap = new HashMap<>();
        resourceResolverMap.put("demo1", demoResourceResolver1);
        resourceResolverMap.put("demo2", demoResourceResolver2);
        resourceResolverProvider.setResourceResolverMap(resourceResolverMap);

        broker = new CacheableResourceServiceBroker();
        broker.setResourceResolverProvider(resourceResolverProvider);
        broker.setCacheEnabled(true);
        broker.setCacheInRequestEnabled(true);

        request = new MockHttpServletRequest();
        ResourceServiceBrokerRequestContext.setCurrentServletRequest(request);
    }

    @After
    public void tearDown() throws Exception {
        ResourceServiceBrokerRequestContext.clear();
    }

    @Test
    public void testFindingResourceResolver() throws Exception {
        ResourceResolver resourceResolver1 = broker.getResourceResolverProvider().getResourceResolver("demo1");
        assertNotNull(resourceResolver1);
        assertSame(demoResourceResolver1, resourceResolver1);

        ResourceResolver resourceResolver2 = broker.getResourceResolverProvider().getResourceResolver("demo2");
        assertNotNull(resourceResolver2);
        assertSame(demoResourceResolver2, resourceResolver2);
    }

    @Test
    public void testResourceDataCache() throws Exception {
        ResourceResolver resourceResolver1 = broker.getResourceResolverProvider().getResourceResolver("demo1");
        assertNotNull(resourceResolver1);
        assertNotNull(resourceResolver1.getResourceDataCache());
        assertSame(resourceResolver1.getResourceDataCache(), broker.getResourceDataCache("demo1"));

        ResourceResolver resourceResolver2 = broker.getResourceResolverProvider().getResourceResolver("demo2");
        assertNotNull(resourceResolver2);
        assertNull(resourceResolver2.getResourceDataCache());
        assertNull(broker.getResourceDataCache("demo2"));
    }

    @Test
    public void testRequestLevelCaching() throws Exception {
        assertFalse(demoResourceResolver2.isCacheEnabled());
        assertTrue(broker.isCacheInRequestEnabled());

        demo1CallCounter.set(0);

        Resource resource1 = broker.resolve("demo2", "/path1");
        assertEquals(1, demo1CallCounter.get());
        Resource resource2 = broker.resolve("demo2", "/path1");
        assertEquals(1, demo1CallCounter.get());
        assertSame(resource1, resource2);

        broker.setCacheInRequestEnabled(false);

        resource1 = broker.resolve("demo2", "/path1");
        assertEquals(2, demo1CallCounter.get());
        resource2 = broker.resolve("demo2", "/path1");
        assertEquals(3, demo1CallCounter.get());
        assertNotSame(resource1, resource2);
        assertEquals(resource1, resource2);

        broker.setCacheInRequestEnabled(true);
    }

    @Test
    public void testRequestLevelCachingForNull() throws Exception {
        assertFalse(demoResourceResolver2.isCacheEnabled());
        assertTrue(broker.isCacheInRequestEnabled());

        demo1CallCounter.set(0);

        Resource resource1 = broker.resolve("demo2", "/null");
        assertEquals(1, demo1CallCounter.get());
        Resource resource2 = broker.resolve("demo2", "/null");
        assertEquals(1, demo1CallCounter.get());
        assertNull(resource1);
        assertNull(resource2);
    }
}
