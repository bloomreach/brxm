package org.hippoecm.hst.core.container;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class HstRequestProcessorImpl implements HstRequestProcessor {
    
    protected Pipelines pipelines;
    
    public HstRequestProcessorImpl(Pipelines pipelines) {
        this.pipelines = pipelines;
    }

    public void processRequest(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        Pipeline pipeline = this.pipelines.getDefaultPipeline();

        try {
            pipeline.beforeInvoke(servletConfig, servletRequest, servletResponse);
            pipeline.invoke(servletConfig, servletRequest, servletResponse);
        } catch (Throwable th) {
            throw new ContainerException(th);
        } finally {
            pipeline.afterInvoke(servletConfig, servletRequest, servletResponse);
        }
    }

}
