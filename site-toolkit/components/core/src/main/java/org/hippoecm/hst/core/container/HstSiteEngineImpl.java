package org.hippoecm.hst.core.container;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.HstSiteEngine;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSiteEngineImpl implements HstSiteEngine {

    protected ComponentManager componentManager;

    public HstSiteEngineImpl(ServletContext servletContext) {
        this.componentManager = new SpringComponentManager();
    }

    public void service(HstRequestContext context) throws Exception {
        Pipeline pipeline = getDefaultPipeline();
        pipeline.invoke(context);
    }

    public void shutdown() throws Exception {
        this.componentManager.stop();
    }

    public void start() throws Exception {
        this.componentManager.start();
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
