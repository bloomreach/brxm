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
package org.hippoecm.hst.component.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ServletContextAware;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ClientComponentManager implements ComponentManager, ServletContextAware, BeanPostProcessor {

    Logger logger = LoggerFactory.getLogger(ClientComponentManager.class);
    
    public static final String IGNORE_UNRESOLVABLE_PLACE_HOLDERS = ClientComponentManager.class.getName() + ".ignoreUnresolvablePlaceholders";
    
    public static final String SYSTEM_PROPERTIES_MODE = ClientComponentManager.class.getName() + ".systemPropertiesMode";
    
    protected AbstractRefreshableConfigApplicationContext applicationContext;
    protected String [] configurationResources;
    protected Configuration configuration;
    protected ServletContext servletContext;
    
    public ClientComponentManager() {
        this(null);
    }
    
    public ClientComponentManager(Configuration configuration) {
        this.configuration = configuration;
    }
    
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void initialize() {
        String [] configurationResources = getConfigurationResources();
        
        this.applicationContext = new ClassPathXmlApplicationContext() {
            // According to the javadoc of org/springframework/context/support/AbstractApplicationContext.html#postProcessBeanFactory,
            // this allows for registering special BeanPostProcessors.
            protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                beanFactory.addBeanPostProcessor(ClientComponentManager.this);
                
                if (servletContext != null) {
                    beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(servletContext));
                }
            }
        };
        
        ArrayList<String> checkedConfigurationResources = new ArrayList<String>();
        
        for (String configurationResource : configurationResources) {
            try {
                this.applicationContext.getResources(configurationResource);
                checkedConfigurationResources.add(configurationResource);
            } catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Ignoring resources on {}. It does not exist.", configurationResource);
                }
            }
        }
        
        if (checkedConfigurationResources.isEmpty()) {
            if (logger.isWarnEnabled()) {
                logger.warn("There's no valid component configuration.");
            }
        } else {
            this.applicationContext.setConfigLocations(checkedConfigurationResources.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
            
            if (configuration != null) {
                Properties initProps = ConfigurationConverter.getProperties(configuration);
                PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
                ppc.setIgnoreUnresolvablePlaceholders(configuration.getBoolean(IGNORE_UNRESOLVABLE_PLACE_HOLDERS, true));
                ppc.setSystemPropertiesMode(configuration.getInt(SYSTEM_PROPERTIES_MODE, PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK));
                ppc.setProperties(initProps);
                this.applicationContext.addBeanFactoryPostProcessor(ppc);
            } else if (getContainerConfiguration() != null) {
                Properties initProps = getContainerConfiguration().toProperties();
                PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
                ppc.setIgnoreUnresolvablePlaceholders(getContainerConfiguration().getBoolean(IGNORE_UNRESOLVABLE_PLACE_HOLDERS, true));
                ppc.setSystemPropertiesMode(getContainerConfiguration().getInt(SYSTEM_PROPERTIES_MODE, PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK));
                ppc.setProperties(initProps);
                this.applicationContext.addBeanFactoryPostProcessor(ppc);
            }
            
            this.applicationContext.refresh();
        }
    }

    public void start() {
        this.applicationContext.start();
    }

    public void stop() {
        this.applicationContext.stop();
    }
    
    public void close() {
        this.applicationContext.close();
    }
    
    public <T> T getComponent(String name) {
        return getComponent(name, (String []) null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name, String ... contextNames) {
        T bean = null;
        
        if (contextNames == null || contextNames.length == 0) {
            try {
                bean = (T) applicationContext.getBean(name);
            } catch (Exception ignore) {
            }
        }
        
        if (bean == null && servletContext != null) {
            WebApplicationContext rootWebAppContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
            
            if (rootWebAppContext != null) {
                try {
                    bean = (T) rootWebAppContext.getBean(name);
                } catch (Exception ignore) {
                }
            }
        }
        
        return bean;
    }
    
    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType) {
        return getComponentsOfType(requiredType, (String []) null);
    }

    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType, String ... contextNames) {
        Map<String, T> beansMap = Collections.emptyMap();
        
        if (contextNames == null || contextNames.length == 0) {
            try {
                beansMap = applicationContext.getBeansOfType(requiredType);
            } catch (Exception ignore) {
            }
        }
        
        if (MapUtils.isEmpty(beansMap) && servletContext != null) {
            WebApplicationContext rootWebAppContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
            
            if (rootWebAppContext != null) {
                try {
                    beansMap = rootWebAppContext.getBeansOfType(requiredType);
                } catch (Exception ignore) {
                }
            }
        }
        
        return beansMap;
    }

    public String[] getConfigurationResources() {
        return this.configurationResources;
    }
    
    public void setConfigurationResources(String [] configurationResources) {
        this.configurationResources = configurationResources;
    }

    public ContainerConfiguration getContainerConfiguration() {
        return HstServices.getComponentManager().getContainerConfiguration();
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ComponentManagerAware) {
            ((ComponentManagerAware) bean).setComponentManager(this);
        }
        
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean; 
    }
    
}
