package org.hippoecm.hst.portlet;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

public class HstPortlet extends GenericPortlet {
    
    protected PortletContext portletContext;
    protected String dispatcherName = "hstdispatch";

    @Override
    public void init(PortletConfig portletConfig) throws PortletException
    {
        super.init(portletConfig);
        
        if (portletConfig.getInitParameter("dispatcherName") != null) {
            this.dispatcherName = portletConfig.getInitParameter("dispatcherName");
        }
    }
    
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        doHstDispatch(request, response);
    }
    
    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        doHstDispatch(request, response);
    }
    
    protected void doHstDispatch(PortletRequest request, PortletResponse response) throws PortletException, IOException {
        PortletRequestDispatcher dispatcher = null;
        
        if (this.dispatcherName.startsWith("/")) {
            dispatcher = this.portletContext.getRequestDispatcher(this.dispatcherName);
        } else {
            dispatcher = this.portletContext.getNamedDispatcher(this.dispatcherName);
        }
        
        dispatcher.include(request, response);
    }
}
