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
package org.hippoecm.hst.core.container;

import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.container.HstContainerConfigImpl;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestDefaultPipeline extends AbstractSpringTestCase {

    protected HstComponentFactory componentFactory;
    protected Pipelines pipelines;
    protected Pipeline defaultPipeline;
    protected ServletConfig servletConfig;
    protected HstContainerConfig requestContainerConfig;
    protected HttpServletRequest servletRequest;
    protected HttpServletResponse servletResponse;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        HstServices.setComponentManager(getComponentManager());
        
        this.componentFactory = (HstComponentFactory) getComponent(HstComponentFactory.class.getName());
        this.pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        this.defaultPipeline = this.pipelines.getDefaultPipeline();
        this.servletConfig = (ServletConfig) getComponent(ServletConfig.class.getName());
        this.requestContainerConfig = new HstContainerConfigImpl(this.servletConfig.getServletContext(), getClass().getClassLoader());
        this.servletRequest = (HttpServletRequest) getComponent(HttpServletRequest.class.getName());
        this.servletResponse = (HttpServletResponse) getComponent(HttpServletResponse.class.getName());
     
        Map<String, Object> newsprops = new HashMap<String, Object>();
        newsprops.put("param1" , "${param1}");
        newsprops.put("param2" , "${param2}");
        //ComponentConfiguration newsCompConfig = new ComponentConfigurationImpl(newsprops); 
        HstComponentFactory componentFactory = getComponent(HstComponentFactory.class.getName());
        
        HstComponent component = new NewsOverview();
        //component.init(this.servletConfig, newsCompConfig);
        
       
        ((HstComponentFactoryImpl) componentFactory).componentRegistry.registerComponent(this.requestContainerConfig, "pages/newsoverview", component);

        component = new Header();
        //component.init(this.servletConfig, compConfig);
        
        ((HstComponentFactoryImpl) componentFactory).componentRegistry.registerComponent(this.requestContainerConfig, "pages/newsoverview/header", component);

        component = new DocumentTitle();
        //component.init(this.servletConfig, compConfig);
        
        ((HstComponentFactoryImpl) componentFactory).componentRegistry.registerComponent(this.requestContainerConfig, "pages/newsoverview/header/title", component);
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
    
    class NewsOverview extends GenericHstComponent {

        @Override
        public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
            super.doBeforeRender(request, response);
            
        }
        
    }
    
    class Header extends GenericHstComponent {
    }
    
    class DocumentTitle extends GenericHstComponent {
    }

}
