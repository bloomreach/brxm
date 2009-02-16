package org.hippoecm.hst.core.container;


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
    public void start() throws ContainerException;

    
    /**
     * Shuts down the engine and all associated services
     *
     * @throws Exception when the engine fails to shutdown
     */
    public void shutdown() throws ContainerException;

    /**
     * Gets the component manager
     */ 
    public ComponentManager getComponentManager();

}
