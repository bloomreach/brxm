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
package org.hippoecm.hst.container;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.container.ServletContextAware;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ServletConfigUtils;

public class HstVirtualHostsFilter implements Filter {

    private static final long serialVersionUID = 1L;
    
    private static final String LOGGER_CATEGORY_NAME = HstVirtualHostsFilter.class.getName();

    private final static String FILTER_DONE_KEY = "filter.done_"+HstVirtualHostsFilter.class.getName();
    private final static String REQUEST_START_TICK_KEY = "request.start_"+HstVirtualHostsFilter.class.getName();
    private final static String DEFAULT_WELCOME_PAGE = "home";
    private final static String DEFAULT_PREVIEW_PREFIX = "/preview";
    private final static String WELCOME_PAGE_INIT_PARAM = "welcome-page";
    private final static String PREVIEW_PREFIX_INIT_PARAM = "preview-prefix";
    
    private String welcome_page = DEFAULT_WELCOME_PAGE;
    private String preview_prefix  = DEFAULT_PREVIEW_PREFIX;
    private FilterConfig filterConfig;

    /* moved here from HstContainerServlet initialization */
    public static final String CONTEXT_NAMESPACE_INIT_PARAM = "hstContextNamespace";
    public static final String CLIENT_COMPONENT_MANAGER_CLASS_INIT_PARAM = "clientComponentManagerClass";
    public static final String CLIENT_COMPONENT_MANAGER_CONFIGURATIONS_INIT_PARAM = "clientComponentManagerConfigurations";
    public static final String CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM = "clientComponentManagerContextAttributeName";
    public static final String CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME = HstVirtualHostsFilter.class.getName() + ".clientComponentManager";
    
    protected String contextNamespace;
    protected String clientComponentManagerClassName;
    protected String [] clientComponentManagerConfigurations;
    protected boolean initialized;
    protected ComponentManager clientComponentManager;
    protected String clientComponentManagerContextAttributeName = CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME;
    protected HstContainerConfig requestContainerConfig;
    
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        String customWelcomePage = filterConfig.getInitParameter(WELCOME_PAGE_INIT_PARAM);
        if(customWelcomePage != null) {
            welcome_page = stripSlashes(customWelcomePage);
        }
        String customPreviewPrefix = filterConfig.getInitParameter(PREVIEW_PREFIX_INIT_PARAM);
        if(customPreviewPrefix != null) {
            preview_prefix = "/" + stripSlashes(customPreviewPrefix);
        }
        
        /* HST and ClientComponentManager initialization */

        contextNamespace = getConfigOrContextInitParameter(CONTEXT_NAMESPACE_INIT_PARAM, contextNamespace);
        
        clientComponentManagerClassName = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CLASS_INIT_PARAM, clientComponentManagerClassName);
        
        String param = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CONFIGURATIONS_INIT_PARAM, null);
        
        if (param != null) {
            String [] configs = param.split(",");
            
            for (int i = 0; i < configs.length; i++) {
                configs[i] = configs[i].trim();
            }
            
            clientComponentManagerConfigurations = configs;
        }
        
        clientComponentManagerContextAttributeName = getConfigOrContextInitParameter(CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM, clientComponentManagerContextAttributeName);
        
        initialized = false;
        
        if (HstServices.isAvailable()) {
            doInit(filterConfig);
        }
    }
    
    protected synchronized void doInit(FilterConfig config) {
        if (initialized) {
            return;
        }
        
        if (clientComponentManager != null) {
            try {
                clientComponentManager.stop();
                clientComponentManager.close();
            } 
            catch (Exception e) {
            	// ignored
            } 
            finally {
                clientComponentManager = null;
            }
        }
        
        try {
            if (clientComponentManagerClassName != null && clientComponentManagerConfigurations != null && clientComponentManagerConfigurations.length > 0) {
                clientComponentManager = (ComponentManager) Thread.currentThread().getContextClassLoader().loadClass(clientComponentManagerClassName).newInstance();
                
                if (clientComponentManager instanceof ServletContextAware) {
                    ((ServletContextAware) clientComponentManager).setServletContext(config.getServletContext());
                }
                
                clientComponentManager.setConfigurationResources(clientComponentManagerConfigurations);
                clientComponentManager.initialize();
                clientComponentManager.start();
                config.getServletContext().setAttribute(clientComponentManagerContextAttributeName, clientComponentManager);
            }
        } 
        catch (Exception e) {
            log("Invalid client component manager class or configuration: " + e);
        } 
        finally {
            initialized = true;
        }
    }
    
    /**
     * Returns the client component manager instance if available.
     * @param servletContext
     * @return
     */
    public static ComponentManager getClientComponentManager(ServletContext servletContext) {
        String attributeName = ServletConfigUtils.getInitParameter(null, servletContext, 
                CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM, CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME);
        return (ComponentManager)servletContext.getAttribute(attributeName);
    }
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        
        Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
        
        try {
            if (!HstServices.isAvailable()) {
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                logger.error("The HST Container Services are not initialized yet.");
                return;
            }
            
            // ensure ClientComponentManager (if defined) is initialized properly
            if (!initialized) {
                doInit(filterConfig);
            }
            
            if (this.requestContainerConfig == null) {
                this.requestContainerConfig = new HstContainerConfigImpl(filterConfig.getServletContext(), Thread.currentThread().getContextClassLoader());
            }
            
            if (this.contextNamespace != null) {
                req.setAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE, contextNamespace);
            }
            
            if (logger.isDebugEnabled()) {request.setAttribute(REQUEST_START_TICK_KEY, System.nanoTime());}
            
            String pathInfo = req.getRequestURI().substring(req.getContextPath().length());
            
            if(pathInfo.equals("")) {
                pathInfo += "/"+welcome_page;
            }
            else if(pathInfo.equals("/")) {
                pathInfo += welcome_page;
            } else if (pathInfo.equals(preview_prefix)) {
                pathInfo += "/"+welcome_page; 
            }
            else if (pathInfo.equals(preview_prefix+"/")) {
                pathInfo += welcome_page; 
            }
            
            VirtualHostsManager virtualHostManager = HstServices.getComponentManager().getComponent(VirtualHostsManager.class.getName());
            VirtualHosts vHosts = virtualHostManager.getVirtualHosts();
            if(vHosts == null || vHosts.isExcluded(pathInfo)) {
                chain.doFilter(request, response);
                return;
            }
            
            if (request.getAttribute(FILTER_DONE_KEY) == null) {
                request.setAttribute(FILTER_DONE_KEY, Boolean.TRUE);
                
                ResolvedSiteMapItem resolvedSiteMapItem = null; // vHosts.match((HttpServletRequest)request);
                
                if(resolvedSiteMapItem != null) {
                    if (resolvedSiteMapItem.getErrorCode() > 0) {
                        try {
                            if (logger.isDebugEnabled()) {
                                logger.debug("The resolved sitemap item for {} has error status: {}", pathInfo, Integer.valueOf(resolvedSiteMapItem.getErrorCode()));
                            }           
                            ((HttpServletResponse)response).sendError(resolvedSiteMapItem.getErrorCode());
                            
                        } catch (IOException e) {
                            if (logger.isDebugEnabled()) {
                                logger.warn("Exception invocation on sendError().", e);
                            } else if (logger.isWarnEnabled()) {
                                logger.warn("Exception invocation on sendError().");
                            }
                        }
                        // we're done:
                        return;
                    } 
                    /*
                     * put the matched RESOLVED_SITEMAP_ITEM temporarily on the request, as we do not yet have a HstRequestContext. When the
                     * HstRequestContext is created, and there is a RESOLVED_SITEMAP_ITEM on the request, we put it on the HstRequestContext.
                     */  
                    request.setAttribute(ContainerConstants.RESOLVED_SITEMAP_ITEM, resolvedSiteMapItem);
                                       
                    // TODO  NOW, INVOKE THE HstRequestProcessor !!!
                    
                    if (resolvedSiteMapItem.getNamedPipeline() != null) {
                        req.setAttribute(Pipeline.class.getName(), resolvedSiteMapItem.getNamedPipeline());
                    }
                    HstServices.getRequestProcessor().processRequest(this.requestContainerConfig, req, res);
                    
                } else {
                    
                    chain.doFilter(request, response);
                }
                
            } else {
                chain.doFilter(request, response);
            }
        } 
        catch(RepositoryNotAvailableException e) {
            final String msg = "Fatal error encountered while processing request: " + e.toString();
            throw new ServletException(msg, e);
        } 
        catch (Exception e) {
        	final String msg = "Fatal error encountered while processing request: " + e.toString();
            if (logger != null && logger.isDebugEnabled()) {
            	logger.error(msg, e);
            }
            throw new ServletException(msg, e);
        }
        finally {
            if (logger != null && logger.isDebugEnabled()) {
                long starttick = request.getAttribute(REQUEST_START_TICK_KEY) == null ? 0 : (Long)request.getAttribute(REQUEST_START_TICK_KEY);
                if(starttick != 0) {
                    logger.debug( "Handling request took --({})-- ms for '{}'.", (System.nanoTime() - starttick)/1000000, req.getRequestURI());
                }
            }
        }
    }

    public void destroy() {
        initialized = false;

        if (clientComponentManager != null) {
            try{
                clientComponentManager.stop();
                clientComponentManager.close();
            } 
            catch (Exception e) {
            	// ignored
            } 
            finally {
                clientComponentManager = null;
            }
        }
    }

    /**
     * Writes the specified message to a filter log file, prepended by the
     * filter's name.  See {@link ServletContext#log(String)}.
     *
     * @param msg a <code>String</code> specifying
     * the message to be written to the log file
     */
    private void log(String msg) {
        filterConfig.getServletContext().log(filterConfig.getFilterName() + ": " + msg);
    }
    
    private String getConfigOrContextInitParameter(String paramName, String defaultValue) {
        String value = getInitParameter(filterConfig, filterConfig.getServletContext(), paramName, defaultValue);
        return (value != null ? value.trim() : null);
    }

    private static String stripSlashes(String value) {
        if(value.startsWith("/")) {
            value = value.substring(1);
        }
        if(value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * Retrieves the init parameter from the filterConfig or servletContext.
     * If the init parameter is not found in filterConfig, then it will look up the init parameter from the servletContext.
     * If either filterConfig or servletContext is null, then either is not used to look up the init parameter.
     * If the parameter is not found, then it will return the defaultValue.
     * @param filterConfig filterConfig. If null, this is not used.
     * @param servletContext servletContext. If null, this is not used.
     * @param paramName parameter name
     * @param defaultValue the default value
     * @return
     */
    private static String getInitParameter(FilterConfig filterConfig, ServletContext servletContext, String paramName, String defaultValue) {
        String value = null;
        
        if (value == null && filterConfig != null) {
            value = filterConfig.getInitParameter(paramName);
        }
        
        if (value == null && servletContext != null) {
            value = servletContext.getInitParameter(paramName);
        }
        
        if (value == null) {
            value = defaultValue;
        }
        
        return value;
    }
}
