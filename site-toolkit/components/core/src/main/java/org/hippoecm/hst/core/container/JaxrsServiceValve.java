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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JaxrsServiceValve
 * 
 * @version $Id$
 */
public class JaxrsServiceValve extends AbstractValve {
    
    private static final Logger log = LoggerFactory.getLogger(JaxrsServiceValve.class);
    
    private Class<?> busFactoryClass; // org.apache.cxf.BusFactory
    private Object bus; // org.apache.cxf.Bus
    private Object servletController; // org.apache.cxf.transport.servlet.ServletController
    private Object jaxrsServerFactoryBean; // org.apache.cxf.jaxrs.JAXRSServerFactoryBean
    
    public JaxrsServiceValve(Class<?> busFactoryClass, Object bus, Object servletController, Object jaxrsServerFactoryBean) {
        this.busFactoryClass = busFactoryClass;
        this.bus = bus;
        this.servletController = servletController;
        this.jaxrsServerFactoryBean = jaxrsServerFactoryBean;
    }
    
    @Override
    public void initialize() throws ContainerException {
        try {
            MethodUtils.invokeStaticMethod(busFactoryClass, "setThreadDefaultBus", new Object[] { bus });
            MethodUtils.invokeMethod(jaxrsServerFactoryBean, "create", null);
        } catch (Exception e) {
            log.error("Failed to initialize jaxrs server.", e);
        } finally {
            try {
                MethodUtils.invokeStaticMethod(busFactoryClass, "setThreadDefaultBus", new Object[] { null });
            } catch (Exception ignore) {
            }
        }
    }
    
    public void destroy() {
        try {
            MethodUtils.invokeMethod(bus, "shutdown", Boolean.TRUE);
        } catch (Exception e) {
            log.error("Failed to destroy jaxrs bus.", e);
        }
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        try {
            MethodUtils.invokeStaticMethod(busFactoryClass, "setThreadDefaultBus", new Object[] { bus });
            
            HttpServletRequest request = context.getServletRequest();
            ResolvedSiteMount resolvedSiteMount = (ResolvedSiteMount) request.getAttribute(ContainerConstants.RESOLVED_SITEMOUNT);
            if (resolvedSiteMount == null) {
                throw new IllegalStateException("Sitemount is not resolved for JAX-RS service for " + request.getServletPath());
            }
            
            String servletPath = resolvedSiteMount.getResolvedMountPath();
            String pathInfo = HstRequestUtils.getPathInfo(request);
            
            HttpServletRequest adjustedRequest = new PathsAdjustedHttpServletRequestWrapper(context.getServletRequest(), servletPath, pathInfo);
            
            MethodUtils.invokeMethod(servletController, "invoke", new Object[] { adjustedRequest, context.getServletResponse() });
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Failed to invoke jaxrs service.", e);
            } else {
                log.error("Failed to invoke jaxrs service. {}", e.toString());
            }
        } finally {
            try {
                MethodUtils.invokeStaticMethod(busFactoryClass, "setThreadDefaultBus", new Object[] { null });
            } catch (Exception ignore) {
            }
        }
    }
    
    private static class PathsAdjustedHttpServletRequestWrapper extends HttpServletRequestWrapper
    {
        private String servletPath;
        private String pathInfo;
        
        private PathsAdjustedHttpServletRequestWrapper(HttpServletRequest request, String servletPath, String pathInfo)
        {
            super(request);
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
        }
        
        @Override
        public String getServletPath()
        {
            return (servletPath != null ? servletPath : super.getServletPath());
        }
        
        @Override
        public String getPathInfo()
        {
            return (pathInfo != null ? pathInfo : super.getPathInfo());
        }
    }
    
}
