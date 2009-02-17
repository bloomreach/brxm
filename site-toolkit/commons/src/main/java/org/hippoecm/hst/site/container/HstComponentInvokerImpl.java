package org.hippoecm.hst.site.container;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentInvoker;

public class HstComponentInvokerImpl implements HstComponentInvoker {
    
    protected ServletContext servletContext;
    protected String dispatcherPath;
    
    public HstComponentInvokerImpl(ServletContext servletContext, String dispatcherPath) {
        this.servletContext = servletContext;
        this.dispatcherPath = dispatcherPath;
    }

    public void invokeAction(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeDispatcher(servletRequest, servletResponse);
    }

    public void invokeBeforeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeDispatcher(servletRequest, servletResponse);
    }

    public void invokeRender(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeDispatcher(servletRequest, servletResponse);
    }

    public void invokeBeforeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeDispatcher(servletRequest, servletResponse);
    }

    public void invokeServeResource(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        invokeDispatcher(servletRequest, servletResponse);
    }

    protected void invokeDispatcher(ServletRequest servletRequest, ServletResponse servletResponse) throws ContainerException {
        RequestDispatcher disp = null;
        
        if (this.dispatcherPath.startsWith("/")) {
            disp = this.servletContext.getRequestDispatcher(this.dispatcherPath);
        } else {
            disp = this.servletContext.getNamedDispatcher(this.dispatcherPath);
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
