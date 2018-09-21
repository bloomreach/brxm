/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.resource.ResourceResolver;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MapResourceResolverProviderTest {

    private MapResourceResolverProvider resourceResolverProvider;

    private ResourceResolver resolverInMap1;
    private ResourceResolver resolverInMap2;

    @Before
    public void setUp() throws Exception {
        resourceResolverProvider = new MapResourceResolverProvider();

        final Map<String, ResourceResolver> resourceResolverMap = new HashMap<>();
        resolverInMap1 = EasyMock.createNiceMock(ResourceResolver.class);
        resolverInMap2 = EasyMock.createNiceMock(ResourceResolver.class);
        resourceResolverMap.put("resolverInMap1", resolverInMap1);
        resourceResolverMap.put("resolverInMap2", resolverInMap2);
        resourceResolverProvider.setResourceResolverMap(resourceResolverMap);
    }

    @Test
    public void testSetResourceResolverMap() throws Exception {
        assertBasicResourceResolvers();
    }

    @Test
    public void testSetResourceResolver() throws Exception {
        final ResourceResolver resolverToSet1 = EasyMock.createNiceMock(ResourceResolver.class);
        resourceResolverProvider.setResourceResolver("resolverToSet1", resolverToSet1);
        final ResourceResolver resolverToSet2 = EasyMock.createNiceMock(ResourceResolver.class);
        resourceResolverProvider.setResourceResolver("resolverToSet2", resolverToSet2);

        assertBasicResourceResolvers();
        assertSame(resolverToSet1, resourceResolverProvider.getResourceResolver("resolverToSet1"));
        assertSame(resolverToSet2, resourceResolverProvider.getResourceResolver("resolverToSet2"));
    }

    @Test
    public void testResourceResolverFromSpringBeanFactory() throws Exception {
        final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        final ResourceResolver resolverInSpring1 = EasyMock.createNiceMock(ResourceResolver.class);
        final ResourceResolver resolverInSpring2 = EasyMock.createNiceMock(ResourceResolver.class);
        beanFactory.addBean("resolverInSpring1", resolverInSpring1);
        beanFactory.addBean("resolverInSpring2", resolverInSpring2);
        resourceResolverProvider.setBeanFactory(beanFactory);

        assertBasicResourceResolvers();
        assertNull(resourceResolverProvider.getResourceResolver("resolverInSpringNonExisting"));
        assertSame(resolverInSpring1, resourceResolverProvider.getResourceResolver("resolverInSpring1"));
        assertSame(resolverInSpring2, resourceResolverProvider.getResourceResolver("resolverInSpring2"));
    }

    private void assertBasicResourceResolvers() throws Exception {
        assertNull(resourceResolverProvider.getResourceResolver("resolverNonExisting"));
        assertSame(resolverInMap1, resourceResolverProvider.getResourceResolver("resolverInMap1"));
        assertSame(resolverInMap2, resourceResolverProvider.getResourceResolver("resolverInMap2"));
    }
}
