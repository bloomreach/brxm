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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResourceResponseImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstComponentInvokerImpl implements HstComponentInvoker {
    
    private final static Logger log = LoggerFactory.getLogger(HstComponentInvokerImpl.class);
    
    protected boolean exceptionThrowable;
    
    public void setExceptionThrowable(boolean exceptionThrowable) {
        this.exceptionThrowable = exceptionThrowable;
    }
    
    public void invokeAction(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                component.doAction(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(th);
                }
                
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeBeforeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                component.doBeforeRender(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(th);
                }
                
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeRender(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        String dispatchUrl = ((HstResponseImpl) hstResponse).getRenderPath(); 
        
        if (dispatchUrl == null) {
            dispatchUrl = ((HstRequestImpl) hstRequest).getComponentWindow().getRenderPath();
        }
        
        ServletRequest wrappedRequest = ((HstRequestImpl) hstRequest).getRequest();
        
        try {
            setHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
            invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, dispatchUrl, window);
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
        }
        
        if (window.hasComponentExceptions()) {
            renderErrorInformation(window, hstResponse);
        }
    }

    public void invokeBeforeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                component.doBeforeServeResource(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(th);
                }
                
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        } else {
            window.addComponentExcpetion(new HstComponentException("The component is not available."));
        }
    }

    public void invokeServeResource(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        
        String dispatchUrl = ((HstResourceResponseImpl) hstResponse).getServeResourcePath();
        
        if (dispatchUrl == null) {
            dispatchUrl = window.getServeResourcePath();
        }

        if (dispatchUrl == null) {
            dispatchUrl = window.getRenderPath();
        }
        
        ServletRequest wrappedRequest = ((HstRequestImpl) hstRequest).getRequest();
        
        try {
            setHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
            invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, dispatchUrl, window);
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
        }

        if (window.hasComponentExceptions()) {
            renderErrorInformation(window, hstResponse);
        }
    }

    protected void invokeDispatcher(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, String dispatchUrl, HstComponentWindow window) throws ContainerException {
        RequestDispatcher disp = null;
        
        if (dispatchUrl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Invoking dispatcher of url: {}", dispatchUrl);
            }
            
            if (dispatchUrl.startsWith("/")) {
                disp = requestContainerConfig.getServletConfig().getServletContext().getRequestDispatcher(dispatchUrl);
            } else {
                disp = requestContainerConfig.getServletConfig().getServletContext().getNamedDispatcher(dispatchUrl);
            }
        }
        
        if (disp == null) {
            if (log.isWarnEnabled()) {
                log.warn("The dispatch url is null. window reference namespace: " + window.getReferenceNamespace());
            }
            window.addComponentExcpetion(new HstComponentException("The dispatch url is null."));
        } else {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                disp.include(servletRequest, servletResponse);
            } catch (ServletException e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }
                
                window.addComponentExcpetion(new HstComponentException(e.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {} \n{}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (IOException e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }

                window.addComponentExcpetion(new HstComponentException(e.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {} \n{}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.getMessage());
                }
            } catch (Throwable th) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(th);
                }
                
                window.addComponentExcpetion(new HstComponentException(th.getMessage()));
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: {} \n{}", th.getMessage(), th);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", th.getMessage());
                }
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
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
    
    private ClassLoader switchToContainerClassloader(HstContainerConfig requestContainerConfig) {
        ClassLoader containerClassloader = requestContainerConfig.getContextClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        if (containerClassloader != currentClassloader) {
            Thread.currentThread().setContextClassLoader(containerClassloader);
            return currentClassloader;
        } else {
            return null;
        }
    }
    
    private void setHstObjectAttributesForServlet(ServletRequest servletRequest, HstRequest hstRequest, HstResponse hstResponse) {
        // Needs to set hst request/response into attribute map
        // because hst request/response can be wrapped so it's not possible to use casting
        // in the servlet side such as tag library.
        servletRequest.setAttribute(ContainerConstants.HST_REQUEST, hstRequest);
        servletRequest.setAttribute(ContainerConstants.HST_RESPONSE, hstResponse);
    }
    
    private void removeHstObjectAttributesForServlet(ServletRequest servletRequest, HstRequest hstRequest, HstResponse hstResponse) {
        // Removes hst request/response into attribute map after dispatching
        servletRequest.removeAttribute(ContainerConstants.HST_REQUEST);
        servletRequest.removeAttribute(ContainerConstants.HST_RESPONSE);
    }
    
}
