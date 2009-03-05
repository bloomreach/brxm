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

public class HstContainerPortlet extends GenericPortlet {

    public static final String HST_URL_PARAM = "hstUrl";
    
    /**
     * Name of portlet init parameter for Action page
     */
    public static final String PARAM_ACTION_PAGE = "actionPage";
    /**
     * Name of portlet init parameter for Custom page
     */
    public static final String PARAM_CUSTOM_PAGE = "customPage";
    /**
     * Name of portlet init parameter for Edit page
     */
    public static final String PARAM_EDIT_PAGE = "editPage";
    /**
     * Name of portlet init parameter for Edit page
     */
    public static final String PARAM_HELP_PAGE = "helpPage";
    /**
     * Name of portlet init parameter for View page
     */
    public static final String PARAM_VIEW_PAGE = "viewPage";

    protected PortletContext portletContext;
    protected String defaultHstUrl;

    public void init(PortletConfig config) throws PortletException {
        super.init(config);

        this.portletContext = config.getPortletContext();

        String param = config.getInitParameter(HST_URL_PARAM);

        if (param != null) {
            this.defaultHstUrl = param;
        }
    }

    public void destroy() {
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response, PARAM_VIEW_PAGE);
    }

    @Override
    public void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response, PARAM_EDIT_PAGE);
    }

    @Override
    public void doHelp(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response, PARAM_HELP_PAGE);
    }

    public void doCustom(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response, PARAM_CUSTOM_PAGE);
    }

    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        processRequest(request, response, PARAM_ACTION_PAGE);
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        if (request.getResourceID() != null) {
            // only handle serveResource by ResourceID parameter
            processRequest(request, response, PARAM_VIEW_PAGE);
        }
    }

    protected void processRequest(PortletRequest request, PortletResponse response, String pageType) throws PortletException, IOException {
        String hstUrl = this.defaultHstUrl;

        PortletPreferences prefs = request.getPreferences();

        if (prefs != null) {
            hstUrl = prefs.getValue(HST_URL_PARAM, hstUrl);
        }
        
        HstResponseState portletResponseState = new HstPortletResponseState(request, response);
        request.setAttribute(HstResponseState.class.getName(), portletResponseState);

        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(hstUrl);

        dispatcher.include(request, response);
        
        portletResponseState.flush();
    }

}
