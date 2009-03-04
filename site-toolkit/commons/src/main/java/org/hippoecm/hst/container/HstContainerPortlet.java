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
import javax.portlet.Portlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceServingPortlet;

public class HstContainerPortlet implements Portlet, ResourceServingPortlet {
    
    protected PortletContext portletContext;
    protected String containerServletPath = "/content";
    protected String containerPathInfo;

    public void init(PortletConfig config) throws PortletException {
        
        this.portletContext = config.getPortletContext();
        
        String param = config.getInitParameter("containerServletPath");
        
        if (param != null) {
            this.containerServletPath = param;
        }
        
        param = config.getInitParameter("containerPathInfo");
        
        if (param != null) {
            this.containerPathInfo = param;
        }
    }

    public void destroy() {
    }

    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }

    public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }
    
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        processRequest(request, response);
    }
    
    protected void processRequest(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        StringBuilder sbPath = new StringBuilder(this.containerServletPath);
        
        if (this.containerPathInfo != null) {
            sbPath.append(this.containerPathInfo);
        }
        
        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(sbPath.toString());
        
        dispatcher.include(request, response);
    }

}
