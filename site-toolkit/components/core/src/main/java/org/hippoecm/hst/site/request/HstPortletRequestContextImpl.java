/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.site.request;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstPortletRequestContext;

/**
 * @version $Id$
 *
 */
public class HstPortletRequestContextImpl extends HstRequestContextImpl implements HstPortletRequestContext {
    
    private PortletConfig portletConfig;
    private PortletRequest portletRequest;
    private PortletResponse portletResponse;

    public HstPortletRequestContextImpl(Repository repository) {
        super(repository);
    }

    public HstPortletRequestContextImpl(Repository repository, Credentials defaultCredentials) {
        super(repository, defaultCredentials);
    }
    
    public void initPortletContext(HttpServletRequest request, HttpServletResponse responsse) {
        portletConfig = (PortletConfig) request.getAttribute("javax.portlet.config");
        portletRequest = (PortletRequest) request.getAttribute("javax.portlet.request");
        portletResponse = (PortletResponse) request.getAttribute("javax.portlet.response");
    }
    
    public boolean isPortletContext() {
        return true;
    }
    
    public PortletConfig getPortletConfig() {
        return portletConfig;
    }
    
    public void setPortletConfig(PortletConfig portletConfig) {
        this.portletConfig = portletConfig;
    }
    
    public PortletRequest getPortletRequest() {
        return portletRequest;
    }

    public void setPortletRequest(PortletRequest portletRequest) {
        this.portletRequest = portletRequest;
    }

    public PortletResponse getPortletResponse() {
        return portletResponse;
    }

    public void setPortletResponse(PortletResponse portletResponse) {
        this.portletResponse = portletResponse;
    }
}
