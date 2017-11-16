/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebContextParam;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebFilter;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebFilterMapping;
import org.onehippo.cms7.essentials.dashboard.generated.jaxb.WebXml;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for reading and modifying web.xml files
 *
 * Apparently, there is no library for doing this in a way that preserves inline comments, or we haven't found it yet.
 * Therefore, we use Jaxb annotations on a set of POJO classes to parse the parts of web.xml interesting to the provided
 * utility methods. These Jaxb annotated POJOs are only used for reading/testing te content of a web.xml, modifications
 * (writing) are currently done based on RegEx matching and simple String operations.
 */
public class WebXmlUtils {
    private static final Logger logger = LoggerFactory.getLogger(WebXmlUtils.class);
    private static final Pattern FILTER_PATTERN = Pattern.compile("(?is)<filter>.+?</filter>[^\\n]*\\n");
    private static final Pattern FILTER_MAPPING_PATTERN = Pattern.compile("(?is)<filter-mapping>.+?</filter-mapping>[^\\n]*\\n");
    private static final String HST_BEANS_ANNOTATED_CLASSES = "hst-beans-annotated-classes";

    private WebXmlUtils() {}

    public enum Dispatcher {
        REQUEST,
        FORWARD
    }

    /**
     * Check if a servlet (by name) has already been defined in the specified module's web.xml.
     *
     * @param context     Plugin context for accessing the project
     * @param module      Target module for checking the web.xml file
     * @param servletName Name of the servlet to look for
     * @return            true if found, false otherwise
     */
    public static boolean hasServlet(final PluginContext context, final TargetPom module, final String servletName) {
        final String webXmlPath = ProjectUtils.getWebXmlPath(context, module);
        if (webXmlPath == null) {
            logger.warn("Failed to check for servlet, module '{}' has no web.xml file.", module);
            return false;
        }

        final String selector = "/web-app/*[name()='servlet']/*[name()='servlet-name' and text()='" + servletName + "']";
        try {
            Document doc = new SAXReader().read(new File(webXmlPath));
            return doc.getRootElement().selectSingleNode(selector) != null;
        } catch (DocumentException e) {
            logger.error("Error checking presence of servlet {}", servletName, e);
        }
        return false;
    }

    /**
     * Add a servlet and corresponding mapping to the specified module's web.xml.
     *
     * If the servlet may already be there, use #hasServlet first.
     *
     * @param context       Plugin context for accessing the project
     * @param module        Target module for modifying the web.xml file
     * @param servletName   Name of the servlet to add
     * @param servletClass  Class of the servlet to add
     * @param loadOnStartup Value for the loadOnStartup parameter, may be null
     * @param mappingUrlPatterns Values for the URL patterns of the mapping
     */
    public static void addServlet(final PluginContext context, final TargetPom module, final String servletName,
                                  final Class servletClass, final Integer loadOnStartup, final String[] mappingUrlPatterns) {
        final String webXmlPath = ProjectUtils.getWebXmlPath(context, module);
        if (webXmlPath == null) {
            logger.warn("Failed to add servlet, module '{}' has no web.xml file.", module);
            return;
        }

        final File webXml = new File(webXmlPath);
        try {
            Document doc = new SAXReader().read(webXml);
            Element webApp = (Element)doc.getRootElement().selectSingleNode("/web-app");
            if (webApp != null) {
                Element servlet = Dom4JUtils.addIndentedSameNameSibling(webApp, "servlet", null);
                Dom4JUtils.addIndentedElement(servlet, "servlet-name", servletName);
                Dom4JUtils.addIndentedElement(servlet, "servlet-class", servletClass.getCanonicalName());
                if (loadOnStartup != null) {
                    Dom4JUtils.addIndentedElement(servlet, "load-on-startup", loadOnStartup.toString());
                }

                Element servletMapping = Dom4JUtils.addIndentedSameNameSibling(webApp, "servlet-mapping", null);
                Dom4JUtils.addIndentedElement(servletMapping, "servlet-name", servletName);
                for (String urlPattern : mappingUrlPatterns) {
                    Dom4JUtils.addIndentedElement(servletMapping, "url-pattern", urlPattern);
                }

                FileWriter writer = new FileWriter(webXml);
                doc.write(writer);
                writer.close();
            }
        } catch (DocumentException | IOException e) {
            logger.error("Failed adding servlet '{}' to web.xml of module '{}'.", servletName, module, e);
        }
    }

    /**
     * Add an HST Bean mapping to the site's web.xml.
     *
     * @param context     project context
     * @param beanMapping desired bean mapping
     * @return            true if added, false if already present
     */
    public static boolean addHstBeanMapping(final PluginContext context, final String beanMapping)
            throws FileNotFoundException, JAXBException {
        final String webXmlPath = ProjectUtils.getWebXmlPath(context, TargetPom.SITE);
        final WebXml webXml = readWebXmlFile(webXmlPath);
        final String hstBeansAnnotatedClassesValue = getHstBeansAnnotatedClassesValue(webXml);
        if (hstBeansAnnotatedClassesValue == null) {
            logger.warn(String.format("No '%s' context parameter found in Site web.xml, ignore bean mapping '%s'",
                    HST_BEANS_ANNOTATED_CLASSES, beanMapping));
            return false;
        }

        if (hstBeansAnnotatedClassesValue.contains(beanMapping)) {
            logger.info("HST bean mapping already inplace");
            return false;
        }

        String webXmlContent = readWebXmlFileAsString(webXmlPath);
        webXmlContent = addToHstBeansAnnotatedClassesValue(webXmlContent, hstBeansAnnotatedClassesValue, beanMapping);
        writeWebXmlFile(webXmlPath, webXmlContent);
        return true;
    }

    /**
     * Add a filter to the site's web.xml.
     *
     * @param context     project context
     * @param module      target module
     * @param filterClass class of to-be-added filter
     * @param filter      marshalled filter XML
     * @return            true if added, false if already present
     */
    public static boolean addFilter(final PluginContext context, final TargetPom module, final String filterClass, final String filter)
            throws JAXBException, FileNotFoundException {
        final String webXmlPath = ProjectUtils.getWebXmlPath(context, module);
        final WebXml webXml = readWebXmlFile(webXmlPath);

        if (hasFilter(webXml, filterClass)) {
            logger.info(String.format("Filter for class '%s' already registered in Site web.xml.", filterClass));
            return false;
        }

        String webXmlContent = readWebXmlFileAsString(webXmlPath);
        webXmlContent = addFilter(webXmlContent, filter);
        writeWebXmlFile(webXmlPath, webXmlContent);
        return true;
    }

    /**
     * Insert a filter mapping to the site's web.xml, immediately after a specified other mapping.
     *
     * @param context    project context
     * @param module     target module
     * @param filterName name of the mapping's referenced filter (only 1 mapping per filter supported for now)
     * @param mapping    marshalled filter-mapping XML
     * @param afterFilterName filter-name of the filter-,apping, after which to insert the new mapping
     * @return           true if added, false if already present
     */
    public static boolean insertFilterMapping(final PluginContext context, final TargetPom module,
                                              final String filterName, final String mapping,
                                              final String afterFilterName) throws JAXBException, FileNotFoundException {
        final String webXmlPath = ProjectUtils.getWebXmlPath(context, module);
        final WebXml webXml = readWebXmlFile(webXmlPath);

        if (hasFilterMapping(webXml, filterName)) {
            logger.info(String.format("Filter mapping for filter '%s' already registered in Site web.xml.", filterName));
            return false;
        }

        final int afterIndex = findFilterMapping(webXml, afterFilterName);
        if (afterIndex < 0) {
            logger.warn(String.format("Filter mapping for filter '%s' missing, dropping mapping for filter '%s'.",
                    afterFilterName, filterName));
            return false;
        }

        String webXmlContent = readWebXmlFileAsString(webXmlPath);
        webXmlContent = addFilterMapping(webXmlContent, mapping, afterIndex);
        writeWebXmlFile(webXmlPath, webXmlContent);
        return true;
    }

    /**
     * Add a dispatcher to all mappings for a certain filter
     *
     * @param context    project context
     * @param module     target module
     * @param filterName name of filter for which the dispatcher is added to all mappings
     * @param dispatcher dispatcher to add
     * @return           true if added, false if already present
     */
    public static boolean addDispatcherToFilterMapping(final PluginContext context, final TargetPom module,
                                                       final String filterName, final Dispatcher dispatcher)
            throws JAXBException, FileNotFoundException {
        final String webXmlPath = ProjectUtils.getWebXmlPath(context, module);
        final WebXml webXml = readWebXmlFile(webXmlPath);

        String webXmlContent = GlobalUtils.readStreamAsText(new FileInputStream(webXmlPath));
        boolean added = false;
        for (int i = 0; i < webXml.getFilterMappings().size(); i++) {
            final WebFilterMapping mapping = webXml.getFilterMappings().get(i);
            if (filterName.equals(mapping.getFilterName()) && !hasDispatcher(mapping, dispatcher)) {
                webXmlContent = addDispatcher(webXmlContent, i, dispatcher);
                added = true;
            }
        }

        if (added) {
            GlobalUtils.writeToFile(webXmlContent, new File(webXmlPath).toPath());
            logger.debug("Added dispatcher to {}", webXmlPath);
            return true;
        }

        logger.warn(String.format("Filter mapping for filter '%s' not found, can't add ", filterName));
        return false;
    }

    static boolean hasDispatcher(final WebFilterMapping mapping, final Dispatcher dispatcher) {
        if (mapping.getDispatchers() != null) {
            for (String d : mapping.getDispatchers()) {
                if (dispatcher.toString().equals(d)) {
                    return true;
                }
            }
        }
        return false;
    }

    static String addDispatcher(String webXmlContent, final int filterMappingIndex, final Dispatcher dispatcher) {
        // find correct filterMapping
        final Matcher matcher = FILTER_MAPPING_PATTERN.matcher(webXmlContent);

        int startIndex = -1;
        int endIndex = -1;
        int countDown = filterMappingIndex;
        while (matcher.find()) {
            if (countDown == 0) {
                startIndex = matcher.start();
                endIndex = matcher.end();
                break;
            }
            countDown--;
        }

        final String filterMapping = webXmlContent.substring(startIndex, endIndex);
        final Pattern pattern = Pattern.compile("([^\\n]+)</filter-mapping>");
        final Matcher endMatcher = pattern.matcher(filterMapping);
        if (endMatcher.find()) {
            int insertPoint = startIndex + endMatcher.start();
            webXmlContent = new StringBuffer()
                    .append(webXmlContent.substring(0, insertPoint))
                    .append(endMatcher.group(1)) // indentation
                    .append("  <dispatcher>")
                    .append(dispatcher)
                    .append("</dispatcher>\n")
                    .append(webXmlContent.substring(insertPoint))
                    .toString();
        }

        return webXmlContent;
    }

    private static WebXml readWebXmlFile(final String path) throws JAXBException {
        return (WebXml) JAXBContext.newInstance(WebXml.class).createUnmarshaller().unmarshal(new File(path));
    }

    private static String readWebXmlFileAsString(final String path) throws FileNotFoundException {
        return GlobalUtils.readStreamAsText(new FileInputStream(path));
    }

    private static void writeWebXmlFile(final String path, final String webXmlContent) {
        GlobalUtils.writeToFile(webXmlContent, new File(path).toPath());
    }

    static String getHstBeansAnnotatedClassesValue(final WebXml webXml) {
        final List<WebContextParam> parameters = webXml.getParameters();
        if (parameters != null) {
            for (WebContextParam parameter : parameters) {
                if (HST_BEANS_ANNOTATED_CLASSES.equals(parameter.getParamName())) {
                    return parameter.getParamValue();
                }
            }
        }
        return null;
    }

    static String addToHstBeansAnnotatedClassesValue(final String webXmlContent, final String currentValues,
                                                     final String valueToAdd) {
        final int startIndex = webXmlContent.indexOf(currentValues);
        final String firstPart = webXmlContent.substring(0, startIndex);

        final int endIndex = startIndex + currentValues.length();
        final String secondPart = webXmlContent.substring(endIndex, webXmlContent.length());

        return firstPart + currentValues + ',' + valueToAdd + secondPart;
    }

    static boolean hasFilter(final WebXml webXml, final String filterClass) {
        for (WebFilter filter : webXml.getFilters()) {
            if (filterClass.equals(filter.getFilterClass())) {
                return true;
            }
        }
        return false;
    }

    static String addFilter(final String webXmlContent, final String filter) {
        final Matcher matcher = FILTER_PATTERN.matcher(webXmlContent);

        int endIndex = -1;
        while (matcher.find()) {
            endIndex = matcher.end();
        }

        if (endIndex > 0) {
            final String firstPart = webXmlContent.substring(0, endIndex);
            final String lastPart = webXmlContent.substring(endIndex);

            return firstPart + filter + lastPart;
        } else {
            logger.warn("No <filter> element found to append extra filter, skipping.");
            return webXmlContent;
        }
    }

    static boolean hasFilterMapping(final WebXml webXml, final String filterName) {
        for (WebFilterMapping mapping : webXml.getFilterMappings()) {
            if (filterName.equals(mapping.getFilterName())) {
                return true;
            }
        }
        return false;
    }

    static int findFilterMapping(final WebXml webXml, final String filterName) {
        int index = 0;
        for (WebFilterMapping mapping : webXml.getFilterMappings()) {
            if (filterName.equals(mapping.getFilterName())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    static String addFilterMapping(final String webXmlContent, final String mapping, final int afterIndex) {
        final Matcher matcher = FILTER_MAPPING_PATTERN.matcher(webXmlContent);

        int endIndex = -1;
        int countDown = afterIndex;
        while (matcher.find()) {
            if (countDown == 0) {
                endIndex = matcher.end();
                break;
            }
            countDown--;
        }

        if (endIndex > 0) {
            final String firstPart = webXmlContent.substring(0, endIndex);
            final String lastPart = webXmlContent.substring(endIndex);

            return firstPart + mapping + lastPart;
        } else {
            logger.warn("No <filter-mapping> element found to append extra filter-mapping, skipping.");
            return webXmlContent;
        }
    }
}
