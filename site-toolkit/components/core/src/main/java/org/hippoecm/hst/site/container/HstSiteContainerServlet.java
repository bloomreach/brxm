package org.hippoecm.hst.site.container;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.HstRequestProcessor;
import org.hippoecm.hst.site.HstServices;

/**
 * HST Site Servlet entry point.
 * 
 * @author <a href="mailto:w.ko@onehippo.com">Woonsan Ko</a>
 * @version $Id$
 */
public class HstSiteContainerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String CONSOLE_LOGGER = "console";
    
    private static final String INIT_PROPS_PARAM_PREFIX = "properties.";

    private static Log log;
    private static Log console;

    protected ComponentManager componentManager;
    
    protected boolean initialized;
    protected boolean repositoryAvailable;

    // -------------------------------------------------------------------
    // I N I T I A L I Z A T I O N
    // -------------------------------------------------------------------
    private static final String INIT_START_MSG = "HST Site Starting Initialization...";
    private static final String INIT_DONE_MSG = "HST Site Initialization complete, Ready to service requests.";

    /**
     * Intialize Servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        startCheckingRepository(config);
        
        if (isRepositoryAvailable()) {
            doInit(config);
        }
    }
    
    protected void startCheckingRepository(ServletConfig config) {
        String repositoryAddress = config.getInitParameter("properties.repository.address");
        
        if (repositoryAddress.startsWith("rmi:")) {
            // do timer scheduled checking if the repository is up or not.
            // this time, just set it to true.
            this.repositoryAvailable = true;
        } else {
            this.repositoryAvailable = true;
        }
    }
    
    protected boolean isRepositoryAvailable() {
        return this.repositoryAvailable;
    }
    
    protected synchronized void doInit(ServletConfig config) {
        
        if (log == null) {
            log = LogFactory.getLog(HstSiteContainerServlet.class);
            console = LogFactory.getLog(CONSOLE_LOGGER);
        }

        console.info(INIT_START_MSG);

        Properties initProperties = new Properties();
        
        for (Enumeration paramNamesEnum = config.getInitParameterNames(); paramNamesEnum.hasMoreElements(); ) {
            String paramName = (String) paramNamesEnum.nextElement();
            String paramValue = config.getInitParameter(paramName);
            
            if (paramName.startsWith(INIT_PROPS_PARAM_PREFIX) && paramValue != null) {
                initProperties.setProperty(paramName.substring(INIT_PROPS_PARAM_PREFIX.length()).trim(), paramValue.trim());
            }
        }

        try {
            console.info("HSTSiteServlet attempting to create the  portlet engine...");
            this.componentManager = new SpringComponentManager(initProperties);
            console.info("HSTSiteServlet attempting to start the Jetspeed Portal Engine...");
            this.componentManager.initialize();
            this.componentManager.start();
            HstServices.setComponentManager(this.componentManager);
            
            console.info("HSTSiteServlet has successfuly started the Jetspeed Portal Engine....");
        } catch (Throwable e) {
            // save the exception to complain loudly later :-)
            final String msg = "HSTSite: init() failed.";
            log.fatal(msg, e);
            console.fatal(msg, e);
        }

        this.initialized = true;
        
        console.info(INIT_DONE_MSG);
        log.info(INIT_DONE_MSG);
        
    }

    // -------------------------------------------------------------------
    // R E Q U E S T P R O C E S S I N G
    // -------------------------------------------------------------------

    /**
     * The primary method invoked when the Jetspeed servlet is executed.
     * 
     * @param req
     *            Servlet request.
     * @param res
     *            Servlet response.
     * @exception IOException
     *                a servlet exception.
     * @exception ServletException
     *                a servlet exception.
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        
        if (!this.initialized && isRepositoryAvailable()) {
            doInit(getServletConfig());
        }

        try {
            HstRequestProcessor processor = this.componentManager.getComponent(HstRequestProcessor.class.getName());
            processor.processRequest(req, res);
        } catch (Exception e) {
            final String msg = "Fatal error encountered while processing portal request: " + e.toString();
            log.fatal(msg, e);
            throw new ServletException(msg, e);
        }
    }

    /**
     * In this application doGet and doPost are the same thing.
     * 
     * @param req
     *            Servlet request.
     * @param res
     *            Servlet response.
     * @exception IOException
     *                a servlet exception.
     * @exception ServletException
     *                a servlet exception.
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    // -------------------------------------------------------------------
    // S E R V L E T S H U T D O W N
    // -------------------------------------------------------------------

    public void destroy() {
        log.info("Done shutting down!");

        try {
            this.componentManager.stop();
            this.componentManager.close();
        } catch (Exception e) {
        } finally {
            HstServices.setComponentManager(null);
        }
    }

}
