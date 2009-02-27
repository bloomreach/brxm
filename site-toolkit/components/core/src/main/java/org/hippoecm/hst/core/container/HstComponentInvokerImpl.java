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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstComponentInvokerImpl implements HstComponentInvoker {
    
    private final static Logger log = LoggerFactory.getLogger(HstComponentInvoker.class);
    
    public void invokeAction(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            try {
                component.doAction(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                window.addComponentExcpetion(e);
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeBeforeRender(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            try {
                component.doBeforeRender(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                window.addComponentExcpetion(e);
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeRender(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        String dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        invokeDispatcher(servletConfig, servletRequest, servletResponse, dispatchUrl, window);
        
        if (window.hasComponentExceptions()) {
            renderErrorInformation(window, hstResponse);
        }
    }

    public void invokeBeforeServeResource(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            try {
                component.doBeforeServeResource(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                window.addComponentExcpetion(e);
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeServeResource(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        String dispatchUrl = hstRequest.getComponentWindow().getServeResourcePath();
        
        if (dispatchUrl == null) {
            dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        }
        
        invokeDispatcher(servletConfig, servletRequest, servletResponse, dispatchUrl, window);

        if (window.hasComponentExceptions()) {
            renderErrorInformation(window, hstResponse);
        }
    }

    protected void invokeDispatcher(ServletConfig servletConfig, ServletRequest servletRequest, ServletResponse servletResponse, String dispatchUrl, HstComponentWindow window) throws ContainerException {
        RequestDispatcher disp = null;
        
        if (dispatchUrl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Invoking dispatcher of url: {}", dispatchUrl);
            }
            
            if (dispatchUrl.startsWith("/")) {
                disp = servletConfig.getServletContext().getRequestDispatcher(dispatchUrl);
            } else {
                disp = servletConfig.getServletContext().getNamedDispatcher(dispatchUrl);
            }
        }
        
        if (disp == null) {
            if (log.isWarnEnabled()) {
                log.warn("The dispatch url is null. window reference namespace: " + window.getReferenceNamespace());
            }
            window.addComponentExcpetion(new HstComponentException("The dispatch url is null."));
        } else {
            try {
                disp.include(servletRequest, servletResponse);
            } catch (ServletException e) {
                window.addComponentExcpetion(new HstComponentException(e.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (IOException e) {
                window.addComponentExcpetion(new HstComponentException(e.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            }
        }
    }
    
    protected void renderErrorInformation(HstComponentWindow window, HstResponse hstResponse) {
        PrintWriter out = null;
        
        try {
            hstResponse.reset();
            out = hstResponse.getWriter();
            for (HstComponentException hce : window.getComponentExceptions()) {
                out.println(hce.getMessage());
            }
            out.flush();
        } catch (Exception e) {
            log.warn("Invalid out.");
        } finally {
            try {
                hstResponse.flushBuffer();
            } catch (IOException e) {
            }
        }
    }

}
