package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class HstRequestProcessorImpl implements HstRequestProcessor {
    
    protected Pipelines pipelines;
    
    public HstRequestProcessorImpl(Pipelines pipelines) {
        this.pipelines = pipelines;
    }

    public void processRequest(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        Pipeline pipeline = this.pipelines.getDefaultPipeline();

        try {
            pipeline.beforeInvoke(servletRequest, servletResponse);
            pipeline.invoke(servletRequest, servletResponse);
        } catch (Throwable th) {
            throw new ContainerException(th);
        } finally {
            pipeline.afterInvoke(servletRequest, servletResponse);
        }
    }

}
