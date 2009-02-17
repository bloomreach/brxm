package org.hippoecm.hst.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.container.ContainerConstants;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentFactory;
import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.HstComponentInvoker;
import org.hippoecm.hst.core.container.HstComponentInvokerProvider;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.HstComponentInvokerImpl;

public class HstDispatcherServlet extends HttpServlet {
    
    protected boolean initialized;
    protected ServletContext servletContext;
    protected String dispatcherPath;
    
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.servletContext = config.getServletContext();
        doInit(config);
    }
    
    protected synchronized void doInit(ServletConfig config) throws ServletException {
        if (HstServices.isAvailable()) {
            ServletContext context = config.getServletContext();
            String contextName = context.getServletContextName();

            this.dispatcherPath = config.getServletName();
            
            if (config.getInitParameter("dispatcher") != null) {
                this.dispatcherPath = config.getInitParameter("dispatcher");
            }
            
            HstComponentInvokerProvider invokerProvider = HstServices.getComponentInvokerProvider();
            HstComponentInvoker invoker = new HstComponentInvokerImpl(context, this.dispatcherPath);
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

        Integer method = ContainerConstants.METHOD_NOOP;
        HstComponentWindow window = null;
        HstComponent component = null;
        
        method = (Integer) req.getAttribute(ContainerConstants.HST_COMPONENT_METHOD_ID);
        window = (HstComponentWindow) req.getAttribute(ContainerConstants.HST_COMPONENT_WINDOW);
        component = window.getComponent();

        if (ContainerConstants.METHOD_ACTION.equals(method)) {
            component.doAction((HstRequest) req, (HstResponse) res); 
        } else if (ContainerConstants.METHOD_RENDER.equals(method)) {
            RequestDispatcher dispatcher = this.servletContext.getRequestDispatcher(window.getRenderPath());
            dispatcher.include(req, res);
        } else if (ContainerConstants.METHOD_RESOURCE.equals(method)) {
            RequestDispatcher dispatcher = this.servletContext.getRequestDispatcher(window.getRenderPath());
            dispatcher.include(req, res);
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
