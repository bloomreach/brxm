package org.hippoecm.hst.core.container;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;

public class TestDefaultPipeline extends AbstractSpringTestCase {
    
    HttpServletRequest servletRequest;
    HttpServletResponse servletResponse;
    
    protected Pipelines pipelines;
    protected Pipeline defaultPipeline;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        this.defaultPipeline = this.pipelines.getDefaultPipeline();
        this.servletRequest = (HttpServletRequest) getComponent(HttpServletRequest.class.getName());
        this.servletResponse = (HttpServletResponse) getComponent(HttpServletResponse.class.getName());
    }
    
    @Test
    public void testDefaultPipeline() throws ContainerException {
        
        HstRequestContext context = new HstRequestContextImpl();
        
        this.defaultPipeline.beforeInvoke(servletRequest, servletResponse, context);
        
        try {
            this.defaultPipeline.invoke(servletRequest, servletResponse, context);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            this.defaultPipeline.afterInvoke(servletRequest, servletResponse, context);
        }
    }
    
}
