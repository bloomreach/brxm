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
package org.hippoecm.hst.component.support.spring;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A bridge component which delegates all invocation to a bean managed by the spring IoC.
 * <p>
 * By default, the delegated bean's name should be configured in the component configuration
 * parameter value with the parameter name, 'spring-delegated-bean-param-name'.
 * This bridge component will retrieve the bean from the spring web application context by the
 * bean name.
 * If you want to change the default parameter name, then you can achieve that 
 * by configuring the parameter name with an added "hst-" prefix in the web.xml.
 * For example, if you want to change the default parameter name to 'my-bean-param', then
 * you can configure this like the following:
 * 
 * <xmp>
 *  <webapp ...>
 *    <!--
 *    ...
 *    -->
 *    <init-param>
 *      <param-name>hst-spring-delegated-bean-param-name</param-name>
 *      <param-value>my-bean-param</param-value>
 *    </init-param>
 *    <!--
 *    ...
 *    -->
 *  </webapp>
 * </xmp>
 * 
 * With the above setting, you need to set the parameters with name, 'my-bean-param' in the
 * component configurations in the repository.
 * </p>
 * <p>
 * If the root web application context has hierarchical child bean factories and one of the
 * child bean factories has defined a bean you need, then you can set the bean name component
 * configuration parameter with context path prefix like the following example:
 * <br/>
 * <CODE>com.mycompany.myapp::contactBean</CODE>
 * <br/>
 * In the above example, 'com.mycompany.myapp' is the name of the child bean factory name,
 * and 'contactBean' is the bean name of the child bean factory.
 * The bean factory paths can be multiple to represent the hierarchy
 * like 'com.mycompany.myapp::crm::contactBean'.
 * <br/>
 * The separator for hierarchical bean factory path can be changed by setting the webapp
 * init parameter, 'hst-spring-context-name-separator-param-name'.
 * </p>
 * 
 * @version $Id$
 */
public class SpringBridgeHstComponent extends GenericHstComponent implements ApplicationListener {

    private static final Logger log = LoggerFactory.getLogger(SpringBridgeHstComponent.class);

    protected String delegatedBeanNameParamName = "spring-delegated-bean";
    protected String contextNameSeparator = "::";
    
    protected AbstractApplicationContext delegatedBeanApplicationContext;
    protected HstComponent delegatedBean;

    private ServletContext servletContext; 
    
    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletContext, componentConfig);

        this.servletContext = servletContext;
        String param = servletContext.getInitParameter("hst-spring-delegated-bean-param-name");
        
        if (param != null) {
            delegatedBeanNameParamName = param;
        }
        
        param = servletContext.getInitParameter("hst-spring-context-name-separator-param-name");
        
        if (param != null) {
            contextNameSeparator = param;
        }
    }

    @Override
    public void destroy() throws HstComponentException {
        this.delegatedBeanApplicationContext = null;
        
        if (delegatedBean != null) {
            delegatedBean.destroy();
            delegatedBean = null;
        }

        super.destroy();
    }
    
    @Override
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doAction(request, response);
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doBeforeRender(request, response);
    }

    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doBeforeServeResource(request, response);
    }
    
    protected String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    protected HstComponent getDelegatedBean(HstRequest request) throws HstComponentException {
        if (delegatedBean == null) {
            String beanName = StringUtils.trim(getParameter(delegatedBeanNameParamName, request));
            
            if (beanName == null) {
                throw new HstComponentException("The name of delegated spring bean is null.");
            }
            
            String [] contextNames = null;
            
            if (beanName.contains(this.contextNameSeparator)) {
                String [] tempArray = beanName.split(this.contextNameSeparator);
                
                if (tempArray.length > 1) {
                    contextNames = new String[tempArray.length - 1];
                    
                    for (int i = 0; i < tempArray.length - 1; i++) {
                        contextNames[i] = tempArray[i];
                    }
                    
                    beanName = tempArray[tempArray.length - 1];
                }
            }
            
            boolean beanFoundFromBeanFactory = false;
            BeanFactory beanFactory = WebApplicationContextUtils.getWebApplicationContext(servletContext);
            
            if (beanFactory != null) {
                String contextName = null;
                
                try {
                    if (contextNames != null) {
                        for (int i = 0; i < contextNames.length; i++) {
                            contextName = contextNames[i];
                            beanFactory = (BeanFactory) beanFactory.getBean(contextName);
                        }
                    }
                } catch (NoSuchBeanDefinitionException e) {
                    throw new HstComponentException("There's no beanFactory definition with the specified name: " + contextName, e);
                } catch (BeansException e) {
                    throw new HstComponentException("The beanFactory cannot be obtained: " + contextName, e);
                } catch (ClassCastException e) {
                    throw new HstComponentException("The bean is not an instance of beanFactory: " + contextName, e);
                }
                
                try {
                    delegatedBean = (HstComponent) beanFactory.getBean(beanName);
                    beanFoundFromBeanFactory = (delegatedBean != null);
                } catch (Exception ignore) {
                }
            }
            
            ComponentManager componentManager = null;
            
            if (delegatedBean == null) {
                componentManager = HstFilter.getClientComponentManager(servletContext);
                
                if (componentManager != null) {
                    delegatedBean = componentManager.getComponent(beanName);
                    if (delegatedBean != null) {
                        log.warn("ClientComponentManager is deprecated. Use HstService#getComponentManager() instead and replace " +
                                "client-assembly spring configuration with hst-assemply/overrides configuration. Remove " +
                                "clientComponentManagerClass init-param from web.xml for HstFilter as well.");
                    }
                }
            }
            
            if (delegatedBean == null) {
                if (beanFactory == null && componentManager == null) {
                    throw new HstComponentException("Cannot find the root web application context or client component manager.");
                } else if (beanFactory != null && componentManager == null) {
                    throw new HstComponentException("Cannot find delegated spring HstComponent bean from the web application context: " + beanName);
                } else if (beanFactory == null && componentManager != null) {
                    throw new HstComponentException("Cannot find delegated spring HstComponent bean from the client component manager: " + beanName);
                } else {
                    throw new HstComponentException("Cannot find delegated spring HstComponent bean from either the web application context or the client component manager: " + beanName);
                }
            }

            delegatedBean.init(servletContext, getComponentConfiguration());
            
            if (beanFoundFromBeanFactory && beanFactory instanceof AbstractApplicationContext) {
                delegatedBeanApplicationContext = (AbstractApplicationContext) beanFactory;
                
                if (!delegatedBeanApplicationContext.getApplicationListeners().contains(this)) {
                    delegatedBeanApplicationContext.addApplicationListener(this);
                }
            }
        }
        return delegatedBean;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent) {
            this.delegatedBeanApplicationContext = null;
            
            if (delegatedBean != null) {
                delegatedBean.destroy();
                delegatedBean = null;
            }
        }
    }
    
}
