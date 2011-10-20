/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.jaxrs;

import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.hippoecm.hst.container.HstContainerRequest;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.jaxrs.AbstractJaxrsService.PathsAdjustedHttpServletRequestWrapper;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestPathsAdjustedHttpServletRequestWrapper {
    
    private static Logger log = LoggerFactory.getLogger(TestPathsAdjustedHttpServletRequestWrapper.class);
    
    private String scheme = "http";
    private String serverName = "www.example.org";
    private int serverPort = 8080;
    private String pathSuffixParameter = "./";
    
    private ResolvedMount resolvedMount;
    
    @Before
    public void setUp() throws Exception {
        ResolvedVirtualHost resolvedVirtualHost = EasyMock.createNiceMock(ResolvedVirtualHost.class);
        EasyMock.expect(resolvedVirtualHost.getResolvedHostName()).andReturn(serverName).anyTimes();
        EasyMock.expect(resolvedVirtualHost.getPortNumber()).andReturn(serverPort).anyTimes();
        
        resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        EasyMock.expect(resolvedMount.getResolvedVirtualHost()).andReturn(resolvedVirtualHost).anyTimes();
        
        EasyMock.replay(resolvedVirtualHost);
        EasyMock.replay(resolvedMount);
    }
    
    @Test
    public void testMatrixParameters() throws Exception {
        
        String contextPath = "/app1";
        String mountPath = "/mount1";
        String pathInfo = "/a;p1=1;p11=11/b;p2=2;p22=22/c;p3=3;p33=33./x;p1=1;p11=11/y;p2=2;p22=22/z;p3=3;p33=33";
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(serverName);
        request.setServerPort(serverPort);
        request.setRequestURI(contextPath + mountPath + pathInfo);
        request.setContextPath(contextPath);
        request.setServletPath(mountPath);
        request.setPathInfo(pathInfo);
        
        HstContainerRequest containerRequest = new HstContainerRequestImpl(request, pathSuffixParameter);
        
        assertEquals(containerRequest.getPathInfo(), "/mount1/a/b/c");
        assertEquals(containerRequest.getPathSuffix(), "x;p1=1;p11=11/y;p2=2;p22=22/z;p3=3;p33=33");
        
        MockHstRequestContext requestContext = new MockHstRequestContext();
        requestContext.setResolvedMount(resolvedMount);
        
        String jaxrsPathInfo = "/demosite:news/" + containerRequest.getPathSuffix();
        
        PathsAdjustedHttpServletRequestWrapper jaxrsRequest = 
            new PathsAdjustedHttpServletRequestWrapper(requestContext, containerRequest, "", jaxrsPathInfo);
        
        assertEquals(jaxrsRequest.getPathInfo(), "/demosite:news/x/y/z");
        assertEquals(jaxrsRequest.getRequestURI(), "/app1/demosite:news/x;p1=1;p11=11/y;p2=2;p22=22/z;p3=3;p33=33");
        assertEquals(jaxrsRequest.getRequestURL().toString(), "http://www.example.org:8080/app1/demosite:news/x;p1=1;p11=11/y;p2=2;p22=22/z;p3=3;p33=33");
        
    }
    
}
