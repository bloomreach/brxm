package org.hippoecm.hst.core.container.engine;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.pipeline.Pipeline;
import org.hippoecm.hst.core.container.pipeline.Pipelines;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSiteEngineImpl implements HstSiteEngine
{
    
    protected ComponentManager componentManager;
    
    public HstSiteEngineImpl(ServletContext servletContext, ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public void service(HstRequestContext context) throws Exception
    {
        Pipeline pipeline = getDefaultPipeline();
        pipeline.invoke(context);
    }

    public void shutdown() throws Exception
    {
    }

    public void start() throws Exception
    {
    }

    public ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    public Pipeline getDefaultPipeline()
    {
        return ((Pipelines) this.componentManager.getComponent(Pipelines.class.getName())).getDefaultPipeline();
    }

    public Pipeline getPipeline(String name)
    {
        return ((Pipelines) this.componentManager.getComponent(Pipelines.class.getName())).getPipeline(name);
    }
}
