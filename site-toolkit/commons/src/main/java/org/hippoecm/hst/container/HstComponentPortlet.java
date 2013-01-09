/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstPortletResponseState;
import org.hippoecm.hst.core.component.HstResponseState;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.internal.HstMutablePortletRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstPortalRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.PortletConfigUtils;

public class HstComponentPortlet extends GenericPortlet {

    //private static final String LOGGER_CATEGORY_NAME = HstComponentPortlet.class.getName();

    /**
     * Name of init parameter and/or portlet preference for the target HST component path to render
     */
    public static final String HST_COMPONENT_PATH = "hstComponentPath";
    
    private String defaultHstComponentPath;
    
    /*
     * Name of render parameter for internal portlet render url path
     */
    public static final String HST_PATH_PARAM_NAME = "_hp";
    
    public void init(PortletConfig config) throws PortletException {
        super.init(config);
        defaultHstComponentPath = PortletConfigUtils.getInitParameter(config, config.getPortletContext(), HST_COMPONENT_PATH, null);
    }

    public void destroy() {
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        String hstDispURL = resolveEmbeddedDispatchURL(request, response);
        if (hstDispURL != null) {
            HstResponseState portletResponseState = new HstPortletResponseState(request, response);
            request.setAttribute(HstResponseState.class.getName(), portletResponseState);
            PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(hstDispURL);
            if (dispatcher != null) {
                dispatcher.include(request, response);
                portletResponseState.flush();
            }
        }
    }
    
    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        if (resolveEmbeddedDispatchURL(request, response) != null) {
            // Action overrides/provides full DispURL as parameter
            String hstDispURL = request.getParameter(HST_PATH_PARAM_NAME);
            if (hstDispURL != null) {
                
                HstPortletResponseState portletResponseState = new HstPortletResponseState(request, response);
                request.setAttribute(HstResponseState.class.getName(), portletResponseState);

                // create the request dispatcher, to delegate the request to the hst url
                PortletRequestDispatcher rd = getPortletContext().getRequestDispatcher(hstDispURL);
                
                if (rd != null) {
                    // delegate to HST Container servlet
                    rd.include(request, response);
                    portletResponseState.flush();
                    String redirectLocationUrl = portletResponseState.getRedirectLocation();
                    if (redirectLocationUrl != null) {
                    	if (portletResponseState.isRenderRedirect()) {
                    		HstRequestContext rc = (HstRequestContext)request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
                    		if (!redirectLocationUrl.startsWith(rc.getEmbeddingContextPath())) {
                    			redirectLocationUrl = rc.getEmbeddingContextPath() + redirectLocationUrl;
                    		}
                    	}
                        response.sendRedirect(redirectLocationUrl);
                    }
                } else {
                    throw new PortletException("HST URL Dispatcher is not found: " + hstDispURL);
                }
            }
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        if (request.getResourceID() != null && resolveEmbeddedDispatchURL(request,response) != null) {
            // only handle serveResource by ResourceID parameter
            HstResponseState portletResponseState = new HstPortletResponseState(request, response);
            PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(request.getResourceID());
            if (dispatcher != null) {
                dispatcher.include(request, response);
                portletResponseState.flush();
            }
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
    
    protected String resolveEmbeddedDispatchURL(PortletRequest request, PortletResponse response) {
    	HstPortalContextProvider pcp = getPortalContextProvider(request);
    	HstPortalRequestContext prc = pcp != null ? pcp.getPortalRequestContext(request) : null;
    	ResolvedSiteMapItem rsmi = prc != null ? prc.getResolvedSiteMapItem() : null;
    	HstComponentConfiguration hcc = rsmi != null ? rsmi.getHstComponentConfiguration() : null;
    	if (hcc != null) {
            HstMutablePortletRequestContext hrc = createHstRequestContext(request, response);            
            hrc.setTargetComponentPath(request.getPreferences().getValue(HST_COMPONENT_PATH, defaultHstComponentPath));
            hrc.setEmbeddingContextPath(prc.getEmbeddingContextPath());
            hrc.setResolvedEmbeddingMount(prc.getResolvedEmbeddingMount());
            hrc.setResolvedMount(rsmi.getResolvedMount());
            hrc.setResolvedSiteMapItem(rsmi);
            return new StringBuilder(rsmi.getResolvedMount().getResolvedMountPath()).append("/").append(rsmi.getPathInfo()).toString();
    	}
        return null;
    }
}
