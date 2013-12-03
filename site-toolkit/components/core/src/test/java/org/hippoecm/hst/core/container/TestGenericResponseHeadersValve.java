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
package org.hippoecm.hst.core.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.easymock.EasyMock;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * TestGenericResponseHeadersValve
 * @version $Id$
 */
public class TestGenericResponseHeadersValve {

    private GenericResponseHeadersValve responseHeadersValve;
    private MockHttpServletResponse servletResponse;
    private ValveContext valveContext;

    @Before
    public void setUp() throws Exception {
        responseHeadersValve = new GenericResponseHeadersValve();
        servletResponse = new MockHttpServletResponse();
        valveContext = EasyMock.createNiceMock(ValveContext.class);
        EasyMock.expect(valveContext.getServletResponse()).andReturn(servletResponse).anyTimes();
        EasyMock.replay(valveContext);
    }

    @Test
    public void testCacheResponseHeaders() throws Exception {
        List<KeyValue<String, Object>> settableHeaders = new ArrayList<KeyValue<String, Object>>();
        settableHeaders.add(new DefaultKeyValue<String, Object>("Cache-Control", "no-cache")); //HTTP 1.1
        settableHeaders.add(new DefaultKeyValue<String, Object>("Pragma", "no-cache")); //HTTP 1.0
        Date expireDate = new Date();
        settableHeaders.add(new DefaultKeyValue<String, Object>("Expires", new Long(expireDate.getTime())));

        responseHeadersValve.setSettableHeaders(settableHeaders);

        List<KeyValue<String, Object>> addableHeaders = new ArrayList<KeyValue<String, Object>>();
        addableHeaders.add(new DefaultKeyValue<String, Object>("Author", "Apache"));
        addableHeaders.add(new DefaultKeyValue<String, Object>("Author", "Cotati"));

        responseHeadersValve.setAddableHeaders(addableHeaders);

        responseHeadersValve.invoke(valveContext);

        Collection<String> headerNames = servletResponse.getHeaderNames();
        assertTrue(headerNames.contains("Cache-Control"));
        assertTrue(headerNames.contains("Pragma"));
        assertTrue(headerNames.contains("Expires"));
        assertTrue(headerNames.contains("Author"));

        assertEquals("no-cache", servletResponse.getHeader("Cache-Control"));
        assertEquals("no-cache", servletResponse.getHeader("Pragma"));
        assertEquals(Long.toString(expireDate.getTime()), servletResponse.getHeader("Expires"));

        List<String> authorHeaders = servletResponse.getHeaders("Author");
        assertTrue(authorHeaders.contains("Apache"));
        assertTrue(authorHeaders.contains("Cotati"));
    }
}
