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

/**
 * Interface which a component bean should implement if it wants to
 * have access the component manager containing itself.
 * If a component which is initialized by {@link ComponentManager} implements
 * this interface, then it will be given the component manager at the 
 * initialization time. The component manager will invoke {@link #setComponentManager(ComponentManager)}
 * method to give itself to the component.
 * 
 * @version $Id$
 */
public interface ComponentManagerAware {
    
    /**
     * Sets the component manager
     * 
     * @param componentManager
     */
    void setComponentManager(ComponentManager componentManager);
    
}
