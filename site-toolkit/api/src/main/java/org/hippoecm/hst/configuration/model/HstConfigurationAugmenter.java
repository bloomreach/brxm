/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.model;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ContainerException;

/**
 * Classes that implement this interface can be used to augment the loaded configuration. The implementations will get the 
 * {@link #augment(HstManager)} called by the {@link HstManager} <b>after</b> the {@link VirtualHosts} object is 
 * completely loaded and all configuration has been enhanced.
 * 
 *  
 */
public interface HstConfigurationAugmenter {

    /**
     * Implementations that are 
     * @param manager
     * @throws ContainerException
     */
    void augment(HstManager manager) throws ContainerException;
}
