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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.component.HstResourceResponseImpl;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstComponentInvokerImpl
 * 
 * @version $Id$
 */
public class HstComponentInvokerImpl implements HstComponentInvoker {
    
    private final static Logger log = LoggerFactory.getLogger(HstComponentInvokerImpl.class);
   
    protected boolean exceptionThrowable;
    
    protected String errorRenderPath;
    
    public void setExceptionThrowable(boolean exceptionThrowable) {
        this.exceptionThrowable = exceptionThrowable;
    }
    
    public void setErrorRenderPath(String errorRenderPath) {
        this.errorRenderPath = errorRenderPath;
    }
    
    public void invokeAction(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = ((HstRequestImpl) hstRequest).getComponentWindow();
        HstComponent component = window.getComponent();
        
        if (component != null) {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                if(log.isDebugEnabled()) {
                    log.debug("invoking action of component: {}", component.getClass().getName());
                }
                
                component.doAction(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
                }
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }
                
                window.addComponentExcpetion(new HstComponentException(e));
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.toString());
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
                if(log.isDebugEnabled()) {
                    log.debug("invoking doBeforeRender of component: {}", component.getClass().getName());
                }
                
                component.doBeforeRender(hstRequest, hstResponse);
            } catch (HstComponentException e) {
                if (this.exceptionThrowable) {
                    throw e;
                }
                
                window.addComponentExcpetion(e);
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.toString());
                }
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }
                
                window.addComponentExcpetion(new HstComponentException(e));
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
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
        HstComponent component = window.getComponent();
        String dispatchUrl = ((HstResponseImpl) hstResponse).getRenderPath(); 
        
        if (dispatchUrl == null) {
            dispatchUrl = ((HstRequestImpl) hstRequest).getComponentWindow().getRenderPath();
        }
        
        ServletRequest wrappedRequest = ((HstRequestImpl) hstRequest).getRequest();
        
        try {
            setHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
            
            // TODO : move the 'composer' logic to a better place
            /*
             * Below we check whether we need to output 'sample' content for the component instead of rendering the actual template. 
             * We only show sample content if and only if:
             * 1) There is already an HttpSession
             * 2) On this HttpSession there is a attribute called ContainerConstants.COMPOSERMODE_TEMPLATE_VIEW with value "true"
             * 3) The component is of type CONTAINER_ITEM_COMPONENT
             * 4) compInfo instanceof HstComponentConfigurationService
             * 5) mount is of type ContainerConstants.COMPOSERMODE && of type ContainerConstants.COMPOSERMODE_TEMPLATE_VIEW
             * 6) the container item component has sample content configured
             */
            
            HstComponentConfiguration compInfo = ((HstComponentConfiguration)window.getComponentInfo());
            SiteMount mount = hstRequest.getRequestContext().getResolvedSiteMount().getSiteMount();
            // do not create a http session!!
            HttpSession session = hstRequest.getSession(false);
            if (session != null 
                    && "true".equals(session.getAttribute(ContainerConstants.COMPOSERMODE_TEMPLATE_VIEW_ATTR_NAME))
                    && compInfo instanceof HstComponentConfigurationService
                    && compInfo.getComponentType() == Type.CONTAINER_ITEM_COMPONENT
                    && mount.isOfType(ContainerConstants.COMPOSERMODE)
                    && ((HstComponentConfigurationService) compInfo).getSampleContent() != null) {
               // We are in composermode && composer-template-view && the current component is a container item, and has sample content: hence, we do not render the actual template, but show sample content
               hstResponse.getWriter().append(((HstComponentConfigurationService)compInfo).getSampleContent());
               hstResponse.flushBuffer();
             /////// TODO END ////////////////////////////
            } else {
                invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, dispatchUrl, window);
            }
        } catch (HstComponentException e) {
            if (this.exceptionThrowable) {
                throw e;
            }
            
            window.addComponentExcpetion(e);
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
            }
        } catch (Exception e) {
            if (this.exceptionThrowable) {
                throw new HstComponentException(e);
            }
            
            window.addComponentExcpetion(new HstComponentException(e));
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
            }
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
        }
        
        if (window.hasComponentExceptions()) {
            renderErrorInformation(requestContainerConfig, servletRequest, servletResponse, window);
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
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
                }
            } catch (Exception e) {
                if (this.exceptionThrowable) {
                    throw new HstComponentException(e);
                }
                
                window.addComponentExcpetion(new HstComponentException(e));
                
                if (log.isDebugEnabled()) {
                    log.warn("Component exception caught: " + e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Component exception caught: {}", e.toString());
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
        HstComponent component = window.getComponent();
        
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
            
        } catch (HstComponentException e) {
            if (this.exceptionThrowable) {
                throw e;
            }
            
            window.addComponentExcpetion(e);
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught on window " + window.getName() + " with component " + component.getClass().getName() + ": {}", e.toString());
            }
        } catch (Exception e) {
            if (this.exceptionThrowable) {
                throw new HstComponentException(e);
            }
            
            window.addComponentExcpetion(new HstComponentException(e));
            
            if (log.isDebugEnabled()) {
                log.warn("Component exception caught: " + e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Component exception caught: {}", e.toString());
            }
        } finally {
            removeHstObjectAttributesForServlet(wrappedRequest, hstRequest, hstResponse);
        }

        if (window.hasComponentExceptions()) {
            renderErrorInformation(requestContainerConfig, servletRequest, servletResponse, window);
        }
    }

    protected void invokeDispatcher(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, String dispatchUrl, HstComponentWindow window) throws Exception {
        RequestDispatcher disp = null;
        
        if (dispatchUrl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Invoking dispatcher of url: {}", dispatchUrl);
            }
            for(String specialPrefix : HstComponentWindow.SPECIAL_DISPATCH_PREFIXES) {
                if(dispatchUrl.startsWith(specialPrefix)) {
                    servletRequest.setAttribute(ContainerConstants.SPECIAL_DISPATCH_INFO, specialPrefix);
                    dispatchUrl = dispatchUrl.substring(specialPrefix.length());
                    if(!dispatchUrl.startsWith("/") && specialPrefix.equals("classpath:")) {
                        // the dispatch url is relative to the current HstComponent. Let's account for this.
                       String currentComponentPackage = "/" + window.getComponent().getClass().getPackage().getName().replace(".", "/");
                       String absoluteUrl = currentComponentPackage + "/" + dispatchUrl;
                       log.debug("Relative dispatch URL '{}' rewritten to '{}'", dispatchUrl, absoluteUrl);
                       dispatchUrl = absoluteUrl;
                    } 
                    break;
                }
            }
            
            if (dispatchUrl.startsWith("/")) {
                disp = requestContainerConfig.getServletContext().getRequestDispatcher(dispatchUrl);
            } else {
                disp = requestContainerConfig.getServletContext().getNamedDispatcher(dispatchUrl);
            }
        }
        
        if (disp == null) {
            if (log.isWarnEnabled()) {
                log.warn("The request dispatcher for dispatch url '{}' is null. Component name: '{}' . Component class: '"+window.getComponentInfo().getComponentClassName()+"'. Component id: '"+window.getComponentInfo().getId()+"'. If the component is inherited, the id might be a concatenation of some id's.", dispatchUrl, window.getName());
            }
            
            window.addComponentExcpetion(new HstComponentException("The dispatch url is null."));
        } else {
            ClassLoader currentClassloader = switchToContainerClassloader(requestContainerConfig);

            try {
                disp.include(servletRequest, servletResponse);
            } finally {
                if (currentClassloader != null) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
        }
    }
    
    protected void renderErrorInformation(HstContainerConfig requestContainerConfig, ServletRequest servletRequest, ServletResponse servletResponse, HstComponentWindow window) {
        try {
            servletResponse.reset();
            
            if (errorRenderPath != null && errorRenderPath.length() != 0) {
                try {
                    servletRequest.setAttribute("errorComponentWindow", window);
                    invokeDispatcher(requestContainerConfig, servletRequest, servletResponse, errorRenderPath, window);
                    servletResponse.flushBuffer();
                } finally {
                    servletRequest.removeAttribute("errorComponentWindow");
                }
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to dispatch to error page: " + e);
            }
            
            try {
                servletResponse.reset();
            } catch (Exception ignore) {
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
