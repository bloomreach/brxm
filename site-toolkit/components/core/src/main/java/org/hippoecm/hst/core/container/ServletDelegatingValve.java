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
package org.hippoecm.hst.core.container;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

import org.hippoecm.hst.core.container.AbstractValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;

public class ServletDelegatingValve extends AbstractValve
{
    
    protected HttpServlet servlet;
    protected ServletConfig config;
    protected boolean servletInitialized;
    
    public ServletDelegatingValve(HttpServlet servlet, ServletConfig config) {
        this.servlet = servlet;
        this.config = config;
    }
    
    @Override
    public void destroy() {
        if (servletInitialized) {
            servlet.destroy();
        }
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        try {
            if (!servletInitialized) {
                synchronized (servlet) {
                    if (!servletInitialized) {
                        if (config instanceof ServletContextAware) {
                            ((ServletContextAware) config).setServletContext(context.getRequestContainerConfig().getServletConfig().getServletContext());
                        }
                        servlet.init(config);
                        servletInitialized = true;
                    }
                }
            }
            
            servlet.service(context.getServletRequest(), context.getServletResponse());
        } catch (Exception e) {
            throw new ContainerException(e);
        }
        
        // continue
        context.invokeNext();
    }
    
    public interface ServletContextAware {
        
        public void setServletContext(ServletContext servletContext);
        
    }
    
    public static class ServletConfigImpl implements ServletConfig, ServletContextAware {
        
        protected String servletName;
        protected Map<String, String> initParams;
        protected ServletContext servletContext;
        
        public ServletConfigImpl(String servletName, Map<String, String> initParams) {
            this(servletName, initParams, null);
        }
        
        public ServletConfigImpl(String servletName, Map<String, String> initParams, ServletContext servletContext) {
            this.servletName = servletName;
            this.initParams = initParams;
            this.servletContext = servletContext;
        }
        
        public String getInitParameter(String paramName) {
            return initParams.get(paramName);
        }
        
        public Enumeration getInitParameterNames() {
            return Collections.enumeration(initParams.keySet()); 
        }
        
        public ServletContext getServletContext() {
            return servletContext;
        }
        
        public void setServletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
        }
        
        public String getServletName() {
            return servletName;
        }
        
    }
    
}

