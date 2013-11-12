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
package org.hippoecm.hst.site.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import com.google.common.eventbus.EventBus;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.addon.module.ModuleInstance;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.hippoecm.hst.core.container.ModuleNotFoundException;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.addon.module.runtime.ModuleInstanceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;

/**
 * SpringComponentManager
 * 
 * @version $Id$
 */
public class SpringComponentManager implements ComponentManager {
    
    private static final String LOGGER_FQCN = SpringComponentManager.class.getName();
    private static Logger log = LoggerFactory.getLogger(LOGGER_FQCN);
    
    public static final String IGNORE_UNRESOLVABLE_PLACE_HOLDERS = SpringComponentManager.class.getName() + ".ignoreUnresolvablePlaceholders";
    
    public static final String SYSTEM_PROPERTIES_MODE = SpringComponentManager.class.getName() + ".systemPropertiesMode";
    
    public static final String BEAN_REGISTER_CONDITION = SpringComponentManager.class.getName() + ".registerCondition";
    
    protected AbstractRefreshableConfigApplicationContext applicationContext;
    protected Configuration configuration;
    protected ContainerConfiguration containerConfiguration;
    protected String [] configurationResources = new String[] {
            SpringComponentManager.class.getName().replace(".", "/") + ".xml", 
            SpringComponentManager.class.getName().replace(".", "/") + "-*.xml"
            };

    private List<ModuleDefinition> addonModuleDefinitions;
    private Map<String, ModuleInstance> addonModuleInstancesMap;
    private List<ModuleInstance> addonModuleInstancesList;

    private ServletConfig servletConfig;
    private ServletContext servletContext;

    private EventBus containerEventBus = new EventBus();

    public SpringComponentManager() {
        this(new PropertiesConfiguration());
    }
    
    public SpringComponentManager(Configuration configuration) {
        this.configuration = configuration;
        this.containerConfiguration = new ContainerConfigurationImpl(this.configuration);
    }

    public SpringComponentManager(ServletConfig servletConfig, Configuration configuration) {
        this(configuration);
        setServletConfig(servletConfig);
    }

    public SpringComponentManager(ServletContext servletContext, Configuration configuration) {
        this(configuration);
        setServletContext(servletContext);
    }

    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
        if (servletConfig != null) {
            setServletContext(servletConfig.getServletContext());
        }
    }

    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }
    
    public void initialize() {
        applicationContext = new DefaultComponentManagerApplicationContext(this, containerConfiguration);
        
        String [] checkedConfigurationResources = ApplicationContextUtils.getCheckedLocationPatterns(applicationContext, getConfigurationResources());
        
        if (ArrayUtils.isEmpty(checkedConfigurationResources)) {
            log.warn("There's no valid component configuration.");
        }
        
        applicationContext.setConfigLocations(checkedConfigurationResources);
        applicationContext.refresh();

        if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
            addonModuleInstancesMap = Collections.synchronizedMap(new HashMap<String, ModuleInstance>());
            
            for (ModuleDefinition addonModuleDefinition : addonModuleDefinitions) {
                ModuleInstance addonModuleInstance = new ModuleInstanceImpl(addonModuleDefinition);
                
                if (addonModuleInstance instanceof ComponentManagerAware) {
                    ((ComponentManagerAware) addonModuleInstance).setComponentManager(this);
                }
                
                if (addonModuleInstance instanceof ApplicationContextAware) {
                    ((ApplicationContextAware) addonModuleInstance).setApplicationContext(applicationContext);
                }
                
                try {
                    log.info("Initializing addon module, {}", addonModuleInstance.getFullName());
                    addonModuleInstance.initialize();
                    addonModuleInstancesMap.put(addonModuleInstance.getName(), addonModuleInstance);
                } catch (Exception e) {
                    log.warn("Failed to initialize invalid module instance, " + addonModuleInstance.getFullName() + ", which will be just closed and ignored.", e);
                    
                    try {
                        addonModuleInstance.close();
                    } catch (Exception ce) {
                        log.warn("Failed to close invalid module instance, " + addonModuleInstance.getFullName() + ".", e);
                    }
                }
            }

            synchronized (addonModuleInstancesMap) {
                addonModuleInstancesList = Collections.synchronizedList(new ArrayList<ModuleInstance>(addonModuleInstancesMap.values()));
            }
        }
    }

    public void start() {
        applicationContext.start();
        
        List<ModuleInstance> failedModuleInstances = new ArrayList<ModuleInstance>();
        
        for (ModuleInstance addonModuleInstance : getAddonModuleInstances()) {
            try {
                log.info("Starting addon module, {}", addonModuleInstance.getFullName());
                addonModuleInstance.start();
            } catch (Exception e) {
                log.warn("Failed to start module instance, " + addonModuleInstance.getFullName() + ", which will be just removed.", e);
                failedModuleInstances.add(addonModuleInstance);
            }
        }
        
        for (ModuleInstance addonModuleInstance : failedModuleInstances) {
            try {
                addonModuleInstance.stop();
            } catch (Exception e) {
                log.warn("Failed to stop non-starting module instance, " + addonModuleInstance.getFullName() + ".", e);
            }
            
            try {
                addonModuleInstance.close();
            } catch (Exception e) {
                log.warn("Failed to close non-starting module instance, " + addonModuleInstance.getFullName() + ".", e);
            }
            
            addonModuleInstancesMap.remove(addonModuleInstance.getName());
            addonModuleInstancesList.remove(addonModuleInstance);
        }
    }

    public void stop() {
        for (ModuleInstance addonModuleInstance : getAddonModuleInstances()) {
            try {
                log.info("Stopping addon module, {}", addonModuleInstance.getFullName());
                addonModuleInstance.stop();
            } catch (Exception e) {
                log.warn("Failed to stop module instance, " + addonModuleInstance.getFullName() + ".", e);
            }
        }
        
        applicationContext.stop();
    }
    
    public void close() {
        for (ModuleInstance addonModuleInstance : getAddonModuleInstances()) {
            try {
                log.info("Closing addon module, {}", addonModuleInstance.getFullName());
                addonModuleInstance.close();
            } catch (Exception e) {
                log.warn("Failed to close module instance, " + addonModuleInstance.getFullName() + ".", e);
            }
        }
        
        applicationContext.close();
    }
    
    public <T> T getComponent(String name) {
        return getComponent(name, (String []) null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name, String ... addonModuleNames) {
        T bean = null;

        if (addonModuleNames == null || addonModuleNames.length == 0) {
            try {
                bean = (T) applicationContext.getBean(name);
            } catch (Exception e) {
                log.warn("The requested bean doesn't exist: '{}'", name);
            }
        } else {
            if (addonModuleInstancesMap == null || addonModuleInstancesMap.isEmpty()) {
                throw new ModuleNotFoundException("No Addon Module is found.");
            }
            
            ModuleInstance moduleInstance = addonModuleInstancesMap.get(addonModuleNames[0]);

            if (moduleInstance == null) {
                throw new ModuleNotFoundException("Module is not found: '" + addonModuleNames[0] + "'");
            }

            for (int i = 1; i < addonModuleNames.length; i++) {
                moduleInstance = moduleInstance.getModuleInstance(addonModuleNames[i]);

                if (moduleInstance == null) {
                    throw new ModuleNotFoundException("Module is not found in '" + ArrayUtils.toString(ArrayUtils.subarray(addonModuleNames, 0, i + 1)) + "'");
                }
            }

            try {
                bean = (T) moduleInstance.getComponent(name);
            } catch (Exception e) {
                log.warn("The requested bean doesn't exist: '{}' in the addon module context, '{}'.",
                         name, ArrayUtils.toString(addonModuleNames));
            }
        }

        return bean;
    }

    public <T> T getComponent(Class<T> requiredType) throws ComponentsException {
        return getComponent(requiredType, (String []) null);
    }

    public <T> T getComponent(Class<T> requiredType, String ... addonModuleNames) throws ComponentsException {
        T bean = null;

        if (addonModuleNames == null || addonModuleNames.length == 0) {
            try {
                bean = (T) applicationContext.getBean(requiredType);
            } catch (Exception e) {
                log.warn("The requested bean doesn't exist by the required type: '{}'", requiredType);
            }
        } else {
            if (addonModuleInstancesMap == null || addonModuleInstancesMap.isEmpty()) {
                throw new ModuleNotFoundException("No Addon Module is found.");
            }
            
            ModuleInstance moduleInstance = addonModuleInstancesMap.get(addonModuleNames[0]);

            if (moduleInstance == null) {
                throw new ModuleNotFoundException("Module is not found: '" + addonModuleNames[0] + "'");
            }

            for (int i = 1; i < addonModuleNames.length; i++) {
                moduleInstance = moduleInstance.getModuleInstance(addonModuleNames[i]);

                if (moduleInstance == null) {
                    throw new ModuleNotFoundException("Module is not found in '" + ArrayUtils.toString(ArrayUtils.subarray(addonModuleNames, 0, i + 1)) + "'");
                }
            }

            try {
                bean = (T) moduleInstance.getComponent(requiredType);
            } catch (Exception e) {
                log.warn("The requested bean doesn't exist by the required type: '{}' in the addon module context, '{}'.",
                        requiredType, ArrayUtils.toString(addonModuleNames));
            }
        }

        if (bean == null) {
            throw new ComponentsException("No component found, not exactly matching a single component by the specified type, " + requiredType);
        }

        return bean;
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType) {
        return getComponentsOfType(requiredType, (String []) null);
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType, String ... addonModuleNames) {
        Map<String, T> beansMap = Collections.emptyMap();

        if (addonModuleNames == null || addonModuleNames.length == 0) {
            try {
                beansMap = applicationContext.getBeansOfType(requiredType);
            } catch (Exception e) {
                log.warn("The required typed bean doesn't exist: '{}'", requiredType);
            }
        } else {
            if (addonModuleInstancesMap == null || addonModuleInstancesMap.isEmpty()) {
                throw new ModuleNotFoundException("No Addon Module is found.");
            }
            
            ModuleInstance moduleInstance = addonModuleInstancesMap.get(addonModuleNames[0]);

            if (moduleInstance == null) {
                throw new ModuleNotFoundException("Module is not found: '" + addonModuleNames[0] + "'");
            }

            for (int i = 1; i < addonModuleNames.length; i++) {
                moduleInstance = moduleInstance.getModuleInstance(addonModuleNames[i]);

                if (moduleInstance == null) {
                    throw new ModuleNotFoundException("Module is not found in '" + ArrayUtils.toString(ArrayUtils.subarray(addonModuleNames, 0, i + 1)) + "'");
                }
            }

            try {
                beansMap = moduleInstance.getComponentsOfType(requiredType);
            } catch (Exception e) {
                log.warn("The required typed bean doesn't exist: '{}' in the addon module context, '{}'.", 
                        requiredType, ArrayUtils.toString(addonModuleNames));
            }
        }

        return beansMap;
    }

    public ContainerConfiguration getContainerConfiguration() {
        return containerConfiguration;
    }

    public String[] getConfigurationResources() {
        if (configurationResources == null) {
            return null;
        }
        
        String [] cloned = new String[configurationResources.length];
        System.arraycopy(configurationResources, 0, cloned, 0, configurationResources.length);
        return cloned;
    }
    
    public void setConfigurationResources(String [] configurationResources) {
        if (configurationResources == null) {
            this.configurationResources = null;
        } else {
            this.configurationResources = new String[configurationResources.length];
            System.arraycopy(configurationResources, 0, this.configurationResources, 0, configurationResources.length);
        }
    }

    public void publishEvent(EventObject event) {
        containerEventBus.post(event);
    }

    public void registerEventSubscriber(Object subscriber) {
        containerEventBus.register(subscriber);
    }

    public void unregisterEventSubscriber(Object subscriber) {
        containerEventBus.unregister(subscriber);
    }

    public void setAddonModuleDefinitions(List<ModuleDefinition> addonModuleDefinitions) {
        if (addonModuleDefinitions == null) {
            this.addonModuleDefinitions = null;
        } else {
            this.addonModuleDefinitions = new ArrayList<ModuleDefinition>(addonModuleDefinitions);
        }
    }
    
    private List<ModuleInstance> getAddonModuleInstances() {
        if (addonModuleInstancesList == null) {
            return Collections.emptyList();
        }
        
        List<ModuleInstance> addonModuleInstances = null;
        
        synchronized (addonModuleInstancesList) {
            addonModuleInstances = new ArrayList<ModuleInstance>(addonModuleInstancesList);
        }
        
        return addonModuleInstances;
    }
}
