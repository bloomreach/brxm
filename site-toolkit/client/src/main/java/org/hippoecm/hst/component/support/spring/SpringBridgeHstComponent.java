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
package org.hippoecm.hst.component.support.spring;

import javax.servlet.ServletConfig;

import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * A bridge component which delegates all invocation to a bean managed by the spring IoC.
 * <p>
 * By default, the delegated bean's name should be configured in the component configuration
 * with the parameter name, 'spring-delegated-bean-param-name'.
 * This bridge component will retrieve the bean from the spring web application context by the
 * bean name.
 * If you want to change the default parameter name, then you can achieve that 
 * by configuring the parameter name in the web.xml.
 * For example, if you want to change the default parameter name to 'my-bean-param', then
 * you can configure this like the following:
 * 
 * <xmp>
 *  <servlet>
 *    <servlet-name>HstContainerServlet</servlet-name>
 *    <servlet-class>org.hippoecm.hst.container.HstContainerServlet</servlet-class>
 *    <!--
 *    ...
 *    -->
 *    <init-param>
 *      <param-name>spring-delegated-bean-param-name</param-name>
 *      <param-value>my-bean-param</param-value>
 *    </init-param>
 *    <!--
 *    ...
 *    -->
 *    <load-on-startup>2</load-on-startup>
 *  </servlet>
 * </xmp>
 * 
 * With the above setting, you need to set the parameters with name, 'my-bean-param' in the
 * component configurations in the repository.
 * </p>
 * 
 * @version $Id$
 */
public class SpringBridgeHstComponent extends GenericHstComponent {
    
    protected String delegatedBeanNameParamName = "spring-delegated-bean-param-name";
    protected HstComponent delegatedBean;
    
    public void init(ServletConfig servletConfig, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletConfig, componentConfig);
        
        String param = servletConfig.getInitParameter("spring-delegated-bean-param-name");
        
        if (param != null) {
            delegatedBeanNameParamName = param;
        }
    }

    public void destroy() throws HstComponentException {
        super.destroy();
        delegatedBean = null;
    }
    
    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doAction(request, response);
    }

    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doBeforeRender(request, response);
    }

    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
        getDelegatedBean(request).doBeforeServeResource(request, response);
    }
    
    protected String getParameter(String name, HstRequest request) {
        return (String)this.getComponentConfiguration().getParameter(name, request.getRequestContext().getResolvedSiteMapItem());
    }
    
    protected HstComponent getDelegatedBean(HstRequest request) {
        if (delegatedBean == null) {
            String beanName = getParameter(delegatedBeanNameParamName, request);
            ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletConfig().getServletContext());
            delegatedBean = (HstComponent) applicationContext.getBean(beanName);
        }
        
        return delegatedBean;
    }
    
}
