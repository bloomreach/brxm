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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.hippoecm.hst.core.container.AbstractValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServletDelegatingValve
 * 
 * @version $Id$
 */
public class ServletDelegatingValve extends AbstractValve
{
    
    protected HttpServlet servlet;
    protected ServletConfig config;
    protected volatile boolean servletInitialized;
    
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
                            ((ServletContextAware) config).setServletContext(context.getRequestContainerConfig().getServletContext());
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
            this(servletName, initParams, new ServletContextImpl(servletName, new HashMap<String,String>()));
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
    
    public static class ServletContextImpl implements ServletContext {
        
        static Logger log = LoggerFactory.getLogger(ServletContextImpl.class);

        protected String contextName;
        protected Map<String,String> initParams;
        protected Map<String,Object> attributes = new HashMap<String,Object>();

        public ServletContextImpl(String contextName, Map<String,String> initParams) {
            this.contextName = contextName;
            this.initParams = initParams;
        }
        
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        public Enumeration getAttributeNames() {
            return Collections.enumeration(attributes.keySet());
        }

        public ServletContext getContext(String uripath) {
            return null;
        }

        public String getInitParameter(String name) {
            return initParams.get(name);
        }
        
        public Enumeration getInitParameterNames() {
            return Collections.enumeration(initParams.keySet()); 
        }

        public int getMajorVersion() {
            return 2;
        }

        public String getMimeType(String file) {
            return null;
        }

        public int getMinorVersion() {
            return 4;
        }

        public RequestDispatcher getNamedDispatcher(String name) {
            return null;
        }

        public String getRealPath(String path) {
            return null;
        }

        public RequestDispatcher getRequestDispatcher(String path) {
            return null;
        }

        public URL getResource(String path) throws MalformedURLException {
            return null;
        }

        public InputStream getResourceAsStream(String path) {
            return null;
        }

        public Set getResourcePaths(String path) {
            return null;
        }

        public String getServerInfo() {
            return "ServletDelegatingValve";
        }

        public Servlet getServlet(String name) throws ServletException {
            return null;
        }

        public String getServletContextName() {
            return contextName;
        }

        public Enumeration getServletNames() {
            return Collections.enumeration(new HashSet<String>());
        }

        public Enumeration getServlets() {
            return Collections.enumeration(new HashSet<Servlet>());
        }

        public void log(String msg) {
            log.info(msg);
        }

        public void log(Exception exception, String msg) {
            log.error(msg, exception);
        }

        public void log(String message, Throwable throwable) {
            log.error(message, throwable);
        }

        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        public void setAttribute(String name, Object object) {
            attributes.put(name, object);
        }

        @Override
        public String getContextPath() {
            throw new UnsupportedOperationException("getContextPath is not supported for ServletDelegatingValve");
        }
    }
}

