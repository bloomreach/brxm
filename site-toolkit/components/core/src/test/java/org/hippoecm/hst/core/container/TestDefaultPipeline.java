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

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfigurationBean;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestDefaultPipeline extends AbstractSpringTestCase {

    protected HstComponentFactory componentFactory;
    protected Pipelines pipelines;
    protected Pipeline defaultPipeline;
    protected ServletConfig servletConfig;
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
        this.servletRequest = (HttpServletRequest) getComponent(HttpServletRequest.class.getName());
        this.servletResponse = (HttpServletResponse) getComponent(HttpServletResponse.class.getName());
        
        HstComponentFactory componentFactory = HstServices.getComponentFactory();
        ((HstComponentFactoryImpl) componentFactory).componentRegistry.registerComponent(this.servletConfig, "pages/newsoverview", new NewsOverview());
        ((HstComponentFactoryImpl) componentFactory).componentRegistry.registerComponent(this.servletConfig, "pages/newsoverview/header", new Header());
        ((HstComponentFactoryImpl) componentFactory).componentRegistry.registerComponent(this.servletConfig, "pages/newsoverview/header/title", new DocumentTitle());
    }
    
    @Test
    public void testDefaultPipeline() throws ContainerException, UnsupportedEncodingException {
        
        this.defaultPipeline.beforeInvoke(this.servletConfig, this.servletRequest, this.servletResponse);
        
        try {
            this.defaultPipeline.invoke(this.servletConfig, this.servletRequest, this.servletResponse);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            this.defaultPipeline.afterInvoke(this.servletConfig, this.servletRequest, this.servletResponse);
        }
        
        String content = ((MockHttpServletResponse) this.servletResponse).getContentAsString();
        assertTrue("The content of HTTP response is null or empty!", content != null && !"".equals(content.trim()));
        System.out.println("[HTTP Response] >>> " + content + " <<<");
    }
    
    class HstComponentBase implements HstComponent {
        
        protected String name;
        
        public HstComponentBase() {
        }
        
        public String getName() {
            if (this.name == null) {
                this.name = getClass().getName();
            }
            
            return this.name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public void init(ServletConfig servletConfig, HstComponentConfigurationBean compConfig) throws HstComponentException {
            System.out.println("[HstComponent: " + getName() + "] init()");
        }

        public void destroy() throws HstComponentException {
            System.out.println("[HstComponent: " + getName() + "] destroy()");
        }

        public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
            System.out.println("[HstComponent: " + getName() + "] doAction()");
        }

        public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
            System.out.println("[HstComponent: " + getName() + "] doBeforeRender()");
        }

        public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
            System.out.println("[HstComponent: " + getName() + "] doBeforeServeResource()");
        }
    }
    
    class NewsOverview extends HstComponentBase {
    }
    
    class Header extends HstComponentBase {
    }
    
    class DocumentTitle extends HstComponentBase {
    }

}
