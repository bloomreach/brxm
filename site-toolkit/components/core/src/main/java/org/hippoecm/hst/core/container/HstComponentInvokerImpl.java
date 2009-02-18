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
    
    protected ServletContext servletContext;
    
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    public void invokeAction(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doAction(hstRequest, hstResponse);
    }

    public void invokeBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doBeforeRender(hstRequest, hstResponse);
    }

    public void invokeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        String dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        invokeDispatcher(servletRequest, servletResponse, dispatchUrl);
    }

    public void invokeBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        HstComponentWindow window = hstRequest.getComponentWindow();
        window.getComponent().doBeforeServeResource(hstRequest, hstResponse);
    }

    public void invokeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        HstRequest hstRequest = (HstRequest) servletRequest;
        HstResponse hstResponse = (HstResponse) servletResponse;
        String dispatchUrl = hstRequest.getComponentWindow().getRenderPath();
        invokeDispatcher(servletRequest, servletResponse, dispatchUrl);
    }

    protected void invokeDispatcher(ServletRequest servletRequest, ServletResponse servletResponse, String dispatchUrl) throws ContainerException {
        RequestDispatcher disp = null;
        
        if (dispatchUrl.startsWith("/")) {
            disp = this.servletContext.getRequestDispatcher(dispatchUrl);
        } else {
            disp = this.servletContext.getNamedDispatcher(dispatchUrl);
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
