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
package org.hippoecm.hst.site.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.apache.commons.jexl.Script;
import org.apache.commons.jexl.ScriptFactory;
import org.apache.commons.lang.BooleanUtils;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * SpringComponentManager
 * 
 * @version $Id$
 */
public class SpringComponentManager implements ComponentManager, BeanPostProcessor {
    
    static Logger log = LoggerFactory.getLogger(SpringComponentManager.class);
    
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

    public SpringComponentManager() {
        this(null);
    }
    
    public SpringComponentManager(Configuration configuration) {
        this.configuration = configuration;
        this.containerConfiguration = new ContainerConfigurationImpl(this.configuration);
    }
    
    public void initialize() {
        String [] configurationResources = getConfigurationResources();
        
        this.applicationContext = new ClassPathXmlApplicationContext() {
            // According to the javadoc of org/springframework/context/support/AbstractApplicationContext.html#postProcessBeanFactory,
            // this allows for registering special BeanPostProcessors.
            @Override
            protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                beanFactory.addBeanPostProcessor(SpringComponentManager.this);
            }
            
            @Override
            protected DefaultListableBeanFactory createBeanFactory() {
                BeanFactory parentBeanFactory = getInternalParentBeanFactory();
                return new FilteringByExpressionListableBeanFactory(parentBeanFactory);
            }
        };
        
        ArrayList<String> checkedConfigurationResources = new ArrayList<String>();
        
        for (String configurationResource : configurationResources) {
            try {
                this.applicationContext.getResources(configurationResource);
                checkedConfigurationResources.add(configurationResource);
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring resources on {}. It does not exist.", configurationResource);
                }
            }
        }
        
        if (checkedConfigurationResources.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("There's no valid component configuration.");
            }
        } else {
            this.applicationContext.setConfigLocations(checkedConfigurationResources.toArray(new String [0]));
            
            if (configuration != null) {
                Properties initProps = ConfigurationConverter.getProperties(configuration);
                PropertyPlaceholderConfigurer ppc = new OverridingByAttributesPropertyPlaceholderConfigurer();
                ppc.setIgnoreUnresolvablePlaceholders(configuration.getBoolean(IGNORE_UNRESOLVABLE_PLACE_HOLDERS, true));
                ppc.setSystemPropertiesMode(configuration.getInt(SYSTEM_PROPERTIES_MODE, PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK));
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
    
    @SuppressWarnings("unchecked")
    public <T> T getComponent(String name) {
        T bean = null;
        
        try {
            bean = (T) this.applicationContext.getBean(name);
        } catch (Exception ignore) {
        }
        
        return bean;
    }
    
    public ContainerConfiguration getContainerConfiguration() {
        return this.containerConfiguration;
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

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ComponentManagerAware) {
            ((ComponentManagerAware) bean).setComponentManager(this);
        }
        
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean; 
    }
    
    private class FilteringByExpressionListableBeanFactory extends DefaultListableBeanFactory
    {
        private JexlContext jexlContext;
        
        @SuppressWarnings("unchecked")
        public FilteringByExpressionListableBeanFactory(BeanFactory parentBeanFactory)
        {
            super(parentBeanFactory);
            jexlContext = JexlHelper.createContext();
            jexlContext.getVars().put("config", containerConfiguration);
        }

        /**
         * Override of the registerBeanDefinition method to optionally filter out a BeanDefinition and
         * if requested dynamically register an bean alias
         */
        public void registerBeanDefinition(String beanName, BeanDefinition bd)
                throws BeanDefinitionStoreException
        {
            boolean registrable = true;
            String expression = (String) bd.getAttribute(BEAN_REGISTER_CONDITION);
            
            if (expression != null) {
                try {
                    Script jexlScript = ScriptFactory.createScript(expression);
                    Object result = jexlScript.execute(jexlContext);
                    
                    if (result == null) {
                        registrable = false;
                    } else if (result instanceof Boolean) {
                        registrable = BooleanUtils.toBoolean((Boolean) result);
                    } else if (result instanceof String) {
                        registrable = BooleanUtils.toBoolean((String) result);
                    } else if (result instanceof Integer) {
                        registrable = BooleanUtils.toBoolean(((Integer) result).intValue());
                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Expression execution error: " + expression, e);
                    } else {
                        log.warn("Expression execution error: {}. {}", expression, e);
                    }
                }
            }
            
            if (registrable) {
                super.registerBeanDefinition(beanName, bd);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Skipping the bean definition: " + bd);
                }
            }
        }
    }
    
}
