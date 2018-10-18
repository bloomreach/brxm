/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.container.sitemapitemhandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class HstSiteMapItemHandlerRegistryImplTest {

    @Test
    public void test_weak_references_cleanup() {

        final HstSiteMapItemHandlerRegistryImpl registry = new HstSiteMapItemHandlerRegistryImpl();

        assertEquals(0L, registry.siteMapItemHandlersMap.size());

        DummySiteMapItemHandler dummy = new DummySiteMapItemHandler();

        registry.registerSiteMapItemHandler("id1", dummy);

        assertEquals(1L, registry.siteMapItemHandlersMap.size());

        assertNotNull(registry.siteMapItemHandlersMap.get("id1").get());

        // set dummy handler null
        dummy = null;

        long start = System.currentTimeMillis();
        while (registry.siteMapItemHandlersMap.get("id1").get() != null) {
            System.gc();
            assertEquals(1L, registry.siteMapItemHandlersMap.size());
            if ( (System.currentTimeMillis() - start) > 10000) {
                fail("Expected that 'dummy' handler would had been garbage collected within 10 seconds");
            }
        }

        assertEquals("After GC, item should still be in the handler map", 1L, registry.siteMapItemHandlersMap.size());

        registry.expungeStaleEntries();

        assertEquals( 0L, registry.siteMapItemHandlersMap.size());

    }

    @Test
    public void test_many_weak_references_cleanup() {
        final HstSiteMapItemHandlerRegistryImpl registry = new HstSiteMapItemHandlerRegistryImpl();

        DummySiteMapItemHandler dummy = new DummySiteMapItemHandler();

        registry.registerSiteMapItemHandler("id1", dummy);

        for (int i = 2 ; i < 10000 ; i++) {
            registry.registerSiteMapItemHandler("id" + i, new DummySiteMapItemHandler());
        }

        long start = System.currentTimeMillis();
        while (registry.siteMapItemHandlersMap.size() > 1) {
            System.gc();
            registry.expungeStaleEntries();
            if ( (System.currentTimeMillis() - start) > 10000) {
                fail("Expected that 'dummy' handler would had been garbage collected within 10 seconds");
            }
        }

        assertEquals(1L, registry.siteMapItemHandlersMap.size());
    }


    public static class DummySiteMapItemHandler implements HstSiteMapItemHandler {
        @Override
        public void init(final ServletContext servletContext, final SiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException {

        }

        @Override
        public ResolvedSiteMapItem process(final ResolvedSiteMapItem resolvedSiteMapItem, final HttpServletRequest request, final HttpServletResponse response) throws HstSiteMapItemHandlerException {
            return resolvedSiteMapItem;
        }

        @Override
        public void destroy() throws HstSiteMapItemHandlerException {

        }
    }
}
