/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * The factory interface which is responsible for creating {@link HstComponentWindow} instances.
 * 
 * @version $Id$
 */
public interface HstComponentWindowFactory {
    
    /**
     * Sets the reference namespace separator.
     * If this is set to '_' and the namespace components are 'a', 'b' and 'c', then
     * the reference namespace should be 'a_b_c'.
     * 
     * @param referenceNameSeparator
     */
    void setReferenceNameSeparator(String referenceNameSeparator);
    
    /**
     * Returns the reference namespace separator.
     * @return
     */
    String getReferenceNameSeparator();

    /**
     * Creates a {@link HstComponentWindow} instance.
     * 
     * @param requestContainerConfig the container configuration
     * @param requestContext the {@link HstRequestContext} instance for the currrent request
     * @param compConfig the component configuration
     * @param compFactory the {@link HstComponentFactory} instance for this container
     * @return an instance of {@link HstComponentWindow}
     * @throws HstComponentException
     */
    HstComponentWindow create(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory) throws HstComponentException;

    /**
     * Creates a {@link HstComponentWindow} instance as a child window of the parentWindow.
     * 
     * @param requestContainerConfig the container configuration
     * @param requestContext the {@link HstRequestContext} instance for the currrent request
     * @param compConfig the component configuration
     * @param compFactory the {@link HstComponentFactory} instance for this container
     * @param parentWindow the parent window
     * @return an instance of {@link HstComponentWindow}
     * @throws HstComponentException
     */
    HstComponentWindow create(HstContainerConfig requestContainerConfig, HstRequestContext requestContext, HstComponentConfiguration compConfig, HstComponentFactory compFactory, HstComponentWindow parentWindow) throws HstComponentException;
    
}
