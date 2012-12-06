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

import java.util.Properties;

import org.hippoecm.hst.component.support.ClientComponentManager;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class DefaultComponentManagerApplicationContext extends ClassPathXmlApplicationContext implements BeanPostProcessor, ComponentManagerAware {

    private ContainerConfiguration containerConfiguration;
    private ComponentManager componentManager;

    public DefaultComponentManagerApplicationContext() {
        this(null, null, null);
    }

    public DefaultComponentManagerApplicationContext(ContainerConfiguration containerConfiguration) {
        this(containerConfiguration, null);
    }

    public DefaultComponentManagerApplicationContext(ComponentManager componentManager) {
        this(componentManager, null, null);
    }

    public DefaultComponentManagerApplicationContext(ComponentManager componentManager, ContainerConfiguration containerConfiguration) {
        this(componentManager, containerConfiguration, null);
    }

    public DefaultComponentManagerApplicationContext(ContainerConfiguration containerConfiguration, ApplicationContext parentApplicationContext) {
        this(null, containerConfiguration, parentApplicationContext);
    }

    public DefaultComponentManagerApplicationContext(ComponentManager componentManager, ContainerConfiguration containerConfiguration, ApplicationContext parentApplicationContext) {
        super(parentApplicationContext);

        this.componentManager = componentManager;
        this.containerConfiguration = containerConfiguration;

        boolean isClientComponentManager = componentManager != null && componentManager instanceof ClientComponentManager;

        if (this.containerConfiguration != null && !this.containerConfiguration.isEmpty()) {
            Properties initProps = this.containerConfiguration.toProperties();
            PropertyPlaceholderConfigurer ppc = new OverridingByAttributesPropertyPlaceholderConfigurer();
            if (isClientComponentManager) {
                ppc.setIgnoreUnresolvablePlaceholders(this.containerConfiguration.getBoolean(ClientComponentManager.IGNORE_UNRESOLVABLE_PLACE_HOLDERS, true));
                ppc.setSystemPropertiesMode(this.containerConfiguration.getInt(ClientComponentManager.SYSTEM_PROPERTIES_MODE, PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK));
            }
            else {
                ppc.setIgnoreUnresolvablePlaceholders(this.containerConfiguration.getBoolean(SpringComponentManager.IGNORE_UNRESOLVABLE_PLACE_HOLDERS, true));
                ppc.setSystemPropertiesMode(this.containerConfiguration.getInt(SpringComponentManager.SYSTEM_PROPERTIES_MODE, PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK));
            }
            ppc.setProperties(initProps);
            addBeanFactoryPostProcessor(ppc);
        }
    }

    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    // According to the javadoc of org/springframework/context/support/AbstractApplicationContext.html#postProcessBeanFactory,
    // this allows for registering special BeanPostProcessors.
    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        beanFactory.addBeanPostProcessor(this);
        // copied from org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
        if (componentManager != null && componentManager.getServletContext() != null) {
            beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(componentManager.getServletContext(), componentManager.getServletConfig()));
            beanFactory.ignoreDependencyInterface(ServletContextAware.class);
            if (componentManager.getServletConfig() != null) {
                beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
            }

            WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, componentManager.getServletContext());
            WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, componentManager.getServletContext(), componentManager.getServletConfig());
        }
    }

    @Override
    protected DefaultListableBeanFactory createBeanFactory() {
        if (containerConfiguration == null) {
            return super.createBeanFactory();
        } else {
            BeanFactory parentBeanFactory = getInternalParentBeanFactory();
            return new FilteringByExpressionListableBeanFactory(parentBeanFactory, containerConfiguration);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (componentManager != null && bean instanceof ComponentManagerAware) {
            ((ComponentManagerAware) bean).setComponentManager(componentManager);
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
