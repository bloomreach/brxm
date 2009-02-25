/*
 *  Copyright 2008 Hippo.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST Container Servlet
 * 
 * This servlet should serve each request
 * 
 * @version $Id$
 */
public class HstContainerServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    public static final String CONTEXT_NAMESPACE_INIT_PARAM = "hstContextNamespace";

    public static final String CONSOLE_LOGGER = "console";

    private final static Logger log = LoggerFactory.getLogger(HstContainerServlet.class);
    
    protected String contextNamespace;
    
    public void init(ServletConfig config) throws ServletException {
        
        super.init(config);
        
        this.contextNamespace = config.getInitParameter(CONTEXT_NAMESPACE_INIT_PARAM);
        
        if (this.contextNamespace == null) {
            this.contextNamespace = config.getServletContext().getInitParameter(CONTEXT_NAMESPACE_INIT_PARAM);
        }

    }
    
    // -------------------------------------------------------------------
    // R E Q U E S T P R O C E S S I N G
    // -------------------------------------------------------------------

    /**
     * The primary method invoked when the servlet is executed.
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        try {
            if (!HstServices.isAvailable()) {
                String msg = "The HST Container Services are not initialized yet.";
                log.warn(msg);
                res.getWriter().println(msg);
                res.flushBuffer();
                
                return;
            }
            
            if (this.contextNamespace != null) {
                req.setAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE, contextNamespace);
            }
            
            HstServices.getRequestProcessor().processRequest(getServletConfig(), req, res);
        } catch (Exception e) {
            final String msg = "Fatal error encountered while processing request: " + e.toString();
            log.error(msg, e);
            throw new ServletException(msg, e);
        }
    
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

}
