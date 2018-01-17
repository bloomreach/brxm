/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.service;

import java.util.List;
import java.util.Map;

import org.onehippo.cms7.essentials.sdk.api.service.model.Module;

/**
 * Exposes services for manipulating web.xml files.
 */
public interface WebXmlService {

    enum Dispatcher {
        REQUEST,
        FORWARD
    }

    /**
     * Ensure that a certain HST bean class scanning pattern is registered in the project's Site web.xml.
     *
     * @param pattern pattern for classpath scanning of annotated HST beans, e.g. 'org/example/**\/*.class'
     * @return        true if pattern is registered (or already there), false upon error
     */
    boolean addHstBeanClassPattern(String pattern);

    /**
     * Ensure that named Filter exists in the web.xml file of the specified module.
     *
     * If the filter does not yet exist, it gets added with the specified name, class and init parameters.
     *
     * @param module      target module to adjust
     * @param filterName  name of the filter
     * @param filterClass FQCN of the filter class
     * @param initParams  name-value map of init-param's
     * @return            true if the filter exists upon returning, false otherwise.
     */
    boolean addFilter(Module module, String filterName, String filterClass, Map<String, String> initParams);

    /**
     * Add a filter mapping to the web.xml file of the specified module.
     *
     * No attempt is made to avoid duplicate mappings.
     *
     * @param module      target module to adjust
     * @param filterName  name of the filter
     * @param urlPatterns list of URL patterns to map to the filter
     * @return            true if the mapping was added successfully, false otherwise.
     */
    boolean addFilterMapping(Module module, String filterName, List<String> urlPatterns);

    /**
     * Add dispatchers to a filter mapping of the web.xml file of the specified module.
     *
     * @param module      target module to adjust
     * @param filterName  name of the filter
     * @param dispatchers list of dispatchers to be added
     * @return            true if the dispatchers were added successfully, false otherwise.
     */
    boolean addDispatchersToFilterMapping(Module module, String filterName, List<Dispatcher> dispatchers);

    /**
     * Ensure that named servlet exists in the web.xml file of the specified module.
     *
     * @param module        target module to adjust
     * @param servletName   name of the servlet
     * @param servletClass  FQCN of the servlet class
     * @param loadOnStartup optional integer number to control initialization of servlets within web application
     * @return              true if the servlet exists upon returning, false otherwise.
     */
    boolean addServlet(Module module, String servletName, String servletClass, Integer loadOnStartup);

    /**
     * Ensure that a servlet-mapping for the named servlet exists with <b>at least</b> the specified URL patterns,
     * in the web.xml file of the specified module.
     *
     * @param module      target module to adjust
     * @param servletName name of the servlet
     * @param urlPatterns list of URL patterns to put in place
     * @return            true if the servlet mapping exists with all specified URL patters, upon returning. False otherwise
     */
    boolean addServletMapping(Module module, String servletName, List<String> urlPatterns);
}
