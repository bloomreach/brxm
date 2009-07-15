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

    public static final String HST_SERVLET_PATH_PARAM = "hstServletPath";
    
    public static final String HST_PATH_INFO_PARAM = "hstPathInfo";
    
    public static final String HST_PATH_PARAM_NAME = "_hp";
    
    public static final String HST_HEADER_PAGE_PARAM_NAME = "HeaderPage";
    
    protected PortletContext portletContext;
    
    protected String hstServletPath = "/content";
    protected String defaultHstPathInfo;
    
    protected String headerPage;

    public void init(PortletConfig config) throws PortletException {
        super.init(config);

        this.portletContext = config.getPortletContext();

        String param = config.getInitParameter(HST_SERVLET_PATH_PARAM);
        
        if (param != null) {
            this.hstServletPath = param;
        }
        
        param = config.getInitParameter(HST_PATH_INFO_PARAM);

        if (param != null) {
            this.defaultHstPathInfo = param;
        }
        
        param = config.getInitParameter(HST_HEADER_PAGE_PARAM_NAME);
        
        if (param != null) {
            headerPage = param;
        }
    }

    public void destroy() {
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
    public void doHelp(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doHeaders(RenderRequest request, RenderResponse response) {
        super.doHeaders(request, response);
        
        if (headerPage != null) {
            try {
                getPortletContext().getRequestDispatcher(headerPage).include(request, response);
            } catch (Exception e) {
                Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
                if (logger != null) {
                    logger.warn("Failed to include header page - {} : {}", headerPage, e);
                }
            }
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

    protected void processRequest(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        HstContainerPortletContext.reset(request, response);
        
        try {
            String hstPathInfo = this.defaultHstPathInfo;
    
            PortletPreferences prefs = request.getPreferences();
    
            if (prefs != null) {
                hstPathInfo = prefs.getValue(HST_PATH_INFO_PARAM, hstPathInfo);
            }
            
            String hstDispUrl = getHstDispatchUrl(request, response, hstPathInfo);
            
            HstResponseState portletResponseState = new HstPortletResponseState(request, response);
            request.setAttribute(HstResponseState.class.getName(), portletResponseState);
    
            if (portletResponseState.isActionResponse()) {
                // create the request dispatcher, to delegate the request to the hst url
                PortletRequestDispatcher rd = getPortletContext().getRequestDispatcher(hstDispUrl);
                
                if (rd != null) {
                    // delegate to HST Container servlet
                    rd.include(request, response);
                    processActionResponseState((ActionRequest) request, (ActionResponse) response, hstDispUrl, portletResponseState);
                } else {
                    throw new PortletException("HST URL Dispatcher is not found: " + hstDispUrl);
                }
            } else if (portletResponseState.isMimeResponse()) {
                processMimeResponseRequest(request, (MimeResponse) response, hstDispUrl, portletResponseState);
            } else {
                throw new PortletException("Unsupported Portlet lifecycle: " + request.getAttribute(PortletRequest.LIFECYCLE_PHASE));
            }
        } finally {
            HstContainerPortletContext.reset(null, null);
        }
    }
    
    protected String getHstDispatchUrl(PortletRequest request, PortletResponse response, String hstPathInfo) {
        StringBuilder hstDispUrl = new StringBuilder(100);
        String hstDispPathParam = request.getParameter(HST_PATH_PARAM_NAME);
        
        if (hstDispPathParam != null) {
            hstDispUrl.append(hstDispPathParam);
        } else {
            hstDispUrl.append(this.hstServletPath);
            hstDispUrl.append(hstPathInfo);
        }
        
        return hstDispUrl.toString();
    }

    private void processMimeResponseRequest(PortletRequest request, MimeResponse response, String hstDispUrl, HstResponseState portletResponseState) throws PortletException, IOException {
        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(hstDispUrl);
        dispatcher.include(request, response);
        portletResponseState.flush();
    }
    
    private void processActionResponseState(ActionRequest request, ActionResponse response, String hstDispUrl, HstResponseState portletResponseState) throws PortletException, IOException {
        // write out Cookies to ActionResponse
        portletResponseState.flush();
        
        String redirectLocationUrl = portletResponseState.getRedirectLocation();

        if (redirectLocationUrl != null) {
            if (redirectLocationUrl.startsWith(this.hstServletPath)) {
                response.setRenderParameter(HST_PATH_PARAM_NAME, redirectLocationUrl);
            } else {
                response.sendRedirect(redirectLocationUrl);
            }
        }
    }

}
