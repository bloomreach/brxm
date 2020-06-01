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
package org.hippoecm.hst.container;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

/**
 * TestHstContainerRequest
 * 
 * @version $Id$
 */
public class TestHstContainerRequest {
    
    @Test
    public void testContainerRequestWithBasicURIs() {
        String pathInfo = "/preview/news/2009./comments/314";
        String contextPath = "/site";
        String requestURI = "/site" + pathInfo;
        StringBuffer requestURL = new StringBuffer("http://localhost:8085" + requestURI);
        
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(request.getRequestURL()).andReturn(requestURL).anyTimes();
        
        replay(request);
        
        HstContainerRequestImpl containerRequest = new HstContainerRequestImpl(request, "./");
        containerRequest.setServletPath("");
        assertEquals("/site/preview/news/2009", containerRequest.getRequestURI());
        assertEquals("http://localhost:8085/site/preview/news/2009", containerRequest.getRequestURL().toString());
        assertEquals("/preview/news/2009", containerRequest.getPathInfo());
        assertEquals("/preview/news/2009", containerRequest.getPathTranslated());
    }
    
    @Test
    public void testContainerRequestWithMatrixURIs() {
        String matrixParams1 = ";lat=50;long=20";
        String matrixParams2 = ";orderBy=author";
        String contextPath = "/site";
        String pathInfo = "/preview/news/2009" + matrixParams1 + "./comments/314" + matrixParams2;
        String requestURI = "/site" + pathInfo;
        StringBuffer requestURL = new StringBuffer("http://localhost:8085" + requestURI);
        
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(request.getRequestURL()).andReturn(requestURL).anyTimes();
        replay(request);
        
        HstContainerRequestImpl containerRequest = new HstContainerRequestImpl(request, "./");
        containerRequest.setServletPath("");
        assertEquals("/site/preview/news/2009" + matrixParams1, containerRequest.getRequestURI());
        assertEquals("http://localhost:8085/site/preview/news/2009" + matrixParams1, containerRequest.getRequestURL().toString());
        assertEquals("/preview/news/2009", containerRequest.getPathInfo());
        assertEquals("/preview/news/2009", containerRequest.getPathTranslated());
    }
    
    @Test
    public void testContainerRequestWithServletPathSet() {
        String matrixParams1 = ";lat=50;long=20";
        String matrixParams2 = ";orderBy=author";
        String contextPath = "/site";
        String pathInfo = "/preview/news/2009" + matrixParams1 + "./comments/314" + matrixParams2;
        String requestURI = "/site" + pathInfo;
        String servletPath = "/preview";
        StringBuffer requestURL = new StringBuffer("http://localhost:8085" + requestURI);
        
        HstContainerRequest request = createNiceMock(HstContainerRequest.class);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(request.getRequestURL()).andReturn(requestURL).anyTimes();
        
        replay(request);
       
        HstContainerRequestImpl containerRequest = new HstContainerRequestImpl(request, "./");
        
        containerRequest.setServletPath("");
        assertEquals("/preview/news/2009", containerRequest.getPathInfo());
        // we now set the servletPath equal to for example the mountPath of mount 'preview'
        containerRequest.setServletPath(servletPath);
      
        assertEquals("/site/preview/news/2009" + matrixParams1, containerRequest.getRequestURI());
        assertEquals("http://localhost:8085/site/preview/news/2009" + matrixParams1, containerRequest.getRequestURL().toString());
        assertEquals("/preview", containerRequest.getServletPath());
        assertEquals("/news/2009", containerRequest.getPathInfo());
        assertEquals("/news/2009", containerRequest.getPathTranslated());
    }
}
