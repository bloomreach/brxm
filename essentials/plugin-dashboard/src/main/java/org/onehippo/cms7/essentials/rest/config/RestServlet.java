/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.google.inject.Injector;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.onehippo.cms7.essentials.rest.exc.EssentialsExceptionMapper;
import org.onehippo.cms7.essentials.rest.exc.ExceptionInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class RestServlet extends CXFNonSpringJaxrsServlet {

    private static final Logger log = LoggerFactory.getLogger(RestServlet.class);
    public static final String ATTRIBUTE_INJECTOR = "GuiceCXF#Injector";
    private static final long serialVersionUID = 1L;

    @Override
    protected void createServerFromApplication(String cName, final ServletConfig servletConfig, final String splitChar) throws ServletException {
        final Injector injector = (Injector) servletConfig.getServletContext().getAttribute(ATTRIBUTE_INJECTOR);
        final JAXRSServerFactoryBean bean = injector.getInstance(JAXRSServerFactoryBean.class);
        setAllInterceptors(bean, servletConfig, splitChar);
        setInvoker(bean, servletConfig);
        setExtensions(bean, servletConfig);
        setDocLocation(bean, servletConfig);
        setSchemasLocations(bean, servletConfig);
        bean.setProviders(getProviders(servletConfig, splitChar));
        bean.setBus(getBus());
        bean.create();

    }

    @Override
    protected List<?> getProviders(final ServletConfig servletConfig, final String splitChar) throws ServletException {
        log.info("@@@@  WIRING PROVIDERS");
        final List<Object> providers = new ArrayList<>();

        final JsonProvider provider = new JsonProvider();
        //provider.setIncludeRoot(false);
        providers.add(provider);
        providers.add(new EssentialsExceptionMapper());
        return providers;
    }

    private String getClassNameAndProperties(String cName, Map<String, List<String>> props) {
        String theName = cName.trim();
        int ind = theName.indexOf('(');
        if (ind != -1 && theName.endsWith(")")) {
            props.putAll(parseMapListSequence(theName.substring(ind + 1, theName.length() - 1)));
            theName = theName.substring(0, ind).trim();
        }
        return theName;
    }


}