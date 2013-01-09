/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstPortletResponseState;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.internal.HstMutablePortletRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.PortletConfigUtils;

public class HstContainerPortlet extends GenericPortlet {
    
    private static final String LOGGER_CATEGORY_NAME = HstContainerPortlet.class.getName();

    /**
     * Name of portlet preference to allow the use of preferences to set pages
     */
    public static final String PARAM_ALLOW_PREFERENCES = "AllowPreferences";
    
    /**
     * Name of portlet preference for the {@link Mount} path of HST URL
     */
    public static final String HST_MOUNT_PATH_PARAM = "hstMountPath";
    
    /**
     * Name of portlet preference for the path info of HST URL
     */
    public static final String HST_PATH_INFO_PARAM = "hstPathInfo";
    
    /**
     * Name of portlet preference for the path info of HST URL
     */
    public static final String HST_PORTAL_CONTENT_PATH_BASED = "HstPortalContentPathBased";
    
    /**
     * Name of portlet preference for the path info for edit mode of HST URL
     */
    public static final String HST_PATH_INFO_EDIT_MODE_PARAM = "hstPathInfoEditMode";
    
    /**
     * Name of portlet preference for the target component path
     */
    public static final String HST_TARGET_COMPONENT_PATH_PARAM = "hstTargetComponentPath";
    
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
    
    protected String defaultHstMountPath = "/preview";
    protected String defaultHstPathInfo;
    protected String defaultFallbackHstPathInfo;
    protected String defaultHstPathInfoEditMode;
    protected String defaultHstComponentTargetPath;
    protected String defaultPortletTitle;
    protected String defaultHeaderPage;
    protected String defaultHelpPage;
    protected boolean defaultPortalContentPathBased;
    
    /**
     * Allow preferences to be set by preferences.
     */
    protected boolean allowPreferences;

    public void init(PortletConfig config) throws PortletException {
        super.init(config);

        this.portletContext = config.getPortletContext();

        allowPreferences = Boolean.parseBoolean(PortletConfigUtils.getInitParameter(config, config.getPortletContext(), PARAM_ALLOW_PREFERENCES, "false"));
        
        defaultHstMountPath = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_MOUNT_PATH_PARAM, defaultHstMountPath);
        
        defaultHstPathInfo = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PATH_INFO_PARAM, defaultHstPathInfo);
        
        defaultHstPathInfoEditMode = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PATH_INFO_EDIT_MODE_PARAM, defaultHstPathInfoEditMode);
        
        defaultHstComponentTargetPath = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_TARGET_COMPONENT_PATH_PARAM, defaultHstComponentTargetPath);
        
        defaultPortletTitle = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PORTLET_TITLE_PARAM_NAME, defaultPortletTitle);
        
        defaultHeaderPage = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_HEADER_PAGE_PARAM_NAME, defaultHeaderPage);
        
        defaultHelpPage = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_HELP_PAGE_PARAM_NAME, defaultHelpPage);
        
        defaultPortalContentPathBased = Boolean.parseBoolean(PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_PORTAL_CONTENT_PATH_BASED, "false"));
    }

    public void destroy() {
    }

    @Override
    public void doHeaders(RenderRequest request, RenderResponse response) {
        String dispatchPage = getDispatchPage(request, HST_HEADER_PAGE_PARAM_NAME, defaultHeaderPage);
        
        if (!PortletConfigUtils.isEmpty(dispatchPage)) {
            try {
                dispatch(getPortletContext().getRequestDispatcher(dispatchPage), request, response, true);
            } catch (Exception e) {
                Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                if (logger != null) {
                    logger.warn("Failed to dispatch page - {} : {}", dispatchPage, e);
                }
            }
        }
    }
    
    @Override
    public void doHelp(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        String dispatchPage = getDispatchPage(request, HST_HELP_PAGE_PARAM_NAME, defaultHelpPage);
        
        if (!PortletConfigUtils.isEmpty(dispatchPage)) {
            try {
            	request.setAttribute(ContainerConstants.HST_RESET_FILTER, Boolean.TRUE);
                dispatch(getPortletContext().getRequestDispatcher(dispatchPage), request, response, true);
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
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        if (!PortletConfigUtils.isEmpty(request.getResourceID())) {
            // only handle serveResource by ResourceID parameter
        	HstMutablePortletRequestContext prc = createHstRequestContext(request, response);
        	if (prc != null) {
                HstPortletResponseState portletResponseState = new HstPortletResponseState(request, response);
                processMimeResponseRequest(request, response, request.getResourceID(), portletResponseState);
        	}
        }
    }
   
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    protected String getDispatchPage(PortletRequest request, String pageName, String defaultPage) {
        String dispatchPage = defaultPage;
        
        if (allowPreferences) {
            PortletPreferences prefs = request.getPreferences();
            
            if (prefs != null) {
                dispatchPage = prefs.getValue(pageName, defaultPage);
            }
            if (dispatchPage == null) {
                dispatchPage = defaultPage;
            }
        }
        
        return dispatchPage;
    }
    
    protected void processRequest(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        
    	HstMutablePortletRequestContext prc = createHstRequestContext(request, response);
    	if (prc != null) {
    		
            boolean isEditMode = PortletMode.EDIT.equals(request.getPortletMode());
            String portletTitle = defaultPortletTitle;
            String hstPathInfo = this.defaultHstPathInfo;
            String hstMountPath = null;
            String targetComponentPath = null;
            boolean portalContentPathBased = defaultPortalContentPathBased;
            
            if (isEditMode && !PortletConfigUtils.isEmpty(defaultHstPathInfoEditMode)) {
                hstPathInfo = this.defaultHstPathInfoEditMode;
            }
            
            if (allowPreferences) {
                PortletPreferences prefs = request.getPreferences();
        
                if (prefs != null) {
                    String prefValue = prefs.getValue(HST_PORTLET_TITLE_PARAM_NAME, null);
                    
                    if (prefValue != null) {
                        portletTitle = prefValue;
                    }
                    
                    if (hstMountPath == null) {
                        prefValue = prefs.getValue(HST_MOUNT_PATH_PARAM, null);
                        
                        if (prefValue != null) {
                            hstMountPath = prefValue;
                        }
                    }
                    
                    if (isEditMode) {
                        prefValue = prefs.getValue(HST_PATH_INFO_EDIT_MODE_PARAM, null);
                        
                        if (PortletConfigUtils.isEmpty(prefValue) && PortletConfigUtils.isEmpty(defaultHstPathInfoEditMode)) {
                            prefValue = prefs.getValue(HST_PATH_INFO_PARAM, null);
                        }
                        
                        if (!PortletConfigUtils.isEmpty(prefValue)) {
                            hstPathInfo = prefValue;
                        }
                    } else {
                        prefValue = prefs.getValue(HST_PATH_INFO_PARAM, null);
                        
                        if (!PortletConfigUtils.isEmpty(prefValue)) {
                            hstPathInfo = prefValue;
                        }
                        
                        prefValue = prefs.getValue(HST_PORTAL_CONTENT_PATH_BASED, null);
                        
                        if (!PortletConfigUtils.isEmpty(prefValue)) {
                            portalContentPathBased = Boolean.parseBoolean(prefValue);
                        }
                        
                        prefValue = prefs.getValue(HST_TARGET_COMPONENT_PATH_PARAM, defaultHstComponentTargetPath);
                        
                        if (prefValue != null) {
                            targetComponentPath = prefValue;
                        }
                    }
                }
            }

            if (hstMountPath == null) {
                hstMountPath = this.defaultHstMountPath;
            }
            
            if (portalContentPathBased) {
            	portalContentPathBased = false;
            	HstPortalContextProvider pcp = getPortalContextProvider(request);
            	if (pcp != null) {
            		String portalContentPath = pcp.getContentPath(request);
            		if (!PortletConfigUtils.isEmpty(portalContentPath)) {
            			hstPathInfo = portalContentPath;
            			portalContentPathBased = true;
            		}
            	}
            }
            
            if (targetComponentPath != null) {
            	prc.setTargetComponentPath(targetComponentPath);
            }
            
            String hstDispUrl = getHstDispatchUrl(request, response, hstMountPath, hstPathInfo, !portalContentPathBased);
            
            HstPortletResponseState portletResponseState = new HstPortletResponseState(request, response);
            request.setAttribute(HstResponseState.class.getName(), portletResponseState);
        	
            if (portletResponseState.isActionResponse()) {
                processActionResponseState((ActionRequest) request, (ActionResponse) response, hstDispUrl, portletResponseState);
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
    }
    
    protected void processMimeResponseRequest(PortletRequest request, MimeResponse response, String hstDispUrl, HstPortletResponseState portletResponseState) throws PortletException, IOException {
        
        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(hstDispUrl);
        if (dispatcher != null) {
            dispatch(dispatcher, request, response, true);
            portletResponseState.flush();
        }
    }
    
    protected void processActionResponseState(ActionRequest request, ActionResponse response, String hstDispUrl, HstPortletResponseState portletResponseState) throws PortletException, IOException {

    	PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(hstDispUrl);
        if (dispatcher != null) {
            dispatch(dispatcher, request, response, true);
        	// write out Cookies to ActionResponse
            portletResponseState.flush();
            String redirectLocationUrl = portletResponseState.getRedirectLocation();
            if (redirectLocationUrl != null) {
            	if (portletResponseState.isRenderRedirect()) {
                    response.setRenderParameter(HST_PATH_PARAM_NAME + request.getPortletMode().toString(), redirectLocationUrl);
                } else {
                    response.sendRedirect(redirectLocationUrl);
                }
            }
        } else {
            throw new PortletException("HST URL Dispatcher is not found: " + hstDispUrl);
        }
    }
    
    protected HstPortalContextProvider getPortalContextProvider(PortletRequest request)
    {
    	if (HstServices.isAvailable()) {
    		return HstServices.getComponentManager().getComponent(HstPortalContextProvider.class.getName());
    	}
    	return null;
    }
    
    protected HstMutablePortletRequestContext createHstRequestContext(PortletRequest request, PortletResponse response) {
    	HstMutablePortletRequestContext prc = null;
    	if (HstServices.isAvailable()) {
    		HstRequestContextComponent rcc = HstServices.getComponentManager().getComponent("org.hippoecm.hst.core.internal.HstRequestContextComponent");
    		prc = (HstMutablePortletRequestContext)rcc.create(true);
			prc.setContextNamespace(response.getNamespace());
			prc.setPortletConfig(getPortletConfig());
			prc.setPortletRequest(request);
			prc.setPortletResponse(response);
        	request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, prc);
    	}
    	return prc;
    }
    
    protected String getHstDispatchUrl(PortletRequest request, PortletResponse response, String hstMountPath, String hstPathInfo, boolean readDispPathParam) {
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
            hstDispUrl.append(hstMountPath);
            hstDispUrl.append(hstPathInfo);
        }
        
        return hstDispUrl.toString();
    }
    
    protected void dispatch(PortletRequestDispatcher dispatcher, PortletRequest request, PortletResponse response, boolean include) throws IOException, PortletException {
    	request.setAttribute(ContainerConstants.HST_RESET_FILTER, Boolean.TRUE);
    	try {
    		if (include) {
        		dispatcher.include(request, response);
    		}
    		else {
    			dispatcher.forward(request, response);
    		}
    	}
    	finally {
    		request.removeAttribute(ContainerConstants.HST_RESET_FILTER);
    	}
    }
}
