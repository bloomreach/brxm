package org.hippoecm.hst.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.HstRequestProcessor;

public class HstDispatcherServlet extends HttpServlet {
    
    protected HstRequestProcessor requestProcessor;
    protected boolean initialized;
    
    public void init(ServletConfig config) throws ServletException {
        doInit(config);
    }
    
    protected void doInit(ServletConfig config) {
        // retrieve the requestProcessor from the Hst Container application.
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        if (!this.initialized) {
            doInit(getServletConfig());
        }
        
        //this.requestProcessor.processRequest(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    public void destroy() {
    }

}
