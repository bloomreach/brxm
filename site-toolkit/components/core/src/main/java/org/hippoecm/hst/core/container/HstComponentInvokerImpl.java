package org.hippoecm.hst.core.container;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class HstComponentInvokerImpl implements HstComponentInvoker {
    
    public void invokeAction(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doAction(hstRequest, hstResponse);
    }

    public void invokeBeforeRender(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doBeforeRender(hstRequest, hstResponse);
    }

    public void invokeRender(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        String dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        invokeDispatcher(servletContext, servletRequest, servletResponse, dispatchUrl);
    }

    public void invokeBeforeServeResource(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doBeforeServeResource(hstRequest, hstResponse);
    }

    public void invokeServeResource(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        String dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        invokeDispatcher(servletContext, servletRequest, servletResponse, dispatchUrl);
    }

    protected void invokeDispatcher(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse, String dispatchUrl) throws ContainerException {
        RequestDispatcher disp = null;
        
        if (dispatchUrl != null) {
            if (dispatchUrl.startsWith("/")) {
                disp = servletContext.getRequestDispatcher(dispatchUrl);
            } else {
                disp = servletContext.getNamedDispatcher(dispatchUrl);
            }
        }
        
        if (disp == null) {
            throw new ContainerException("Cannot create request dispatcher for " + dispatchUrl);
        }
        
        try {
            disp.include(servletRequest, servletResponse);
        } catch (ServletException e) {
            throw new ContainerException(e);
        } catch (IOException e) {
            throw new ContainerException(e);
        }
    }
}
