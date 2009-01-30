package org.hippoecm.hst.core.container.engine;

import javax.servlet.ServletContext;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.pipeline.Pipeline;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSiteEngineImpl implements HstSiteEngine
{
    public HstSiteEngineImpl(ServletContext servletContext, ComponentManager componentManager)
    {
        // TODO Auto-generated constructor stub
    }

    public void service(HstRequestContext context) throws Exception
    {
        // TODO Auto-generated method stub
    }

    public void shutdown() throws Exception
    {
        // TODO Auto-generated method stub
    }

    public void start() throws Exception
    {
        // TODO Auto-generated method stub
    }

    public ComponentManager getComponentManager()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Pipeline getPipeline()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Pipeline getPipeline(String pipelineName)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
