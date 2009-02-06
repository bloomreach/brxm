package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Engine Abstraction - to run from both unit tests and servlet
 *
 * @author <a href="mailto:w.ko@onehippo.com">Woonsan Ko</a>
 * @version $Id$
 */
public interface HstSiteEngine
{
    /**
     * Initializes the engine with a commons configuration, starting all early initable services.
     *
     * @throws Exception when the engine fails to initilialize
     */
    public void start() throws Exception;

    
    /**
     * Shuts down the engine and all associated services
     *
     * @throws Exception when the engine fails to shutdown
     */
    public void shutdown() throws Exception;

    /**
     * Makes a service request to the engine.
     *
     * @param context a <code>RequestContext</code> with the state of the request.
     * @throws Exception when the engine fails to initilialize
     */
    public void service(ServletRequest request, ServletResponse response, HstRequestContext context) throws Exception;

    /**
     * Gets the engine's request default pipeline.
     * 
     * @return Pipeline The engine's request pipeline.
     */
    public Pipeline getDefaultPipeline();
 
    /**
     * Gets the specified engine's request pipeline.
     * 
     * @return Pipeline A specific request pipeline.
     */ 
    public Pipeline getPipeline(String pipelineName);
 
    /**
     * Gets the component manager
     */ 
    public ComponentManager getComponentManager();

}
