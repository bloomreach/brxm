package org.hippoecm.hst.container;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.site.HstServices;

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

    private static Log log;
    private static Log console;
    
    protected ServletContext servletContext;
    protected String contextNamespace;
    
    public void init(ServletConfig config) throws ServletException {
        
        super.init(config);
        
        this.servletContext = config.getServletContext();
        this.contextNamespace = config.getInitParameter(CONTEXT_NAMESPACE_INIT_PARAM);
        
        if (this.contextNamespace == null) {
            this.contextNamespace = this.servletContext.getInitParameter(CONTEXT_NAMESPACE_INIT_PARAM);
        }

        if (log == null) {
            log = LogFactory.getLog(HstContainerServlet.class);
            console = LogFactory.getLog(CONSOLE_LOGGER);
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
            if (this.contextNamespace != null) {
                req.setAttribute(ContainerConstants.CONTEXT_NAMESPACE_ATTRIBUTE, contextNamespace);
            }
            
            HstServices.getRequestProcessor().processRequest(this.servletContext, req, res);
        } catch (Exception e) {
            final String msg = "Fatal error encountered while processing request: " + e.toString();
            log.fatal(msg, e);
            throw new ServletException(msg, e);
        }
    
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

}
