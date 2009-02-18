package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;

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
        
        HstComponentInvoker invoker = HstServices.getComponentInvoker();
        invoker.setServletContext(this.servletConfig.getServletContext());
        
        HstComponentFactory componentFactory = HstServices.getComponentFactory();
        ((HstComponentFactoryImpl) componentFactory).componentMap.put("pages/newsoverview", new NewsOverview());
        ((HstComponentFactoryImpl) componentFactory).componentMap.put("pages/newsoverview/header", new Header());
        ((HstComponentFactoryImpl) componentFactory).componentMap.put("pages/newsoverview/header/title", new DocumentTitle());
    }
    
    @Test
    public void testDefaultPipeline() throws ContainerException {
        
        this.defaultPipeline.beforeInvoke(this.servletRequest, this.servletResponse);
        
        try {
            this.defaultPipeline.invoke(this.servletRequest, this.servletResponse);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            this.defaultPipeline.afterInvoke(this.servletRequest, this.servletResponse);
        }
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
        
        public void init(ServletConfig servletConfig) throws HstComponentException {
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
