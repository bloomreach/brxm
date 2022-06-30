/*
 * Copyright 2008-2022 Bloomreach
 */
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentMetadata;

/**
 * The HstComponent registry interface
 * 
 * @version $Id$
 */
public interface HstComponentRegistry {

    /**
     * Registers the HstComponent. The key is the pair of container configuration and component ID.
     * 
     * @param requestContainerConfig the container configuration
     * @param componentId the component ID
     * @param component
     */
    void registerComponent(HstContainerConfig requestContainerConfig, String componentId, HstComponent component);
    
    /**
     * Unregister the HstComponent. The key is the pair of container configuration and component ID.
     * 
     * @param requestContainerConfig the container configuration
     * @param componentId the component ID
     */
    void unregisterComponent(HstContainerConfig requestContainerConfig, String componentId);
    
    /**
     * Returns the registered HstComponent. The key is the pair of container configuration and component ID.
     * <P>
     * If the component is not found, then it will return null.
     * </P>
     * 
     * @param requestContainerConfig the container configuration
     * @param componentId the component ID
     * @return the HstComponent registered with the key pair.
     */
    HstComponent getComponent(HstContainerConfig requestContainerConfig, String componentId);
    
    /**
     * Returns the metadata of the registered HstComponent. The key is the pair of container configuration and component ID.
     * <P>
     * If the component metadata is not found, then it will return null.
     * </P>
     * 
     * @param requestContainerConfig the container configuration
     * @param componentId the component ID
     * @return the metadata of the HstComponent registered with the key pair.
     */
    HstComponentMetadata getComponentMetadata(HstContainerConfig requestContainerConfig, String componentId);
    
    /**
     * Unregisters all the HstComponents.
     */
    void unregisterAllComponents();

    /**
     * @return {@code true} if this {@link HstComponentRegistry} is waiting for termination. Note that the registry
     * functions as normal when this returns {@code true}. This method should return true if the backing
     * {@link org.hippoecm.hst.configuration.hosting.VirtualHosts} model is belongs too is invalidated and is about
     * to be garbage collected
     */
    boolean isAwaitingTermination();
}
