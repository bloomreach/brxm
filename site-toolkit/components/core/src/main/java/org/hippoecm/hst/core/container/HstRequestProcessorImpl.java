package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class HstRequestProcessorImpl implements HstRequestProcessor {
    
    protected Pipelines pipelines;
    
    public HstRequestProcessorImpl(Pipelines pipelines) {
        this.pipelines = pipelines;
    }

    public void processRequest(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        Pipeline pipeline = null;

        try {
            pipeline = this.pipelines.getDefaultPipeline();
            pipeline.beforeInvoke(servletRequest, servletResponse);
            pipeline.invoke(servletRequest, servletResponse);
        } catch (Throwable th) {
            throw new ContainerException(th);
        } finally {
            if (pipeline != null)
                pipeline.afterInvoke(servletRequest, servletResponse);
        }
    }

}
