/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

}
