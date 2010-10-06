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
package org.hippoecm.hst.container;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        String pathTranslated = null;
        String requestURI = "/site" + pathInfo;
        StringBuffer requestURL = new StringBuffer("http://localhost:8085" + requestURI);
        
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getRequestURL()).andReturn(requestURL).anyTimes();
        expect(request.getPathInfo()).andReturn(pathInfo).anyTimes();
        expect(request.getPathTranslated()).andReturn(pathTranslated).anyTimes();
        
        replay(request);
        
        HstContainerRequest containerRequest = new HstContainerRequestImpl(request, "./");
        assertEquals("/site/preview/news/2009", containerRequest.getRequestURI());
        assertEquals("http://localhost:8085/site/preview/news/2009", containerRequest.getRequestURL().toString());
        assertEquals("/preview/news/2009", containerRequest.getPathInfo());
        assertNull(containerRequest.getPathTranslated());
    }
    
    @Test
    public void testContainerRequestWithMatrixURIs() {
        String matrixParams1 = ";lat=50;long=20";
        String matrixParams2 = ";orderBy=author";
        String pathInfo = "/preview/news/2009" + matrixParams1 + "./comments/314" + matrixParams2;
        String pathTranslated = null;
        String requestURI = "/site" + pathInfo;
        StringBuffer requestURL = new StringBuffer("http://localhost:8085" + requestURI);
        
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getRequestURI()).andReturn(requestURI).anyTimes();
        expect(request.getRequestURL()).andReturn(requestURL).anyTimes();
        expect(request.getPathInfo()).andReturn(pathInfo).anyTimes();
        expect(request.getPathTranslated()).andReturn(pathTranslated).anyTimes();
        
        replay(request);
        
        HstContainerRequest containerRequest = new HstContainerRequestImpl(request, "./");
        assertEquals("/site/preview/news/2009" + matrixParams1, containerRequest.getRequestURI());
        assertEquals("http://localhost:8085/site/preview/news/2009" + matrixParams1, containerRequest.getRequestURL().toString());
        assertEquals("/preview/news/2009", containerRequest.getPathInfo());
        assertNull(containerRequest.getPathTranslated());
    }
}
