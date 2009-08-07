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
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;

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
     * Name of render parameter for internal portlet render url path
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
        
        String param = config.getInitParameter(HST_PORTLET_REQUEST_DISPATCHER_PATH_PROVIDER);
        
        if (param != null) {
            try {
                hstPortletRequestDispatcherPathProvider = (HstPortletRequestDispatcherPathProvider) Thread.currentThread().getContextClassLoader().loadClass(param.trim()).newInstance();
            } catch (Exception e) {
                throw new PortletException("Cannot create hstPortletRequestDispatcherPathProvider object from " + param);
            }
        } else {
            hstPortletRequestDispatcherPathProvider = new DefaultPortletRequestDispatcherImpl();
        }
        
        hstPortletRequestDispatcherPathProvider.init(config);

        param = config.getInitParameter(PARAM_ALLOW_PREFERENCES);
        
        if (param != null) {
            this.allowPreferences = Boolean.parseBoolean(param);
        }

        param = config.getInitParameter(HST_SERVLET_PATH_PARAM);
        
        if (param != null) {
            this.defaultHstServletPath = param;
        }
        
        param = config.getInitParameter(HST_PATH_INFO_PARAM);

        if (param != null) {
            this.defaultHstPathInfo = param;
        }

        param = config.getInitParameter(HST_PATH_INFO_EDIT_MODE_PARAM);
        
        if (param != null) {
            defaultHstPathInfoEditMode = param;
        }
        
        param = config.getInitParameter(HST_PORTLET_TITLE_PARAM_NAME);
        
        if (param != null) {
            defaultPortletTitle = param;
        }

        param = config.getInitParameter(HST_HEADER_PAGE_PARAM_NAME);
        
        if (param != null) {
            defaultHeaderPage = param;
        }
        
        param = config.getInitParameter(HST_HELP_PAGE_PARAM_NAME);
        
        if (param != null) {
            defaultHelpPage = param;
        }
    }

    public void destroy() {
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
            processRequest(request, response);
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
        HstContainerPortletContext.reset(request, response);
        
        try {
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
            
            String hstDispUrl = getHstDispatchUrl(request, response, hstServletPath, hstPathInfo);
            
            HstResponseState portletResponseState = new HstPortletResponseState(request, response);
            request.setAttribute(HstResponseState.class.getName(), portletResponseState);
    
            if (portletResponseState.isActionResponse()) {
                // create the request dispatcher, to delegate the request to the hst url
                PortletRequestDispatcher rd = getPortletContext().getRequestDispatcher(hstDispUrl);
                
                if (rd != null) {
                    // delegate to HST Container servlet
                    rd.include(request, response);
                    processActionResponseState((ActionRequest) request, (ActionResponse) response, hstServletPath, hstDispUrl, portletResponseState);
                } else {
                    throw new PortletException("HST URL Dispatcher is not found: " + hstDispUrl);
                }
            } else if (portletResponseState.isMimeResponse()) {
                if (portletResponseState.isRenderResponse() && portletTitle != null) {
                    ((RenderResponse) response).setTitle(portletTitle);
                }
                
                processMimeResponseRequest(request, (MimeResponse) response, hstDispUrl, portletResponseState);
            } else {
                throw new PortletException("Unsupported Portlet lifecycle: " + request.getAttribute(PortletRequest.LIFECYCLE_PHASE));
            }
        } finally {
            HstContainerPortletContext.reset(null, null);
        }
    }
    
    protected String getHstDispatchUrl(PortletRequest request, PortletResponse response, String hstServletPath, String hstPathInfo) {
        StringBuilder hstDispUrl = new StringBuilder(100);

        String lifecyclePhase = (String) request.getAttribute(PortletRequest.LIFECYCLE_PHASE);
        boolean isActionResponse = PortletRequest.ACTION_PHASE.equals(lifecyclePhase);
        boolean isViewMode = PortletMode.VIEW.equals(request.getPortletMode());
        
        String hstDispPathParam = request.getParameter(HST_PATH_PARAM_NAME + request.getPortletMode().toString());
        
        if (hstDispPathParam == null && (isViewMode || isActionResponse)) {
            hstDispPathParam = request.getParameter(HST_PATH_PARAM_NAME);
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
        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(hstDispUrl);
        dispatcher.include(request, response);
        portletResponseState.flush();
    }
    
    private void processActionResponseState(ActionRequest request, ActionResponse response, String hstServletPath, String hstDispUrl, HstResponseState portletResponseState) throws PortletException, IOException {
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
