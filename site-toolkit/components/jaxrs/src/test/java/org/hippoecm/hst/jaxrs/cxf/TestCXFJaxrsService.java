/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.cxf;

import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestCXFJaxrsService {
    
    private CXFJaxrsService service = new CXFJaxrsService("a_service");
    
    @Test
    public void testGetJaxrsPathInfo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/app1/mount1");
        MockHstRequestContext requestContext = new MockHstRequestContext();
        HstContainerURL baseURL = EasyMock.createNiceMock(HstContainerURL.class);
        EasyMock.expect(baseURL.getContextPath()).andReturn("/app1").anyTimes();
        EasyMock.expect(baseURL.getResolvedMountPath()).andReturn("/mount1").anyTimes();
        EasyMock.replay(baseURL);

        final ResolvedMount resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        EasyMock.expect(resolvedMount.getMatchingIgnoredPrefix()).andStubReturn(null);
        EasyMock.replay(resolvedMount);

        requestContext.setResolvedMount(resolvedMount);

        requestContext.setBaseURL(baseURL);
        
        String pathInfo = service.getJaxrsPathInfo(requestContext, request);
        assertEquals("/", pathInfo);
        
        request = new MockHttpServletRequest();
        request.setRequestURI("/app1/mount1/");
        requestContext = new MockHstRequestContext();
        baseURL = EasyMock.createNiceMock(HstContainerURL.class);
        EasyMock.expect(baseURL.getContextPath()).andReturn("/app1").anyTimes();
        EasyMock.expect(baseURL.getResolvedMountPath()).andReturn("/mount1").anyTimes();
        EasyMock.replay(baseURL);
        requestContext.setBaseURL(baseURL);

        requestContext.setResolvedMount(resolvedMount);
        
        pathInfo = service.getJaxrsPathInfo(requestContext, request);
        assertEquals("/", pathInfo);
        
        request = new MockHttpServletRequest();
        request.setRequestURI("/app1/mount1/a/b/c");
        requestContext = new MockHstRequestContext();
        baseURL = EasyMock.createNiceMock(HstContainerURL.class);
        EasyMock.expect(baseURL.getContextPath()).andReturn("/app1").anyTimes();
        EasyMock.expect(baseURL.getResolvedMountPath()).andReturn("/mount1").anyTimes();
        EasyMock.replay(baseURL);
        requestContext.setBaseURL(baseURL);

        requestContext.setResolvedMount(resolvedMount);

        pathInfo = service.getJaxrsPathInfo(requestContext, request);
        assertEquals("/a/b/c", pathInfo);
    }
    
    @Test
    public void testGetJaxrsPathInfoWithMatrixParamsInMountPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/app1/mount1;p1=1/a/b/c");
        MockHstRequestContext requestContext = new MockHstRequestContext();
        HstContainerURL baseURL = EasyMock.createNiceMock(HstContainerURL.class);
        EasyMock.expect(baseURL.getContextPath()).andReturn("/app1").anyTimes();
        EasyMock.expect(baseURL.getResolvedMountPath()).andReturn("/mount1").anyTimes();
        EasyMock.replay(baseURL);

        final ResolvedMount resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        EasyMock.expect(resolvedMount.getMatchingIgnoredPrefix()).andStubReturn(null);
        EasyMock.replay(resolvedMount);

        requestContext.setResolvedMount(resolvedMount);


        requestContext.setBaseURL(baseURL);
        
        String pathInfo = service.getJaxrsPathInfo(requestContext, request);
        assertEquals("/a/b/c", pathInfo);
    }
    
}
