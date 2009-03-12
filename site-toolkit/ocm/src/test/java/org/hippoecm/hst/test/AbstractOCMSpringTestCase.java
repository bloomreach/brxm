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
package org.hippoecm.hst.test;

import java.util.Properties;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <p>
 * AbstractOCMSpringTestCase
 * </p>
 * <p>
 * 
 * @version $Id$
 */
public abstract class AbstractOCMSpringTestCase
{

    protected final static Logger log = LoggerFactory.getLogger(AbstractOCMSpringTestCase.class);
    protected ComponentManager componentManager;

    @Before
    public void setUp() throws Exception {
        this.componentManager = new SpringComponentManager();
        ((SpringComponentManager) this.componentManager).setConfigurations(getConfigurations());
        
        this.componentManager.initialize();
        this.componentManager.start();
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }
    
    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected <T> T getComponent(String name) {
        return getComponentManager().<T>getComponent(name);
    }
    
    public static class SpringComponentManager implements ComponentManager, BeanPostProcessor {
        
        protected AbstractApplicationContext applicationContext;
        protected Properties initProps;
        protected String [] configurations;

        public SpringComponentManager() {
            this(null);
        }
        
        public SpringComponentManager(Properties initProps) {
            this.initProps = initProps;
        }
        
        public void initialize() {
            String [] configurations = getConfigurations();
            
            if (null == configurations) {
                String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
                String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
                configurations = new String[] { classXmlFileName, classXmlFileName2 };            
            }

            this.applicationContext = new ClassPathXmlApplicationContext(configurations, false) {
                // According to the javadoc of org/springframework/context/support/AbstractApplicationContext.html#postProcessBeanFactory,
                // this allows for registering special BeanPostProcessors.
                protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                    beanFactory.addBeanPostProcessor(SpringComponentManager.this);
                }
            };
            
            if (this.initProps != null) {
                PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
                ppc.setIgnoreUnresolvablePlaceholders(true);
                ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
                ppc.setProperties(this.initProps);
                this.applicationContext.addBeanFactoryPostProcessor(ppc);
            }
            
            this.applicationContext.refresh();
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
            return (T) this.applicationContext.getBean(name);
        }

        public String[] getConfigurations() {
            return this.configurations;
        }
        
        public void setConfigurations(String [] configurations) {
            this.configurations = configurations;
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

}
