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
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.ServletContextAware;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerFactory;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.ServletConfigUtils;

public class HstFilter implements Filter {

    private static final long serialVersionUID = 1L;
    
    private static final String LOGGER_CATEGORY_NAME = HstFilter.class.getName();

    private final static String FILTER_DONE_KEY = "filter.done_"+HstFilter.class.getName();
    private final static String REQUEST_START_TICK_KEY = "request.start_"+HstFilter.class.getName();
  
    private FilterConfig filterConfig;

    /* moved here from HstContainerServlet initialization */
    public static final String CONTEXT_NAMESPACE_INIT_PARAM = "hstContextNamespace";
    public static final String CLIENT_COMPONENT_MANAGER_CLASS_INIT_PARAM = "clientComponentManagerClass";
    public static final String CLIENT_COMPONENT_MANAGER_CONFIGURATIONS_INIT_PARAM = "clientComponentManagerConfigurations";
    public static final String CLIENT_COMPONENT_MANAGER_CONTEXT_ATTRIBUTE_NAME_INIT_PARAM = "clientComponentManagerContextAttributeName";
    public static final String CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME = HstFilter.class.getName() + ".clientComponentManager";
    
    protected String contextNamespace;
    protected String clientComponentManagerClassName;
    protected String [] clientComponentManagerConfigurations;
    protected volatile boolean initialized;
    protected ComponentManager clientComponentManager;
    protected String clientComponentManagerContextAttributeName = CLIENT_COMPONENT_MANANGER_DEFAULT_CONTEXT_ATTRIBUTE_NAME;
    protected volatile HstContainerConfig requestContainerConfig;
    
    protected HstManager hstSitesManager;
    protected HstSiteMapItemHandlerFactory siteMapItemHandlerFactory;
    
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
       
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
            synchronized (this) {
                doInit(filterConfig);
                initialized = true;
            }
            
            Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
            
            if (hstSitesManager != null) {
                siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                if(siteMapItemHandlerFactory == null) {
                    logger.error("Cannot find the siteMapItemHandlerFactory component");
                }
            } else {
                logger.error("Cannot find the virtualHostsManager component for '{}'", HstManager.class.getName());
            }
        }
    }
    
    protected void doInit(FilterConfig config) {
        hstSitesManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        
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

    	if (request.getAttribute(ContainerConstants.HST_RESET_FILTER) != null) {
    		request.removeAttribute(FILTER_DONE_KEY);
    		request.removeAttribute(ContainerConstants.HST_RESET_FILTER);
    	}
    	
		if (request.getAttribute(FILTER_DONE_KEY) != null) {
			chain.doFilter(request, response);
			return;
		}
		
    	HttpServletRequest req = (HttpServletRequest)request;
    	HttpServletResponse res = (HttpServletResponse)response;

    	// Cross-context includes are not (yet) supported to be handled directly by HstFilter
    	// Typical use-case for these is within a portal environment where the portal dispatches to a portlet (within in a separate portlet application)
    	// which *might* dispatch to HST. If such portlet dispatches again it most likely will run through this filter (being by default configured against /*)
    	// but in that case the portlet container will have setup a wrapper request as embedded within this web application (not cross-context).
    	if (isCrossContextInclude(req)) {
    		chain.doFilter(request, response);
    		return;
    	}
    	
		request.setAttribute(FILTER_DONE_KEY, Boolean.TRUE);

    	Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
    	
    	try {
    		if (!HstServices.isAvailable()) {
    			res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    			logger.error("The HST Container Services are not initialized yet.");
    			return;
    		}
    		
    		// ensure ClientComponentManager (if defined) is initialized properly
    		if (!initialized) {
    		    synchronized (this) {
    		        if (!initialized) {
    	                doInit(filterConfig);
    	                initialized = true;
    		        }
    		    }
    		    
                if (hstSitesManager != null) {
                    siteMapItemHandlerFactory = hstSitesManager.getSiteMapItemHandlerFactory();
                    if(siteMapItemHandlerFactory == null) {
                        logger.error("Cannot find the siteMapItemHandlerFactory component");
                    }
                } else {
                    logger.error("Cannot find the virtualHostsManager component for '{}'", HstManager.class.getName());
                }
    		}
    		
    		if(this.siteMapItemHandlerFactory == null || this.hstSitesManager == null) {
    			res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    			logger.error("The HST virtualHostsManager or siteMapItemHandlerFactory is not available");
    			return;
    		}
    		
    		if (requestContainerConfig == null) {
        		synchronized (this) {
        			if (requestContainerConfig == null) {
        				requestContainerConfig = new HstContainerConfigImpl(filterConfig.getServletContext(), Thread.currentThread().getContextClassLoader());
        			}
        		}
    		}
    		
    		if (logger.isDebugEnabled()) {request.setAttribute(REQUEST_START_TICK_KEY, System.nanoTime());}
    		
    		// Sets up the container request wrapper
            HstContainerRequest containerRequest = new HstContainerRequestImpl(req, hstSitesManager.getPathSuffixDelimiter());
            
    		VirtualHosts vHosts = hstSitesManager.getVirtualHosts();

    		
    		// we always want to have the virtualhost available, even when we do not have hst request processing:
    		// We need to know whether to include the contextpath in URL's or not, even for jsp's that are not dispatched by the HST
    		// This info is on the virtual host.
            String hostName = HstRequestUtils.getFarthestRequestHost(containerRequest);
            ResolvedVirtualHost resolvedVirtualHost = vHosts.matchVirtualHost(hostName);
            // when resolvedVirtualHost = null, we cannot do anything else then fall through to the next filter
            if(resolvedVirtualHost == null) {
                logger.warn("hostName '{}' can not be matched. Skip HST Filter and request processing. ", hostName);
                chain.doFilter(request, response);
                return;
            }
            
            request.setAttribute(ContainerConstants.VIRTUALHOSTS_REQUEST_ATTR, resolvedVirtualHost);
    		
    		// when getPathSuffix() is not null, we have a REST url and never skip hst request processing
    		if(vHosts == null || (containerRequest.getPathSuffix() == null && vHosts.isExcluded(containerRequest.getPathInfo()))) {
    		    chain.doFilter(request, response);
    			return;
    		}
    		
    		HstMutableRequestContext requestContext = (HstMutableRequestContext)containerRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);    		
    		if (requestContext == null) {
        		HstRequestContextComponent rcc = (HstRequestContextComponent)HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        		requestContext = rcc.create(false);
        		if (this.contextNamespace != null) {
        			requestContext.setContextNamespace(contextNamespace);
        		}
        		containerRequest.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
    		}
    		requestContext.setServletContext(filterConfig.getServletContext());
    		requestContext.setPathSuffix(containerRequest.getPathSuffix());
    		
    		ResolvedMount mount = requestContext.getResolvedMount();
    		if (mount == null) {
				mount = vHosts.matchMount(hostName, containerRequest.getContextPath() , containerRequest.getPathInfo());
				if(mount != null) {
					requestContext.setResolvedMount(mount);
				} 
				else {
					throw new MatchException("No matching Mount for '"+hostName+"' and '"+containerRequest.getRequestURI()+"'");
				}
    		}
    		
    		// CRUCIAL :  now we set the HST equivalent of the servletPath, namely the Mount#getMountPath as servletPath on the containerRequest
    		((HstContainerRequestImpl)containerRequest).setServletPath(mount.getResolvedMountPath());
    		
			HstURLFactory factory = hstSitesManager.getUrlFactory();
			
    		HstContainerURL hstContainerURL = requestContext.getBaseURL();
    		if (hstContainerURL == null) {
				hstContainerURL = factory.getContainerURLProvider().parseURL(containerRequest, res, mount);
				requestContext.setBaseURL(hstContainerURL);
    		}
    		
    		if (mount.getMount().isMapped()) {
    			ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
    			boolean processSiteMapItemHandlers = false;
    			
    			if (resolvedSiteMapItem == null) {
    				processSiteMapItemHandlers = true;
					resolvedSiteMapItem = mount.matchSiteMapItem(hstContainerURL.getPathInfo());
					if(resolvedSiteMapItem == null) {
						// should not be possible as when it would be null, an exception should have been thrown
					    // on ERROR, we always remove the HST_REQUEST_CONTEXT from the req 
					    req.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
						logger.warn(hostName+"' and '"+containerRequest.getRequestURI()+"' could not be processed by the HST: Error resolving request to sitemap item");
						res.sendError(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
					requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
    			}
    			
				processResolvedSiteMapItem(containerRequest, res, requestContext, processSiteMapItemHandlers, logger);				
					
    		}
    		else {
				if(mount.getNamedPipeline() == null) {
					logger.warn(hostName+"' and '"+containerRequest.getRequestURI()+"' could not be processed by the HST: No hstSite and no custom namedPipeline for Mount");
					// on ERROR, we always remove the HST_REQUEST_CONTEXT from the req 
					req.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
					res.sendError(HttpServletResponse.SC_NOT_FOUND);
				}
				else {
					logger.info("Processing request for pipeline '{}'", mount.getNamedPipeline());
					HstServices.getRequestProcessor().processRequest(this.requestContainerConfig, requestContext, containerRequest, res, mount.getNamedPipeline());
				}
    		}
    	} 
    	catch (MatchException e) {
    	    // on ERROR, we always remove the HST_REQUEST_CONTEXT from the req 
    	    req.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            if(logger.isDebugEnabled()) {
                logger.warn(e.getClass().getName() + " for '"+req.getRequestURI()+"':" , e);
            } else {
                logger.warn(e.getClass().getName() + " for '{}': '{}'" , req.getRequestURI(),  e.getMessage());    
            }
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ContainerException e) {
           // on ERROR, we always remove the HST_REQUEST_CONTEXT from the req 
            req.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            if(logger.isDebugEnabled()) {
                logger.warn(e.getClass().getName() + " for '"+req.getRequestURI()+"':" , e);
            } else {
                logger.warn(e.getClass().getName() + " for '{}': '{}'" , req.getRequestURI(),  e.getMessage());    
            }
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } 
    	catch (Exception e) {
    	    // on ERROR, we always remove the HST_REQUEST_CONTEXT from the req 
    	    req.removeAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
    	    if(logger.isDebugEnabled()) {
                logger.warn("Fatal error encountered while processing request '"+req.getRequestURI()+"':" , e);
            } else {
                logger.warn("Fatal error encountered while processing request '{}': '{}'" , req.getRequestURI(),  e.getMessage());    
            }
    	    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
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
    
    protected void processResolvedSiteMapItem(HttpServletRequest req, HttpServletResponse res, HstMutableRequestContext requestContext, boolean processHandlers, Logger logger) throws ContainerException {
    	ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();

    	if (processHandlers) {
        	// run the sitemap handlers if present: the returned resolvedSiteMapItem can be a different one then the one that is put in
    		resolvedSiteMapItem = processHandlers(resolvedSiteMapItem, req, res);
    		if(resolvedSiteMapItem == null) {
    			// one of the handlers has finished the request already
    			return;
    		}
    		// sync possibly changed ResolvedSiteMapItem
    		requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
    	}

		if (resolvedSiteMapItem.getErrorCode() > 0) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("The resolved sitemap item for {} has error status: {}", requestContext.getBaseURL().getRequestPath(), Integer.valueOf(resolvedSiteMapItem.getErrorCode()));
				}           
				res.sendError(resolvedSiteMapItem.getErrorCode());

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
		
		HstServices.getRequestProcessor().processRequest(this.requestContainerConfig, requestContext, req, res, resolvedSiteMapItem.getNamedPipeline());
		
		// now, as long as there is a forward, we keep invoking processResolvedSiteMapItem: 
        if(req.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) != null) {
            String forwardPathInfo = (String) req.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);
            req.removeAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);

            resolvedSiteMapItem = resolvedSiteMapItem.getResolvedMount().matchSiteMapItem(forwardPathInfo);
            if(resolvedSiteMapItem == null) {
                // should not be possible as when it would be null, an exception should have been thrown
                throw new MatchException("Error resolving request to sitemap item: '"+HstRequestUtils.getFarthestRequestHost(req)+"' and '"+req.getRequestURI()+"'");
            }
            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
            requestContext.setBaseURL(hstSitesManager.getUrlFactory().getContainerURLProvider().createURL(requestContext.getBaseURL(), forwardPathInfo));
            
            processResolvedSiteMapItem(req, res, requestContext, true, logger);
        }
		
		return;
    }

    /**
     * This method is invoked for every {@link HstSiteMapItemHandler} from the resolvedSiteMapItem that was matched from {@link ResolvedMount#matchSiteMapItem(HstContainerURL)}. 
     * If in the for loop the <code>orginalResolvedSiteMapItem</code> switches to a different newResolvedSiteMapItem, then still
     * the handlers for  <code>orginalResolvedSiteMapItem</code> are processed and not the one from <code>newResolvedSiteMapItem</code>. If some intermediate
     * {@link HstSiteMapItemHandler#process(ResolvedSiteMapItem, HttpServletRequest, HttpServletResponse)} returns <code>null</code>, the loop and processing is stooped, 
     * and <code>null</code> is returned. Entire request processing at that point is assumed to be completed already by one of the {@link HstSiteMapItemHandler}s (for 
     * example if one of the handlers is a caching handler). When <code>null</code> is returned, request processing is stopped.
     * @param orginalResolvedSiteMapItem
     * @param req
     * @param res
     * @return a new or original {@link ResolvedSiteMapItem}, or <code>null</code> when request processing can be stopped
     */
    protected ResolvedSiteMapItem processHandlers(ResolvedSiteMapItem orginalResolvedSiteMapItem, HttpServletRequest req, HttpServletResponse res) {
        Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
        
        ResolvedSiteMapItem newResolvedSiteMapItem = orginalResolvedSiteMapItem;
        List<HstSiteMapItemHandlerConfiguration> handlerConfigsFromMatchedSiteMapItem = orginalResolvedSiteMapItem.getHstSiteMapItem().getSiteMapItemHandlerConfigurations();
        for(HstSiteMapItemHandlerConfiguration handlerConfig : handlerConfigsFromMatchedSiteMapItem) {
           HstSiteMapItemHandler handler = siteMapItemHandlerFactory.getSiteMapItemHandlerInstance(requestContainerConfig, handlerConfig);
           logger.debug("Processing siteMapItemHandler for configuration handler '{}'", handlerConfig.getName() );
           try {
               newResolvedSiteMapItem = handler.process(newResolvedSiteMapItem, req, res);
               if(newResolvedSiteMapItem == null) {
                   logger.debug("handler for '{}' return null. Request processing done. Return null", handlerConfig.getName());
                   return null;
               }
           } catch (HstSiteMapItemHandlerException e){
               logger.error("Exception during executing siteMapItemHandler '"+handlerConfig.getName()+"'");
               throw e;
           }
        }
        return newResolvedSiteMapItem;
    }
    
    public synchronized void destroy() {
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
     * Determine if the current request is an cross-context include, as typically exercised by Portals dispatching to a targetted portlet
     * @param request
     * @return
     */
    protected boolean isCrossContextInclude(HttpServletRequest request) {
    	String includeContextPath = (String)request.getAttribute("javax.servlet.include.context_path");
    	return (includeContextPath != null && !includeContextPath.equals(request.getContextPath()));
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
