/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertTrue;

public class DefaultPipelineIT extends AbstractPipelineTestCase {

    protected HttpServletRequest servletRequest;
    protected HttpServletResponse servletResponse;

    @Before
    @Override
    public void setUp() throws Exception{
        super.setUp();
        this.servletRequest = mockRequest();
        this.servletResponse = mockResponse();
    }

    @Test
    public void testDefaultPipeline() throws ContainerException, UnsupportedEncodingException {
        
        ((MockHttpServletRequest)servletRequest).setPathInfo("/news");
        ((MockHttpServletRequest)servletRequest).addHeader("Host", servletRequest.getServerName());
        
        ((MockHttpServletRequest)servletRequest).setRequestURI(servletRequest.getContextPath() + servletRequest.getServletPath() + servletRequest.getPathInfo());
        

        HstRequestContext requestContext = resolveRequest(servletRequest, servletResponse);
       
        try {
            this.defaultPipeline.invoke(this.requestContainerConfig, requestContext, this.servletRequest, this.servletResponse);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            this.defaultPipeline.cleanup(this.requestContainerConfig, requestContext, this.servletRequest, this.servletResponse);
        }
        
        String content = ((MockHttpServletResponse) this.servletResponse).getContentAsString();
        assertTrue("The content of HTTP response is null or empty!", content != null && !"".equals(content.trim()));
    }

    @Test
    public void testDefaultPipeline2() throws ContainerException, UnsupportedEncodingException {
        
        ((MockHttpServletRequest)servletRequest).setPathInfo("/news/2009/februari");
        ((MockHttpServletRequest)servletRequest).addHeader("Host", servletRequest.getServerName());
        ((MockHttpServletRequest)servletRequest).setRequestURI(servletRequest.getContextPath() + servletRequest.getServletPath() + servletRequest.getPathInfo());
        
        HstRequestContext requestContext = resolveRequest(servletRequest, servletResponse);
      
        try {
            this.defaultPipeline.invoke(this.requestContainerConfig, requestContext, this.servletRequest, this.servletResponse);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            this.defaultPipeline.cleanup(this.requestContainerConfig, requestContext, this.servletRequest, this.servletResponse);
        }
        
        String content = ((MockHttpServletResponse) this.servletResponse).getContentAsString();
        assertTrue("The content of HTTP response is null or empty!", content != null && !"".equals(content.trim()));
    }

}
