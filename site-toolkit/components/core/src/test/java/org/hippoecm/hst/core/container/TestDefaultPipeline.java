package org.hippoecm.hst.core.container;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Before;
import org.junit.Test;

public class TestDefaultPipeline extends AbstractSpringTestCase {

    protected Repository repository;
    protected Credentials defaultCredentials;
    protected Pipelines pipelines;
    protected Pipeline defaultPipeline;
    protected HttpServletRequest servletRequest;
    protected HttpServletResponse servletResponse;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.repository = (Repository) getComponent(Repository.class.getName());
        this.defaultCredentials = (Credentials) getComponent(Credentials.class.getName() + ".default");
        this.pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        this.defaultPipeline = this.pipelines.getDefaultPipeline();
        this.servletRequest = (HttpServletRequest) getComponent(HttpServletRequest.class.getName());
        this.servletResponse = (HttpServletResponse) getComponent(HttpServletResponse.class.getName());
    }
    
    @Test
    public void testDefaultPipeline() throws ContainerException {
        
        HstRequestContext context = new HstRequestContextImpl(this.repository, this.defaultCredentials);
        
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
