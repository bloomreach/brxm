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
package org.hippoecm.hst.site.addon.module.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.addon.module.ModuleInstance;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ApplicationContextUtils;
import org.hippoecm.hst.site.container.DefaultComponentManagerApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;

public class ModuleInstanceImpl implements ModuleInstance, ComponentManagerAware, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(ModuleInstanceImpl.class);

    private ModuleDefinition moduleDefinition;
    private String name;
    private String fullName;
    
    private ApplicationContext parentApplicationContext;
    private AbstractRefreshableConfigApplicationContext applicationContext;
    private ComponentManager componentManager;
    
    private Map<String, ModuleInstance> childModuleInstancesMap;
    private List<ModuleInstance> childModuleInstancesList;

    public ModuleInstanceImpl(ModuleDefinition moduleDefinition) {
        this(moduleDefinition, null);
    }

    public ModuleInstanceImpl(ModuleDefinition moduleDefinition, String [] namePrefixes) {
        this.moduleDefinition = moduleDefinition;
        this.name = this.moduleDefinition.getName();
        String [] mergedNamePrefixes = (String []) ArrayUtils.add(namePrefixes, this.name);
        this.fullName = ArrayUtils.toString(mergedNamePrefixes);
        
        List<ModuleDefinition> childModuleDefinitions = this.moduleDefinition.getModuleDefinitions();
        
        if (childModuleDefinitions != null && !childModuleDefinitions.isEmpty()) {
            childModuleInstancesMap = Collections.synchronizedMap(new HashMap<String, ModuleInstance>());
            
            for (ModuleDefinition childModuleDefinition : childModuleDefinitions) {
                ModuleInstance childModuleInstance = new ModuleInstanceImpl(childModuleDefinition, mergedNamePrefixes);
                childModuleInstancesMap.put(childModuleInstance.getName(), childModuleInstance);
            }
            
            synchronized (childModuleInstancesMap) {
                childModuleInstancesList = Collections.synchronizedList(new ArrayList<ModuleInstance>(childModuleInstancesMap.values()));
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public void setApplicationContext(ApplicationContext parentApplicationContext) throws BeansException {
        this.parentApplicationContext = parentApplicationContext;
    }

    public void initialize() {
        applicationContext = new DefaultComponentManagerApplicationContext(componentManager.getContainerConfiguration(), parentApplicationContext);
        
        if (componentManager != null && applicationContext instanceof ComponentManagerAware) {
            ((ComponentManagerAware) applicationContext).setComponentManager(componentManager);
        }
        
        String [] checkedConfigurationResources = ApplicationContextUtils.getCheckedLocationPatterns(applicationContext, moduleDefinition.getConfigLocations());

        if (ArrayUtils.isEmpty(checkedConfigurationResources)) {
            log.warn("There's no valid component configuration for addon module, '{}'.", name);
        }
        
        applicationContext.setConfigLocations(checkedConfigurationResources);
        applicationContext.refresh();

        for (ModuleInstance moduleInstance : getModuleInstances()) {
            if (componentManager != null && moduleInstance instanceof ComponentManagerAware) {
                ((ComponentManagerAware) moduleInstance).setComponentManager(componentManager);
            }
            
            if (componentManager != null && moduleInstance instanceof ApplicationContextAware) {
                ((ApplicationContextAware) moduleInstance).setApplicationContext(applicationContext);
            }
            
            moduleInstance.initialize();
        }
    }

    public void start() {
        applicationContext.start();
        
        for (ModuleInstance moduleInstance : getModuleInstances()) {
            moduleInstance.start();
        }
    }

    public void stop() {
        for (ModuleInstance moduleInstance : getModuleInstances()) {
            moduleInstance.stop();
        }
        
        applicationContext.stop();
    }
    
    public void close() {
        for (ModuleInstance moduleInstance : getModuleInstances()) {
            moduleInstance.close();
        }
        
        applicationContext.close();
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name) {
        T bean = null;

        try {
            bean = (T) applicationContext.getBean(name);
        } catch (Exception ignore) {
            log.warn("The requested bean doesn't exist: '{}'", name);
        }
        
        return bean;
    }

    public <T> T getComponent(Class<T> requiredType) {
        T bean = null;

        try {
            bean = (T) applicationContext.getBean(requiredType);
        } catch (Exception ignore) {
            log.warn("The requested bean doesn't exist by the required type: '{}'", requiredType);
        }

        if (bean == null) {
            throw new ComponentsException("No component found, not exactly matching a single component by the specified type, " + requiredType);
        }

        return bean;
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType) {
        Map<String, T> beansMap = Collections.emptyMap();

        try {
            beansMap = applicationContext.getBeansOfType(requiredType);
        } catch (Exception ignore) {
            log.warn("The requested bean doesn't exist: '{}'", name);
        }
        
        return beansMap;
    }

    public List<ModuleInstance> getModuleInstances() {
        if (childModuleInstancesList == null) {
            return Collections.emptyList();
        }
        
        List<ModuleInstance> moduleInstances = null;
        
        synchronized (childModuleInstancesList) {
            moduleInstances = new ArrayList<ModuleInstance>(childModuleInstancesList);
        }
        
        return moduleInstances;
    }

    public ModuleInstance getModuleInstance(String name) {
        if (childModuleInstancesMap == null) {
            return null;
        }
        
        return childModuleInstancesMap.get(name);
    }
}
