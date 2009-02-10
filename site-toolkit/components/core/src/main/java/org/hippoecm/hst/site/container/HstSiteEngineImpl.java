package org.hippoecm.hst.site.container;

import java.util.Properties;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstSiteEngine;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSiteEngineImpl implements HstSiteEngine {

    protected ComponentManager componentManager;

    public HstSiteEngineImpl() {
        this(null);
    }
    
    public HstSiteEngineImpl(Properties initProps) {
        this.componentManager = new SpringComponentManager(initProps);
    }

    public void service(ServletRequest request, ServletResponse response, HstRequestContext context) throws ContainerException {
        Pipeline pipeline = null;
        
        try {
            pipeline = getDefaultPipeline();
            pipeline.beforeInvoke(request, response, context);
            pipeline.invoke(request, response, context);
        } catch (Throwable th) {
            throw new ContainerException(th);
        } finally {
            if (pipeline != null)
                pipeline.afterInvoke(request, response, context);
        }
    }

    public void start() throws ContainerException {
        this.componentManager.initialize();
        this.componentManager.start();
    }

    public void shutdown() throws ContainerException {
        this.componentManager.stop();
        this.componentManager.close();
    }

    public ComponentManager getComponentManager() {
        return this.componentManager;
    }

    public Pipeline getDefaultPipeline() {
        return ((Pipelines) this.componentManager.getComponent(Pipelines.class.getName())).getDefaultPipeline();
    }

    public Pipeline getPipeline(String name) {
        return ((Pipelines) this.componentManager.getComponent(Pipelines.class.getName())).getPipeline(name);
    }
}
