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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;

/**
 * The factory interface which is responsible for creating HstComponent instances.
 * 
 * @version $Id$
 */
public interface HstComponentFactory {
    
    /**
     * Returns the HstComponent instance from the HstComponent context.
     * 
     * @param requestContainerConfig the HstContainer configuration
     * @param compConfig the HstComponent configuration
     * @param mount the Mount to create the component instance for
     * @return the instance of the HstComponent
     * @throws HstComponentException
     */
    HstComponent getComponentInstance(HstContainerConfig requestContainerConfig, HstComponentConfiguration compConfig, Mount mount) throws HstComponentException;
    
    /**
     * Returns arbitrary object instance from the HstComponent context.
     * 
     * @param <T>
     * @param requestContainerConfig
     * @param className
     * @return
     * @throws HstComponentException
     */
    <T> T getObjectInstance(HstContainerConfig requestContainerConfig, String className) throws HstComponentException;
    
    /**
     * Returns the default HST Component class name
     * @return
     */
    String getDefaultHstComponentClassName();
}
