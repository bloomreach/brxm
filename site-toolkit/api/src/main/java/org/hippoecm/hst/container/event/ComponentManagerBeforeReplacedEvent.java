/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.container.event;

import java.util.EventObject;

import org.hippoecm.hst.core.container.ComponentManager;

/**
 * Published by the component which is responsible for loading ComponentManager just before trying to replace
 * the old component manager by the new component manager.
 */
public class ComponentManagerBeforeReplacedEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public ComponentManagerBeforeReplacedEvent(ComponentManager oldComponentManager) {
        super(oldComponentManager);
    }

    public ComponentManager getComponentManager() {
        return (ComponentManager) getSource();
    }
}