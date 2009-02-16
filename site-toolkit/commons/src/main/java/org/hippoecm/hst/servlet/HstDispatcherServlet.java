package org.hippoecm.hst.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstComponentFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstComponentInvoker;
import org.hippoecm.hst.core.container.HstComponentInvokerProvider;
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.HstComponentInvokerImpl;

public class HstDispatcherServlet extends HttpServlet {
    
    protected boolean initialized;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        doInit(config);
    }
    
    protected synchronized void doInit(ServletConfig config) throws ServletException {
        if (HstServices.isAvailable()) {
            ServletContext context = config.getServletContext();
            String contextName = context.getServletContextName();
            String servletName = config.getServletName();
            
            HstComponentInvokerProvider invokerProvider = HstServices.getComponentInvokerProvider();
            HstComponentInvoker invoker = new HstComponentInvokerImpl(context, servletName);
            invokerProvider.registerComponentInvoker(contextName, invoker);
            
            HstComponentFactory componentFactory = HstServices.getComponentFactory();
            componentFactory.registerComponentContext(contextName, config, Thread.currentThread().getContextClassLoader());
            
            this.initialized = true;
        }
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        
        if (!this.initialized) {
            doInit(getServletConfig());
        }

        HstRequestProcessor processor = HstServices.getRequestProcessor();
        
        try {
            processor.processRequest(req, res);
        } catch (ContainerException e) {
            throw new ServletException(e);
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    public void destroy() {
        if (HstServices.isAvailable()) {
            ServletContext context = getServletConfig().getServletContext();
            String contextName = context.getServletContextName();
            
            HstComponentInvokerProvider invokerProvider = HstServices.getComponentInvokerProvider();
            invokerProvider.unregisterComponentInvoker(contextName);
            
            HstComponentFactory componentFactory = HstServices.getComponentFactory();
            componentFactory.unregisterComponentContext(contextName);
            
            this.initialized = false;
        }
    }

}
