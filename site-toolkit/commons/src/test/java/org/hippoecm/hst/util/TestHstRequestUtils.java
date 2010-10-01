/*
 *  Copyright 2008 Hippo.
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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

/**
 * TestHstRequestUtils
 * 
 * @version $Id$
 */
public class TestHstRequestUtils {
    
    @Test
    public void testRequestPath() throws Exception {
        String contextPath = "/site";
        String pathInfo = "/news/headlines";
        String matrixParams = ";JSESSIONID=abc;foo=bar";
        String requestURI = contextPath + pathInfo;
        
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        replay(request);
        
        String requestPath = HstRequestUtils.getRequestPath(request);
        
        assertEquals(pathInfo, requestPath);
        
        requestURI = contextPath + pathInfo + ";" + matrixParams;
        
        reset(request);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        replay(request);
        
        requestPath = HstRequestUtils.getRequestPath(request);
        
        assertEquals(pathInfo, requestPath);
    }
    
}
