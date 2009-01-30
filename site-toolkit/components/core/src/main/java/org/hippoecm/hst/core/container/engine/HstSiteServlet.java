package org.hippoecm.hst.core.container.engine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.SpringComponentManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestContextComponent;

/**
 * HST Site Servlet entry point.
 * 
 * @author <a href="mailto:w.ko@onehippo.com">Woonsan Ko</a>
 * @version $Id$
 */
public class HstSiteServlet extends HttpServlet
{
    public static final String CONSOLE_LOGGER = "console";
    
    private static Log log;
    private static Log console;
    
    private String [] appConfigs = { "/WEB-INF/conf/context/applicationContext.xml" };
    private String appConfigFileExtension = ".xml";

    private HstSiteEngine engine;
    private HstRequestContextComponent contextComponent;
    
    // -------------------------------------------------------------------
    // I N I T I A L I Z A T I O N
    // -------------------------------------------------------------------
    private static final String INIT_START_MSG = "HST Site Starting Initialization...";
    private static final String INIT_DONE_MSG = "HST Site Initialization complete, Ready to service requests.";

    /**
     * Intialize Servlet.
     */
    public final void init( ServletConfig config ) throws ServletException
    {
        if ( log == null )
        {
            log = LogFactory.getLog(HstSiteServlet.class);
            console = LogFactory.getLog(CONSOLE_LOGGER);                
        }
        
        console.info(INIT_START_MSG);

        super.init(config);
        
        ServletContext servletContext = config.getServletContext();
        
        String appConfigFileExtensionParam = this.getInitParameter("appConfigFileExtension");
        
        if (appConfigFileExtensionParam != null)
            this.appConfigFileExtension = appConfigFileExtensionParam;
        
        String appConfigDirParam = this.getInitParameter("appConfigDir");
        
        if (appConfigDirParam != null)
        {
            if (appConfigDirParam.endsWith("/"))
                appConfigDirParam = appConfigDirParam.substring(0, appConfigDirParam.length() - 1);
            
            File appConfigDirFile = new File(servletContext.getRealPath(appConfigDirParam));
            
            if (appConfigDirFile.isDirectory())
            {
                String [] fileNames = appConfigDirFile.list(new FilenameFilter() {

                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(appConfigFileExtension);
                    }
                });
                
                this.appConfigs = new String[fileNames.length];
                for (int i = 0; i < fileNames.length; i++)
                {
                    this.appConfigs[i] = appConfigDirParam + "/" + fileNames[i];
                }
            }
        }

        try
        {
            console.info("HSTSiteServlet attempting to create the  portlet engine...");
            engine = new HstSiteEngineImpl(config.getServletContext(), initializeComponentManager());
            console.info("HSTSiteServlet attempting to start the Jetspeed Portal Engine...");
            engine.start();                
            console.info("HSTSiteServlet has successfuly started the Jetspeed Portal Engine....");
            contextComponent = (HstRequestContextComponent) engine.getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        }
        catch (Throwable e)
        {
            // save the exception to complain loudly later :-)
            final String msg = "HSTSite: init() failed.";
            log.fatal(msg, e);
            console.fatal(msg, e);
        }

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
    public final void doGet( HttpServletRequest req, HttpServletResponse res ) throws IOException, ServletException
    {
        HstRequestContext context = null;
        
        try
        {
            // send request through pipeline
            context = contextComponent.create(req, res, getServletConfig());
            engine.service(context);
        }
        catch (Exception e)
        {
            final String msg = "Fatal error encountered while processing portal request: "+e.toString();
            log.fatal(msg, e);
            throw new ServletException(msg, e);
        }
        finally
        {
            if (context != null)
                contextComponent.release(context);
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
    public final void doPost( HttpServletRequest req, HttpServletResponse res ) throws IOException, ServletException
    {
        doGet(req, res);
    }

    // -------------------------------------------------------------------
    // S E R V L E T S H U T D O W N
    // -------------------------------------------------------------------

    public final void destroy()
    {
        log.info("Done shutting down!");
        
        try
        {
            this.engine.shutdown();
        }
        catch (Exception e)
        {
        }
    }

    private ComponentManager initializeComponentManager()
    {
        ServletContext servletContext = getServletConfig().getServletContext();
        String [] configFiles = new String[this.appConfigs.length];
        
        for (int i = 0; i < this.appConfigs.length; i++)
        {
            File file = new File(servletContext.getRealPath(this.appConfigs[i]));
            configFiles[i] = file.toURI().toString();
        }
        
        SpringComponentManager cm = new SpringComponentManager(configFiles);
        cm.init();
        return cm;        
    }
    
}
