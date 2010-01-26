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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.MimeResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.hippoecm.hst.core.component.HstPortletResponseState;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.PortletConfigUtils;

public class HstContainerPortlet extends GenericPortlet {
    
    private static final String LOGGER_CATEGORY_NAME = HstContainerPortlet.class.getName();

    /**
     * Name of implementation class name of <CODE>HstPortletRequestDispatcherPathProvider</CODE> interface.
     */
    public static final String HST_PORTLET_REQUEST_DISPATCHER_PATH_PROVIDER   = "hstPortletRequestDispatcherPathProvider.className";
    
    /**
     * Name of portlet preference to allow the use of preferences to set pages
     */
    public static final String PARAM_ALLOW_PREFERENCES   = "AllowPreferences";
    
    /**
     * Name of portlet preference for the servlet path of HST URL
     */
    public static final String HST_SERVLET_PATH_PARAM = "hstServletPath";
    
    /**
     * Name of portlet preference for the path info of HST URL
     */
    public static final String HST_PATH_INFO_PARAM = "hstPathInfo";
    
    /**
     * Name of portlet preference for the path info for edit mode of HST URL
     */
    public static final String HST_PATH_INFO_EDIT_MODE_PARAM = "hstPathInfoEditMode";
    
    /**
     * Name of portlet preference for the portlet title
     */
    public static final String HST_PORTLET_TITLE_PARAM_NAME = "hstPortletTitle";

    /**
     * Name of portlet preference for Header page
     */
    public static final String HST_HEADER_PAGE_PARAM_NAME = "HeaderPage";
    
    /**
     * Name of portlet preference for Edit page
     */
    public static final String HST_HELP_PAGE_PARAM_NAME = "HelpPage";

    /*
     * Name of portlet parameter for internal HST pathInfo url
     */
    public static final String HST_PATH_PARAM_NAME = "_hp";
    
    protected PortletContext portletContext;
    
    protected String defaultHstServletPath = "/preview";
    protected String defaultHstPathInfo;
    protected String defaultHstPathInfoEditMode;
    protected String defaultPortletTitle;
    protected String defaultHeaderPage;
    protected String defaultHelpPage;
    
    protected HstPortletRequestDispatcherPathProvider hstPortletRequestDispatcherPathProvider;

    /**
     * Allow preferences to be set by preferences.
     */
    protected boolean allowPreferences;

    public void init(PortletConfig config) throws PortletException {
        super.init(config);

        this.portletContext = config.getPortletContext();

        String pathInfoProviderClassName = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PORTLET_REQUEST_DISPATCHER_PATH_PROVIDER, DefaultPortletRequestDispatcherImpl.class.getName());
        
        try {
            hstPortletRequestDispatcherPathProvider = (HstPortletRequestDispatcherPathProvider) Thread.currentThread().getContextClassLoader().loadClass(pathInfoProviderClassName.trim()).newInstance();
        } catch (Exception e) {
            throw new PortletException("Cannot create hstPortletRequestDispatcherPathProvider object from " + pathInfoProviderClassName);
        }
        
        hstPortletRequestDispatcherPathProvider.init(config);
        
        allowPreferences = Boolean.parseBoolean(PortletConfigUtils.getInitParameter(config, config.getPortletContext(), PARAM_ALLOW_PREFERENCES, "false"));
        
        defaultHstServletPath = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_SERVLET_PATH_PARAM, defaultHstServletPath);
        
        defaultHstPathInfo = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PATH_INFO_PARAM, defaultHstPathInfo);
        
        defaultHstPathInfoEditMode = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PATH_INFO_EDIT_MODE_PARAM, defaultHstPathInfoEditMode);
        
        defaultPortletTitle = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PORTLET_TITLE_PARAM_NAME, defaultPortletTitle);
        
        defaultHeaderPage = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_HEADER_PAGE_PARAM_NAME, defaultHeaderPage);
        
        defaultHelpPage = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_HELP_PAGE_PARAM_NAME, defaultHelpPage);
    }

    public void destroy() {
        if (hstPortletRequestDispatcherPathProvider != null) {
            hstPortletRequestDispatcherPathProvider.destroy();
        }
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void doHeaders(RenderRequest request, RenderResponse response) {
        String dispatchPage = getPortletModeDispatchPage(request, HST_HEADER_PAGE_PARAM_NAME, defaultHeaderPage);
        
        if (dispatchPage != null) {
            try {
                getPortletContext().getRequestDispatcher(dispatchPage).include(request, response);
            } catch (Exception e) {
                Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                if (logger != null) {
                    logger.warn("Failed to dispatch page - {} : {}", dispatchPage, e);
                }
            }
        }
    }
    
    @Override
    public void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void doHelp(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        String dispatchPage = getPortletModeDispatchPage(request, HST_HELP_PAGE_PARAM_NAME, defaultHelpPage);
        
        if (dispatchPage != null) {
            try {
                getPortletContext().getRequestDispatcher(dispatchPage).include(request, response);
            } catch (Exception e) {
                Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                if (logger != null) {
                    logger.warn("Failed to dispatch page - {} : {}", dispatchPage, e);
                }
            }
        } else {
            processRequest(request, response);
        }
    }
    
    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        if (request.getResourceID() != null) {
            // only handle serveResource by ResourceID parameter
            HstResponseState portletResponseState = new HstPortletResponseState(request, response);
            processMimeResponseRequest(request, response, request.getResourceID(), portletResponseState);
        }
    }
    
    protected String getPortletModeDispatchPage(PortletRequest request, String paramName, String defaultPage) {
        String dispatchPage = (String) request.getAttribute(paramName);
        
        if (dispatchPage == null && allowPreferences) {
            PortletPreferences prefs = request.getPreferences();
            
            if (prefs != null) {
                dispatchPage = prefs.getValue(paramName, defaultPage);
            }
        }
        
        if (dispatchPage == null) {
            dispatchPage = defaultPage;
        }
        
        return dispatchPage;
    }
    
    protected void processRequest(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        
        request.setAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE, response.getNamespace());
        
        boolean isEditMode = PortletMode.EDIT.equals(request.getPortletMode());

        String portletTitle = defaultPortletTitle;
        
        String hstPathInfo = this.defaultHstPathInfo;
        
        if (isEditMode && this.defaultHstPathInfoEditMode != null) {
            hstPathInfo = this.defaultHstPathInfoEditMode;
        }
        
        String hstServletPath = hstPortletRequestDispatcherPathProvider.getServletPath(request);
        
        if (allowPreferences) {
            PortletPreferences prefs = request.getPreferences();
    
            if (prefs != null) {
                String prefValue = prefs.getValue(HST_PORTLET_TITLE_PARAM_NAME, null);
                
                if (prefValue != null) {
                    portletTitle = prefValue;
                }
                
                if (hstServletPath == null) {
                    prefValue = prefs.getValue(HST_SERVLET_PATH_PARAM, null);
                    
                    if (prefValue != null) {
                        hstServletPath = prefValue;
                    }
                }
                
                if (isEditMode) {
                    prefValue = prefs.getValue(HST_PATH_INFO_EDIT_MODE_PARAM, null);
                    
                    if (prefValue == null && this.defaultHstPathInfoEditMode == null) {
                        prefValue = prefs.getValue(HST_PATH_INFO_PARAM, null);
                    }
                } else {
                    prefValue = prefs.getValue(HST_PATH_INFO_PARAM, null);
                }
                
                if (prefValue != null) {
                    hstPathInfo = prefValue;
                }
            }
        }

        if (hstServletPath == null) {
            hstServletPath = this.defaultHstServletPath;
        }
        
        String containerHstPathInfo = hstPortletRequestDispatcherPathProvider.getPathInfo(request);
        
        if (containerHstPathInfo != null) {
            hstPathInfo = containerHstPathInfo;
        }
        
        boolean readDispPathParam = (containerHstPathInfo == null);
        String hstDispUrl = getHstDispatchUrl(request, response, hstServletPath, hstPathInfo, readDispPathParam);
        
        HstResponseState portletResponseState = new HstPortletResponseState(request, response);
        request.setAttribute(HstResponseState.class.getName(), portletResponseState);

        if (portletResponseState.isActionResponse()) {
            String hstActionDispUrl = hstDispUrl;
            int offset = hstActionDispUrl.indexOf('?');
            
            if (offset != -1) {
                hstActionDispUrl = hstActionDispUrl.substring(0, offset);
            }
            
            // create the request dispatcher, to delegate the request to the hst url
            PortletRequestDispatcher rd = getPortletContext().getRequestDispatcher(hstActionDispUrl);
            
            if (rd != null) {
                // delegate to HST Container servlet
                rd.include(request, response);
                processActionResponseState((ActionRequest) request, (ActionResponse) response, hstServletPath, portletResponseState);
            } else {
                throw new PortletException("HST URL Dispatcher is not found: " + hstActionDispUrl);
            }
        } else if (portletResponseState.isMimeResponse()) {
            // must be RenderResponse, ResourceResponse is handled directly from serveResource()
            if (portletTitle != null) {
                ((RenderResponse) response).setTitle(portletTitle);
            }
            
            processMimeResponseRequest(request, (MimeResponse) response, hstDispUrl, portletResponseState);
        } else {
            throw new PortletException("Unsupported Portlet lifecycle: " + request.getAttribute(PortletRequest.LIFECYCLE_PHASE));
        }
    }
    
    protected String getHstDispatchUrl(PortletRequest request, PortletResponse response, String hstServletPath, String hstPathInfo) {
        return getHstDispatchUrl(request, response, hstServletPath, hstPathInfo, true);
    }
    
    protected String getHstDispatchUrl(PortletRequest request, PortletResponse response, String hstServletPath, String hstPathInfo, boolean readDispPathParam) {
        StringBuilder hstDispUrl = new StringBuilder(100);
        
        String hstDispPathParam = null;
        
        if (readDispPathParam) {
            String lifecyclePhase = (String) request.getAttribute(PortletRequest.LIFECYCLE_PHASE);
            boolean isActionResponse = PortletRequest.ACTION_PHASE.equals(lifecyclePhase);
            boolean isViewMode = PortletMode.VIEW.equals(request.getPortletMode());
            
            hstDispPathParam = request.getParameter(HST_PATH_PARAM_NAME + request.getPortletMode().toString());
            
            if (hstDispPathParam == null && (isViewMode || isActionResponse)) {
                hstDispPathParam = request.getParameter(HST_PATH_PARAM_NAME);
            }
        }
        
        if (hstDispPathParam != null) {
            hstDispUrl.append(hstDispPathParam);
        } else {
            hstDispUrl.append(hstServletPath);
            hstDispUrl.append(hstPathInfo);
        }
        
        return hstDispUrl.toString();
    }

    private void processMimeResponseRequest(PortletRequest request, MimeResponse response, String hstDispUrl, HstResponseState portletResponseState) throws PortletException, IOException {
        
        request.setAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE, response.getNamespace());
        
        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(hstDispUrl);
        if (dispatcher != null) {
            dispatcher.include(request, response);
            portletResponseState.flush();
        }
    }
    
    private void processActionResponseState(ActionRequest request, ActionResponse response, String hstServletPath, HstResponseState portletResponseState) throws IOException {
        // write out Cookies to ActionResponse
        portletResponseState.flush();
        
        String redirectLocationUrl = portletResponseState.getRedirectLocation();

        if (redirectLocationUrl != null) {
            if (redirectLocationUrl.startsWith(hstServletPath)) {
                response.setRenderParameter(HST_PATH_PARAM_NAME + request.getPortletMode().toString(), redirectLocationUrl);
            } else {
                response.sendRedirect(redirectLocationUrl);
            }
        }
    }
}
