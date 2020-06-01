/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services;

import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ServletContextRegistryTest {

    private ServletContext servletContext;

    @Before
    public void setUp() {
        servletContext = createNiceMock(ServletContext.class);
        expect(servletContext.getContextPath()).andReturn("/site").anyTimes();
        expect(servletContext.getAttribute(eq(ServletContextRegistry.WebAppType.class.getName()))).andReturn(ServletContextRegistry.WebAppType.HST);
        servletContext.setAttribute(eq(ServletContextRegistry.WebAppType.class.getName()), eq(ServletContextRegistry.WebAppType.HST));
        expectLastCall();
        replay(servletContext);
    }

    @After
    public void tearDown() {
        if (ServletContextRegistry.getContexts().containsKey(servletContext.getContextPath())) {
            ServletContextRegistry.unregister(servletContext);
        }
    }

    @Test
    public void register_servlet_context() {

        ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.HST);

        assertEquals(1, ServletContextRegistry.getContexts().size());
        try {
            ServletContextRegistry.getContexts().put("foo", servletContext);
            fail("#getContexts should return unmudifiable map");
        } catch (UnsupportedOperationException e) {
            // expected
        }

        try {
            ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.HST);
            fail("#registering same servlet context twice is not allowed");
        } catch (IllegalStateException e) {
            // expected
        }
        try {
            ServletContextRegistry.register(servletContext, ServletContextRegistry.WebAppType.REPO);
            fail("#registering same servlet context twice is not allowed, even not with different type");
        } catch (IllegalStateException e) {
            // expected
        }

        assertEquals(1, ServletContextRegistry.getContexts(ServletContextRegistry.WebAppType.HST).size());
        assertEquals(0, ServletContextRegistry.getContexts(ServletContextRegistry.WebAppType.REPO).size());

    }

    @Test(expected = IllegalArgumentException.class)
    public void register_servlet_context_with_null_type_not_allowed() {
        ServletContextRegistry.register(servletContext, null);
    }

}
