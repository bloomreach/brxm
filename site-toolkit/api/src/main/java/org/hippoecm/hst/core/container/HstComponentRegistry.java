/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
    
}
