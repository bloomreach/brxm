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
 * HST Site Container Servlet, deprecated by {@link HstContextLoaderListener}.
 *
 * <P>
 * This can be used as a servlet, configured like the following in web.xml:
 * </P>
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
 * <P>
 * As you can see above, you can optionally set 'hst-config-properties' servlet init parameter in order to override
 * the servlet context parameter like the following:
 * </P>
 * <PRE><CODE>
 *  &lt;context-param>
 *    &lt;param-name>hst-config-properties&lt;/param-name>
 *    &lt;param-value>/WEB-INF/hst-config.properties&lt;/param-value>
 *  &lt;/context-param>
 * </CODE></PRE>
 * <P>
 * This servlet simply invokes {@link DefaultHstSiteConfigurer} to load HST Context and initialize the container.
 * Please be referred to {@link DefaultHstSiteConfigurer} to see how it finds and loads configurations in detail.
 * </P>
 *
 * @deprecated Use {@link HstContextLoaderListener} instead.
 */
@Deprecated
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
