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

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.beanutils.MethodUtils;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
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
            String pathInfo = HstRequestUtils.getPathInfo(request);
            HstRequestContext requestContext = (HstRequestContext) request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            HttpServletRequest adjustedRequest = new PathsAdjustedHttpServletRequestWrapper(context.getServletRequest(), null, pathInfo, requestContext);
            
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
    
    private static class PathsAdjustedHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private String servletPath;
        private String pathInfo;
        private HstRequestContext requestContext;
        
        private PathsAdjustedHttpServletRequestWrapper(HttpServletRequest request, String servletPath, String pathInfo, HstRequestContext requestContext) {
            super(request);
            this.servletPath = servletPath;
            this.pathInfo = pathInfo;
            this.requestContext = requestContext;
        }
        
        @Override
        public String getServletPath() {
            return (servletPath != null ? servletPath : super.getServletPath());
        }
        
        @Override
        public String getPathInfo() {
            return (pathInfo != null ? pathInfo : super.getPathInfo());
        }

        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletRequestWrapper#getUserPrincipal()
         */
        public Principal getUserPrincipal() {
            // initialize configuration of user principal class
            HstRequestImpl.initUserPrincipalClass(requestContext);

            // check for user principal in current thread's user subject
            boolean [] subjectFound = new boolean[1];
            Principal principal = HstRequestImpl.getSubjectUserPrincipal(subjectFound);
            if (subjectFound[0]) {
                return principal;
            }

            // return existing principal for request if any
            return super.getUserPrincipal();
        }
        
        /* (non-Javadoc)
         * @see javax.servlet.http.HttpServletRequestWrapper#isUserInRole(java.lang.String)
         */
        public boolean isUserInRole(String role) {
            // initialize configuration of role principal class
            HstRequestImpl.initRolePrincipalClass(requestContext);

            // check for role principals in current thread's user subject
            boolean [] subjectFound = new boolean[1];
            boolean userInRole = HstRequestImpl.isSubjectUserInRole(role, subjectFound);
            if (subjectFound[0]) {
                return userInRole;
            }
            
            // return existing role tests if available
            return super.isUserInRole(role);
        }
    }
}
