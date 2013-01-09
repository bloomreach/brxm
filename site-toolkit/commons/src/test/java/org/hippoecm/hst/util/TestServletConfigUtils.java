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
package org.hippoecm.hst.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.junit.Ignore;
import org.junit.Test;

/**
 * TestServletConfigUtils
 * 
 * @version $Id$
 */
public class TestServletConfigUtils {
    
    @Test
    public void testGetInitParameterForServlet() {
        assertNull(ServletConfigUtils.getInitParameter(null, null, "param1", null));
        assertEquals("", ServletConfigUtils.getInitParameter(null, null, "param1", ""));
        assertEquals("default value", ServletConfigUtils.getInitParameter(null, null, "param1", "default value"));
        
        ServletConfig servletConfig = createMock(ServletConfig.class);
        ServletContext servletContext = createMock(ServletContext.class);
        
        expect(servletConfig.getInitParameter("param1")).andReturn("param1's value from config");
        replay(servletConfig);
        assertEquals("param1's value from config", ServletConfigUtils.getInitParameter(servletConfig, null, "param1", null));
        verify(servletConfig);
        reset(servletConfig);
        
        expect(servletContext.getInitParameter("param1")).andReturn("param1's value from context");
        replay(servletContext);
        assertEquals("param1's value from context", ServletConfigUtils.getInitParameter(null, servletContext, "param1", null));
        verify(servletContext);
        reset(servletContext);
        
        expect(servletConfig.getInitParameter("param1")).andReturn("param1's value from config");
        replay(servletConfig);
        assertEquals("param1's value from config", ServletConfigUtils.getInitParameter(servletConfig, servletContext, "param1", null));
        verify(servletConfig);
        reset(servletConfig);
        
        expect(servletContext.getInitParameter("param2")).andReturn("param2's value from context");
        replay(servletContext);
        assertEquals("param2's value from context", ServletConfigUtils.getInitParameter(servletConfig, servletContext, "param2", null));
        verify(servletContext);
        reset(servletContext);
    }
    
    @Test
    public void testGetInitParameterForAdhocObjects() {
        assertNull(ServletConfigUtils.getInitParameter("param1", null, (Object []) null));
        assertEquals("", ServletConfigUtils.getInitParameter("param1", "", null, (Object []) null));
        assertEquals("default value", ServletConfigUtils.getInitParameter("param1", "default value", (Object []) null));

        assertNull(ServletConfigUtils.getInitParameter("param1", null, null, null));
        assertEquals("", ServletConfigUtils.getInitParameter("param1", "", null, null, null));
        assertEquals("default value", ServletConfigUtils.getInitParameter("param1", "default value", null, null));
        
        ServletConfig servletConfig = createMock(ServletConfig.class);
        ServletContext servletContext = createMock(ServletContext.class);
        
        expect(servletConfig.getInitParameter("param1")).andReturn(null);
        expect(servletContext.getInitParameter("param1")).andReturn(null);
        replay(servletConfig);
        replay(servletContext);
        assertNull(ServletConfigUtils.getInitParameter("param1", null, servletConfig, servletContext));
        verify(servletConfig);
        verify(servletContext);
        reset(servletConfig);
        reset(servletContext);
        
        expect(servletConfig.getInitParameter("param1")).andReturn("param1's value from config");
        replay(servletConfig);
        assertEquals("param1's value from config", ServletConfigUtils.getInitParameter("param1", null, servletConfig, servletContext));
        verify(servletConfig);
        reset(servletConfig);

        expect(servletConfig.getInitParameter("param1")).andReturn(null);
        expect(servletContext.getInitParameter("param1")).andReturn("param1's value from context");
        replay(servletConfig);
        replay(servletContext);
        assertEquals("param1's value from context", ServletConfigUtils.getInitParameter("param1", null, servletConfig, servletContext));
        verify(servletConfig);
        verify(servletContext);
        reset(servletConfig);
        reset(servletContext);

        ParameterHoldingObject paramHoldingObject = new ParameterHoldingObject();
        expect(servletConfig.getInitParameter("param1")).andReturn(null);
        expect(servletContext.getInitParameter("param1")).andReturn(null);
        replay(servletConfig);
        replay(servletContext);
        assertEquals("param1's value from param holding object", ServletConfigUtils.getInitParameter("param1", null, servletConfig, servletContext, paramHoldingObject));
        verify(servletConfig);
        verify(servletContext);
        reset(servletConfig);
        reset(servletContext);
    }
    
    @Ignore
    private class ParameterHoldingObject {
        public String getInitParameter(String name) {
            return name + "'s value from param holding object";
        }
    }
}
