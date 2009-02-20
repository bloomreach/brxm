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

public class HstContainerPortlet implements Portlet {
    
    protected PortletContext portletContext;
    protected String hstContainerPath = "/hstcontainer";

    public void init(PortletConfig config) throws PortletException {
        
        this.portletContext = config.getPortletContext();
        
        String param = config.getInitParameter("hstContainerPath");
        
        if (param != null) {
            this.hstContainerPath = param;
        }
        
    }

    public void destroy() {
    }

    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        doDispatch(request, response);
    }

    public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        doDispatch(request, response);
    }
    
    protected void doDispatch(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(this.hstContainerPath);
        dispatcher.include(request, response);
    }

}
