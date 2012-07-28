/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.addon.module;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

public interface ModuleInstance {

    public String getName();

    public String getFullName();

    public void initialize();

    public void start();

    public void stop();

    public void close();

    public <T> T getComponent(String name);
    
    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType);

    public ModuleInstance getModuleInstance(String name);

    public List<ModuleInstance> getModuleInstances();

    /**
     * Publish the given event to all components which wants to listen to.
     * Note that an implementation may decide to support specific child types of <CODE>EventObject</CODE> objects only.
     * Spring Framework based implementations can support Spring Framework's <CODE>ApplicationEvent</CODE> objects only, for instance.
     * If an implementation doesn't support the specific type of EventObject objects, then the EventObject object will be just ignored.
     * @param event the event to publish (may be an application-specific or built-in HST-2 event)
     */
    public void publishEvent(EventObject event);

}
