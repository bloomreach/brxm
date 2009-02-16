package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponentContext;
import org.hippoecm.hst.core.component.HstComponentFactory;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.HstComponentInvokerImpl;
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
        
        HstComponentInvokerProvider invokerProvider = HstServices.getComponentInvokerProvider();
        HstComponentInvoker invoker = new HstComponentInvokerImpl(servletConfig.getServletContext(), "hstdispatch");
        invokerProvider.registerComponentInvoker(HstComponentContext.LOCAL_COMPONENT_CONTEXT_NAME, invoker);
        
        HstComponentFactory componentFactory = HstServices.getComponentFactory();
        componentFactory.registerComponentContext(HstComponentContext.LOCAL_COMPONENT_CONTEXT_NAME, servletConfig, Thread.currentThread().getContextClassLoader());
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
    
}
