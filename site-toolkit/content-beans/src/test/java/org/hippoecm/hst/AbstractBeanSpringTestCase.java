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
package org.hippoecm.hst;

import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.list.TreeList;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.SimpleTextPage;
import org.hippoecm.hst.content.beans.SimpleTextPageCopy;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterImpl;
import org.hippoecm.hst.content.beans.standard.HippoDirectory;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoFacetSearch;
import org.hippoecm.hst.content.beans.standard.HippoFacetSelect;
import org.hippoecm.hst.content.beans.standard.HippoFixedDirectory;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
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
 * AbstractBeanSpringTestCase
 * </p>
 * 
 * @version $Id$
 */
public abstract class AbstractBeanSpringTestCase
{

    protected final static Logger log = LoggerFactory.getLogger(AbstractBeanSpringTestCase.class);
    protected ComponentManager componentManager;

    @Before
    public void setUp() throws Exception {
        this.componentManager = new SpringComponentManager();
        ((SpringComponentManager) this.componentManager).setConfigurationResources(getConfigurations());
        
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
    
    protected ObjectConverter getObjectConverter() {
        
        // builds ordered mapping from jcrPrimaryNodeType to class or interface(s).
        List<KeyValue<String, Class[]>> jcrPrimaryNodeTypeClassPairs = new TreeList();


        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, SimpleTextPage.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, SimpleTextPageCopy.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDocument.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFolder.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSearch.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFacetSelect.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoDirectory.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoFixedDirectory.class);
        addJcrPrimaryNodeTypeClassPair(jcrPrimaryNodeTypeClassPairs, HippoHtml.class);
        
        // builds a fallback jcrPrimaryNodeType array.
        String [] fallBackJcrPrimaryNodeTypes = new String [] {"hippo:document"};
        
        ObjectConverter objectConverter = new ObjectConverterImpl(jcrPrimaryNodeTypeClassPairs, fallBackJcrPrimaryNodeTypes);
        return objectConverter;
    }
    
    private static void addJcrPrimaryNodeTypeClassPair(List<KeyValue<String, Class[]>> jcrPrimaryNodeTypeClassPairs, Class clazz) {
        String jcrPrimaryNodeType = null;
        
        if (clazz.isAnnotationPresent(Node.class)) {
            Node anno = (Node) clazz.getAnnotation(Node.class);
            jcrPrimaryNodeType = anno.jcrType();
        }
        
        if (jcrPrimaryNodeType == null) {
            throw new IllegalArgumentException("There's no annotation for jcrType in the class: " + clazz);
        }
        
        jcrPrimaryNodeTypeClassPairs.add(new DefaultKeyValue(jcrPrimaryNodeType, new Class [] { clazz }, true));
    }
    public static class SpringComponentManager implements ComponentManager, BeanPostProcessor {
        
        protected AbstractApplicationContext applicationContext;
        protected Configuration configuration;
        protected ContainerConfiguration containerConfiguration;
        protected String [] configurationResources;

        public SpringComponentManager() {
            this(null);
        }
        
        public SpringComponentManager(Configuration configuration) {
            this.configuration = configuration;
        }
        
        public void initialize() {
            String [] configurationResources = getConfigurationResources();
            
            if (null == configurationResources) {
                String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
                String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
                configurationResources = new String[] { classXmlFileName, classXmlFileName2 };            
            }

            this.applicationContext = new ClassPathXmlApplicationContext(configurationResources, false) {
                // According to the javadoc of org/springframework/context/support/AbstractApplicationContext.html#postProcessBeanFactory,
                // this allows for registering special BeanPostProcessors.
                protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                    beanFactory.addBeanPostProcessor(SpringComponentManager.this);
                }
            };
            
            if (this.configuration != null) {
                Properties initProps = ConfigurationConverter.getProperties(this.configuration);
                PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
                ppc.setIgnoreUnresolvablePlaceholders(true);
                ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
                ppc.setProperties(initProps);
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
        
        public ContainerConfiguration getContainerConfiguration() {
            return this.containerConfiguration;
        }

        public String[] getConfigurationResources() {
            return this.configurationResources;
        }
        
        public void setConfigurationResources(String [] configurationResources) {
            this.configurationResources = configurationResources;
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
