package org.hippoecm.hst.site.container;

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

public class HstSiteContainerPortlet implements Portlet {
    
    protected PortletContext portletContext;
    protected String siteContainerPath = "/sitecontainer";

    public void init(PortletConfig config) throws PortletException {
        
        this.portletContext = config.getPortletContext();
        
        String param = config.getInitParameter("siteContainerPath");
        
        if (param != null) {
            this.siteContainerPath = param;
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
        PortletRequestDispatcher dispatcher = this.portletContext.getRequestDispatcher(this.siteContainerPath);
        dispatcher.include(request, response);
        
    }

}
