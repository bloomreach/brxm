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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.onehippo.cms7.crisp.core.resource.MapResourceResolverProvider;
import org.onehippo.cms7.crisp.core.resource.SpringResourceDataCache;
import org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver;
import org.springframework.cache.concurrent.ConcurrentMapCache;

public class CacheableResourceServiceBrokerTest {

    private CacheableResourceServiceBroker broker;
    private SimpleJacksonRestTemplateResourceResolver demoResourceResolver1;
    private SimpleJacksonRestTemplateResourceResolver demoResourceResolver2;

    @Before
    public void setUp() throws Exception {
        demoResourceResolver1 = new SimpleJacksonRestTemplateResourceResolver();
        demoResourceResolver1.setCacheEnabled(true);
        demoResourceResolver1.setResourceDataCache(new SpringResourceDataCache(new ConcurrentMapCache("demo1Cache")));

        demoResourceResolver2 = new SimpleJacksonRestTemplateResourceResolver();
        demoResourceResolver2.setCacheEnabled(false);

        MapResourceResolverProvider resourceResolverProvider = new MapResourceResolverProvider();
        Map<String, ResourceResolver> resourceResolverMap = new HashMap<>();
        resourceResolverMap.put("demo1", demoResourceResolver1);
        resourceResolverMap.put("demo2", demoResourceResolver2);
        resourceResolverProvider.setResourceResolverMap(resourceResolverMap);

        broker = new CacheableResourceServiceBroker();
        broker.setResourceResolverProvider(resourceResolverProvider);
        broker.setCacheEnabled(true);
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

}
