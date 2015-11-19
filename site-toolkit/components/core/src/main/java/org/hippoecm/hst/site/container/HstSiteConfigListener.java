/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default HST Site Container Configuration/Initialization/Destroying Listener.
 * 
 * This should initialize all the components that can be accessed via HstServices
 * from the each HST-based applications.
 * <P>
 * The configuration could be set by a properties file or an xml file.
 * If you would set the configuration by a properties file, you can set an init parameter 
 * named 'hst-config-properties' for the servlet context. 
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
 * For example, you can set context init parameter instead of the config init parameter like the following: 
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
 * <BR/>
 * If you don't provide the init parameter named 'hst-config-properties' at all, the value is set to 
 * '/WEB-INF/hst-config.properties' by default.
 * </P>
 * <P>
 * Also, the configuration can be set by an XML file which is of the XML configuration format of
 * <A href="http://commons.apache.org/configuration/">Apache Commons Configuration</A>.
 * If you want to set the configuration by the Apache Commons Configuration XML file, you should provide
 * an parameter named 'hst-configuration' for servlet context. For example,
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
 * If you don't provide the parameter named 'hst-config-properties' at all, the value is set to 
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
public class HstSiteConfigListener implements ServletContextListener, Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(HstSiteConfigListener.class);

    private HstSiteConfigurer siteConfigurer;

    public HstSiteConfigListener() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            siteConfigurer = new DefaultHstSiteConfigurer();
            ((DefaultHstSiteConfigurer) siteConfigurer).setServletContext(sce.getServletContext());
            siteConfigurer.initialize();
        } catch (Exception e) {
            log.error("Error occurred while initializing HstSiteConfigurer.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (siteConfigurer != null) {
            try {
                siteConfigurer.destroy();
            } catch (Exception e) {
                log.error("Error occurred while destroying HstSiteConfigurer.", e);
            }
        }
    }

}
