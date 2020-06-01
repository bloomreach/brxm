/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.site.HstServices;
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
import org.springframework.web.servlet.FrameworkServlet;

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
 * child bean factories has defined a bean you need, then you can set the fully qualified bean name
 * parameter with context path prefix like the following example:
 * <br/>
 * <CODE>com.mycompany.myapp::contactBean</CODE>
 * <br/>
 * In the above example, 'com.mycompany.myapp' is the name of the child bean factory name or
 * the child HST component module name, and 'contactBean' is the bean name of the child bean factory
 * or the child HST component module.
 * The bean factory paths can be multiple to represent the hierarchy
 * like 'com.mycompany.myapp::crm::contactBean'.
 * <br/>
 * The separator for hierarchical bean factory path can be changed by setting the webapp
 * init parameter, 'hst-spring-context-name-separator-param-name'.
 * </p>
 * <p>
 * Also, if the delegatee bean should be found in a web application context initialized by a
 * Spring MVC Servlet derived from <code>org.springframework.web.servlet.FrameworkServlet</code>
 * such as <code>org.hippoecm.hst.component.support.spring.mvc.HstDispatcherServlet</code> or
 * <code>org.springframework.web.servlet.DispatcherServlet</code>, instead of the root web
 * application context, then the fully qualified bean name must be prefixed by the servlet
 * context attribute name prefix (see {@link FrameworkServlet#SERVLET_CONTEXT_PREFIX}).
 * </p>
 * <p>
 * For example, if you want to retrieve a bean named 'contactBean' from the application context
 * initialized by a <code>org.hippoecm.hst.component.support.spring.mvc.HstDispatcherServlet</code>
 * separately from the root web application context and the servlet's name is 'springapp',
 * then you can specify the paramaeter like the following:
 * <br/>
 * <CODE>org.springframework.web.servlet.FrameworkServlet.CONTEXT.springapp::contactBean</CODE>
 * </p>
 */
public class SpringBridgeHstComponent extends GenericHstComponent implements ApplicationListener<ApplicationEvent> {

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
        final HstComponent delegatee = getDelegatedBean(request);

        if (HDC.isStarted()) {
            final Task curTask = HDC.getCurrentTask();
            curTask.setAttribute("SpringBridgeHstComponent.delegatee", delegatee.getClass().toString());
        }

        delegatee.doAction(request, response);
    }

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        final HstComponent delegatee = getDelegatedBean(request);

        if (HDC.isStarted()) {
            final Task curTask = HDC.getCurrentTask();
            curTask.setAttribute("SpringBridgeHstComponent.delegatee", delegatee.getClass().toString());
        }

        delegatee.doBeforeRender(request, response);
    }

    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        final HstComponent delegatee = getDelegatedBean(request);

        if (HDC.isStarted()) {
            final Task curTask = HDC.getCurrentTask();
            curTask.setAttribute("SpringBridgeHstComponent.delegatee", delegatee.getClass().toString());
        }

        delegatee.doBeforeServeResource(request, response);
    }

    protected String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }

    protected HstComponent getDelegatedBean(HstRequest request) throws HstComponentException {
        if (delegatedBean == null) {
            String fqbnParam = StringUtils.trim(getParameter(delegatedBeanNameParamName, request));

            if (fqbnParam == null) {
                throw new HstComponentException("The name of delegated spring bean is null.");
            }

            FullyQualifiedBeanName fqbn = new FullyQualifiedBeanName(fqbnParam, contextNameSeparator);
            String [] contextNames = fqbn.getContextNames();
            String beanName = fqbn.getBeanName();
            boolean beanFoundFromBeanFactory = false;
            BeanFactory beanFactory = getContextBeanFactory(servletContext, contextNames);

            if (beanFactory != null) {
                try {
                    delegatedBean = (HstComponent) beanFactory.getBean(beanName);
                    beanFoundFromBeanFactory = (delegatedBean != null);
                } catch (Exception ignore) {
                }
            }

            if (delegatedBean == null) {
                if (contextNames != null) {
                    delegatedBean = HstServices.getComponentManager().getComponent(beanName, contextNames);
                } else {
                    delegatedBean = HstServices.getComponentManager().getComponent(beanName);
                }
            }

            if (delegatedBean == null) {
                if (beanFactory == null) {
                    throw new HstComponentException("Cannot find the root web application context or component manager.");
                } else {
                    throw new HstComponentException("Cannot find delegated spring HstComponent bean from the web application context: " + fqbnParam);
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

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent) {
            this.delegatedBeanApplicationContext = null;

            if (delegatedBean != null) {
                delegatedBean.destroy();
                delegatedBean = null;
            }
        }
    }

    protected BeanFactory getContextBeanFactory(final ServletContext servletContext, final String [] contextNames) {
        BeanFactory beanFactory = null;
        String contextName = null;

        try {
            int beginIndex;

            if (contextNames.length > 0 && StringUtils.startsWith(contextNames[0], FrameworkServlet.SERVLET_CONTEXT_PREFIX)) {
                beanFactory = WebApplicationContextUtils.getWebApplicationContext(servletContext, contextNames[0]);
                beginIndex = 1;
            } else {
                beanFactory = WebApplicationContextUtils.getWebApplicationContext(servletContext);
                beginIndex = 0;
            }

            if (contextNames != null) {
                for (int i = beginIndex; i < contextNames.length; i++) {
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
        } catch (Exception e) {
            throw new HstComponentException("The beanFactory cannot be obtained: " + contextName, e);
        }

        return beanFactory;
    }

    private static class FullyQualifiedBeanName {

        private final String [] contextNames;
        private final String beanName;

        public FullyQualifiedBeanName(final String fqbn, final String scopeSeparator) {
            if (StringUtils.contains(fqbn, scopeSeparator)) {
                String [] tempArray = StringUtils.split(fqbn, scopeSeparator);
                contextNames = new String[tempArray.length - 1];

                for (int i = 0; i < tempArray.length - 1; i++) {
                    contextNames[i] = StringUtils.trim(tempArray[i]);
                }

                beanName = StringUtils.trim(tempArray[tempArray.length - 1]);
            } else {
                contextNames = ArrayUtils.EMPTY_STRING_ARRAY;
                beanName = StringUtils.trim(fqbn);
            }
        }

        public String [] getContextNames() {
            return contextNames;
        }

        public String getBeanName() {
            return beanName;
        }
    }
}
