package org.hippoecm.frontend.core;

import java.io.Serializable;
import java.util.List;


public interface PluginContext {

    String getProperty(String key);

    Plugin start(PluginConfig config);
    
    /**
     * Returns a reference to the service that has been configured to be available
     * under "name" for the plugin.
     * 
     * @param name
     * @return
     */
    List<Serializable> getServices(String name);

    /**
     * Registers a service with the given name.
     * 
     * @param name
     * @param service
     */
    void registerService(Serializable service, String name);

    /**
     * Registers a service under the given name.
     * 
     * @param name
     * @param service
     */
    void unregisterService(Serializable service, String name);

    /**
     * Registers a service with the given name.
     * 
     * @param name
     * @param service
     */
    void registerListener(ServiceListener listener, String name);

    /**
     * Unregisters a service with the given name.
     * 
     * @param name
     * @param service
     */
    void unregisterListener(ServiceListener listener, String name);

}
