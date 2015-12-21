/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HST Site Container Servlet
 * 
 * This servlet should initialize all the components that can be accessed via HstServices
 * from the each HST-based applications.
 * <P>
 * The configuration could be set by a properties file or an xml file.
 * If you would set the configuration by a properties file, you can set an init parameter 
 * named 'hst-config-properties' for the servlet config or for the servlet context. 
 * </P>
 * <P>
 * <EM>The parameter value for the properties file or the xml file is regarded as a web application
 * context relative path or file system relative path if the path does not start with 'file:'.
 * So, you should use a 'file:' prefixed URI for the path parameter value if you want to set an absolute path.
 * When the path starts with a leading slash ('/'), the path is regarded as a servlet context relative path
 * or an absolute file path if the servlet context relative resource is not found.
 * If the path does not start with 'file:' nor with a leading slash ('/'), it is regarded as a relative path of the file system.
 * </EM>
 * </P>
 * <P>
 * For example, you can add an init parameter named 'hst-config-properties' for this servlet config
 * like the following:
 * <PRE><CODE>
 *   &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;init-param>
 *      &lt;param-name>hst-config-properties&lt;/param-name>
 *      &lt;param-value>/WEB-INF/hst-config.properties&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * <BR/>
 * Also, you can set context init parameter instead of the config init parameter like the following: 
 * <PRE><CODE>
 *  &lt;context-param>
 *    &lt;param-name>hst-config-properties&lt;/param-name>
 *    &lt;param-value>/WEB-INF/hst-config.properties&lt;/param-value>
 *  &lt;/context-param>
 *  &lt;!-- SNIP -->
 *  &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * The servlet will retrieve the config init parameter first and it will retrieve the context init parameter
 * when the config init parameter is not set.
 * <BR/>
 * If you don't provide the init parameter named 'hst-config-properties' at all, the value is set to 
 * '/WEB-INF/hst-config.properties' by default.
 * </P>
 * <P>
 * Also, the configuration can be set by an XML file which is of the XML configuration format of
 * <A href="http://commons.apache.org/configuration/">Apache Commons Configuration</A>.
 * If you want to set the configuration by the Apache Commons Configuration XML file, you should provide
 * an init parameter named 'hst-configuration' for servlet config or servlet context. For example,
 * <PRE><CODE>
 *   &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;init-param>
 *      &lt;param-name>hst-configuration&lt;/param-name>
 *      &lt;param-value>/WEB-INF/hst-configuration.xml&lt;/param-value>
 *    &lt;/init-param>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * Also, you can set context init parameter instead of the config init parameter like the following:
 * <PRE><CODE>
 *  &lt;context-param>
 *    &lt;param-name>hst-configuration&lt;/param-name>
 *    &lt;param-value>/WEB-INF/hst-configuration.xml&lt;/param-value>
 *  &lt;/context-param>
 *  &lt;!-- SNIP -->
 *  &lt;servlet>
 *    &lt;servlet-name>HstSiteConfigServlet&lt;/servlet-name>
 *    &lt;servlet-class>org.hippoecm.hst.site.container.HstSiteConfigServlet&lt;/servlet-class>
 *    &lt;load-on-startup>1&lt;/load-on-startup>
 *  &lt;/servlet>
 * </CODE></PRE>
 * <BR/>
 * For your information, you can configure the <CODE>/WEB-INF/hst-configuration.xml</CODE> file like the following example.
 * In this example, you can see that system properties can be aggregated, multiple properties files can be added and 
 * system property values can be used to configure other properties file paths as well: 
 * <PRE><CODE>
 * &lt;?xml version='1.0'?>
 * &lt;configuration>
 *   &lt;system/>
 *   &lt;properties fileName='${catalina.home}/conf/hst-config-1.properties'/>
 *   &lt;properties fileName='${catalina.home}/conf/hst-config-2.properties'/>
 * &lt;/configuration>
 * </CODE></PRE>
 * <EM>Please refer to the documentation of <A href="http://commons.apache.org/configuration/">Apache Commons Configuration</A> for details.</EM>
 * <BR/>
 * The servlet will retrieve the config init parameter first and it will retrieve the context init parameter
 * when the config init parameter is not set.
 * <BR/>
 * If you don't provide the init parameter named 'hst-config-properties' at all, the value is set to 
 * '/WEB-INF/hst-configuration.xml' by default.
 * <BR/>
 * <EM>The parameter value for the properties file or the xml file is regarded as a web application
 * context relative path or file system relative path if the path does not start with 'file:'.
 * So, you should use a 'file:' prefixed URI for the path parameter value if you want to set an absolute path.
 * When the path starts with a leading slash ('/'), the path is regarded as a servlet context relative path.
 * If the path does not start with 'file:' nor with a leading slash ('/'), it is regarded as a relative path of the file system.
 * </EM>
 * </P>
 */
public class HstSiteConfigServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(HstSiteConfigServlet.class);

    /**
     * @deprecated Use {@link HstSiteConfigurer#HST_CONFIGURATION_PARAM} instead.
     */
    @Deprecated
    public static final String HST_CONFIGURATION_PARAM = HstSiteConfigurer.HST_CONFIGURATION_PARAM;

    /**
     * @deprecated Use {@link HstSiteConfigurer#HST_CONFIG_PROPERTIES_PARAM} instead.
     */
    @Deprecated
    public static final String HST_CONFIG_PROPERTIES_PARAM = HstSiteConfigurer.HST_CONFIG_PROPERTIES_PARAM;

    /**
     * @deprecated Use {@link HstSiteConfigurer#HST_SYSTEM_PROPERTIES_OVERRIDE_PARAM} instead.
     */
    @Deprecated
    public static final String HST_SYSTEM_PROPERTIES_OVERRIDE_PARAM = HstSiteConfigurer.HST_SYSTEM_PROPERTIES_OVERRIDE_PARAM;

    /**
     * @deprecated Use {@link HstSiteConfigurer#HST_CONFIG_ENV_PROPERTIES_PARAM} instead.
     */
    @Deprecated
    public static final String HST_CONFIG_ENV_PROPERTIES_PARAM = HstSiteConfigurer.HST_CONFIG_ENV_PROPERTIES_PARAM;

    private HstSiteConfigurer siteConfigurer;

    public HstSiteConfigServlet() {
        super();
    }

    /**
     * Initialize Servlet.
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        try {
            siteConfigurer = new DefaultHstSiteConfigurer();
            ((DefaultHstSiteConfigurer) siteConfigurer).setServletContext(config.getServletContext());
            ((DefaultHstSiteConfigurer) siteConfigurer).setServletConfig(config);
            siteConfigurer.initialize();
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause() instanceof ServletException) {
                throw (ServletException) e.getCause();
            } else {
                throw new ServletException(e);
            }
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
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
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        doGet(req, res);
    }

    // -------------------------------------------------------------------
    // S E R V L E T S H U T D O W N
    // -------------------------------------------------------------------
    @Override
    public synchronized void destroy() {
        if (siteConfigurer != null) {
            try {
                siteConfigurer.destroy();
            } catch (Exception e) {
                log.error("Error occurred while destroying HstSiteConfigurer.", e);
            }
        }
    }

    HstSiteConfigurer getHstSiteConfigurer() {
        return siteConfigurer;
    }

    void setHstSiteConfigurer(HstSiteConfigurer siteConfigurer) {
        this.siteConfigurer = siteConfigurer;
    }
}
