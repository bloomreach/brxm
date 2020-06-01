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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.WebXmlService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.Dom4JUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * WebXmlServiceImpl provides the actual implementation of a WebXmlService as an injectable Spring bean.
 * It shall *not* maintain any state (instance variables), so that it can be used by multiple clients
 * simultaneously.
 */
@Service
public class WebXmlServiceImpl implements WebXmlService {
    private static final Logger LOG = LoggerFactory.getLogger(WebXmlServiceImpl.class);
    private static final String HST_BEANS_ANNOTATED_CLASSES = "hst-beans-annotated-classes";

    private final ProjectService projectService;

    @Inject
    public WebXmlServiceImpl(final ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public boolean addHstBeanClassPattern(final String pattern) {
        return update(Module.SITE, doc -> {
            Element contextParameter = contextParameterFor(HST_BEANS_ANNOTATED_CLASSES, doc);
            if (contextParameter != null) {
                final Element parameterValue = (Element) contextParameter.selectSingleNode("./*[name()='param-value']");
                final String value = parameterValue.getText();
                final String[] mappings = value.trim().split("\\s*,\\s*");
                for (String mapping : mappings) {
                    if (mapping.equals(pattern)) {
                        LOG.debug("HST bean class pattern '{}' already in place.", pattern);
                        return;
                    }
                }
                parameterValue.setText(addPattern(pattern, value, mappings));
            } else {
                createContextParameter(doc, HST_BEANS_ANNOTATED_CLASSES, pattern);
            }
        });
    }

    private String addPattern(final String pattern, final String old, final String[] patterns) {
        if (patterns.length > 1) {
            final String lastEntry = patterns[patterns.length - 1];
            final int startLastEntry = old.indexOf(lastEntry);
            final int endLastEntry = startLastEntry + lastEntry.length();
            final String oneToLastEntry = patterns[patterns.length - 2];
            final int startOneToLastEntry = old.indexOf(oneToLastEntry);
            final int endOneToLastEntry = startOneToLastEntry + oneToLastEntry.length();
            final String separator = old.substring(endOneToLastEntry, startLastEntry);

            return old.substring(0, endLastEntry) + separator + pattern + old.substring(endLastEntry);
        }
        if (patterns.length == 1) {
            return old + ", " + pattern;
        }
        return pattern;
    }

    private Element contextParameterFor(final String parameterName, final Document doc) {
        final String selector = String.format("/web-app/*[name()='context-param']/*[name()='param-name' and text()='%s']",
                parameterName);
        return parentElementFor(selector, doc);
    }

    private void createContextParameter(final Document doc, final String name, final String value) {
        final Element webApp = (Element) doc.getRootElement().selectSingleNode("/web-app");
        final Element contextParam = Dom4JUtils.addIndentedSameNameSibling(webApp, "context-param", null);
        Dom4JUtils.addIndentedElement(contextParam, "param-name", name);
        Dom4JUtils.addIndentedElement(contextParam, "param-value", value);
    }

    @Override
    public boolean addFilter(final Module module, final String filterName,
                             final String filterClass, final Map<String, String> initParams) {
        return update(module, doc -> {
            Element filter = filterFor(filterName, doc);
            if (filter == null) {
                createFilter(doc, filterName, filterClass, initParams);
            }
        });
    }

    private void createFilter(final Document doc, final String filterName, final String filterClass,
                              final Map<String, String> initParams) {
        final Element webApp = (Element) doc.getRootElement().selectSingleNode("/web-app");
        final Element filter = Dom4JUtils.addIndentedSameNameSibling(webApp, "filter", null);
        Dom4JUtils.addIndentedElement(filter, "filter-name", filterName);
        Dom4JUtils.addIndentedElement(filter, "filter-class", filterClass);
        for (String initParamName : initParams.keySet()) {
            final Element initParam = Dom4JUtils.addIndentedElement(filter, "init-param", null);
            Dom4JUtils.addIndentedElement(initParam, "param-name", initParamName);
            Dom4JUtils.addIndentedElement(initParam, "param-value", initParams.get(initParamName));
        }
    }

    private Element filterFor(final String filterName, final Document doc) {
        final String selector = String.format("/web-app/*[name()='filter']/*[name()='filter-name' and text()='%s']",
                filterName);
        return parentElementFor(selector, doc);
    }

    private String filterMappingSelectorFor(final String filterName) {
        return String.format("/web-app/*[name()='filter-mapping']/*[name()='filter-name' and text()='%s']", filterName);
    }

    @Override
    public boolean addFilterMapping(final Module module, final String filterName, final List<String> urlPatterns) {
        return update(module, doc -> {
            final Element webApp = (Element) doc.getRootElement().selectSingleNode("/web-app");
            final Element filterMapping = Dom4JUtils.addIndentedSameNameSibling(webApp, "filter-mapping", null);
            addNameAndPatternsToFilterMapping(filterMapping, filterName, urlPatterns);
        });
    }

    @Override
    public boolean insertFilterMapping(final Module module, final String filterName, final List<String> urlPatterns,
                                       final String insertBefore) {
        return update(module, doc -> {
            final Element webApp = (Element) doc.getRootElement().selectSingleNode("/web-app");
            final String locationSelector = filterMappingSelectorFor(insertBefore);
            final Element filterMapping = Dom4JUtils.insertIndentedElement(webApp, "filter-mapping", locationSelector);
            addNameAndPatternsToFilterMapping(filterMapping, filterName, urlPatterns);
        });
    }

    private void addNameAndPatternsToFilterMapping(final Element mapping, final String filterName, final List<String> urlPatterns) {
        Dom4JUtils.addIndentedElement(mapping, "filter-name", filterName);
        for (String pattern : urlPatterns) {
            Dom4JUtils.addIndentedElement(mapping, "url-pattern", pattern);
        }
    }

    @Override
    public boolean addDispatchersToFilterMapping(final Module module, final String filterName,
                                                 final List<Dispatcher> dispatchers) {
        return update(module, doc -> {
            Element filterMapping = filterMappingFor(filterName, doc);
            if (filterMapping == null) {
                String message = String.format("Failed to find filter-mapping for filter '%s' in web.xml file of module '%s'.",
                        filterName, module.getName());
                throw new Dom4JUtils.ModifierException(message);
            }
            Set<Dispatcher> existingDispatchers = filterMapping.selectNodes("./*[name()='dispatcher']")
                    .stream()
                    .map(n -> Dispatcher.valueOf(n.getText()))
                    .collect(Collectors.toSet());
            for (Dispatcher dispatcher : dispatchers) {
                if (!existingDispatchers.contains(dispatcher)) {
                    Dom4JUtils.addIndentedElement(filterMapping, "dispatcher", dispatcher.toString());
                }
            }
        });
    }

    private Element filterMappingFor(final String filterName, final Document doc) {
        final String selector = String.format("/web-app/*[name()='filter-mapping']/*[name()='filter-name' and text()='%s']",
                filterName);
        return parentElementFor(selector, doc);
    }

    @Override
    public boolean addServlet(final Module module, final String servletName, final String servletClass,
                              final Integer loadOnStartup) {
        return update(module, doc -> {
            Element servlet = servletFor(servletName, doc);
            if (servlet == null) {
                createServlet(doc, servletName, servletClass, loadOnStartup);
            }
        });
    }

    private Element servletFor(final String servletName, final Document doc) {
        final String selector = String.format("/web-app/*[name()='servlet']/*[name()='servlet-name' and text()='%s']",
                servletName);
        return parentElementFor(selector, doc);
    }

    private void createServlet(final Document doc, final String name, final String servletClass,
                               final Integer loadOnStartup) {
        final Element webApp = (Element)doc.getRootElement().selectSingleNode("/web-app");

        final Element servlet = Dom4JUtils.addIndentedSameNameSibling(webApp, "servlet", null);
        Dom4JUtils.addIndentedElement(servlet, "servlet-name", name);
        Dom4JUtils.addIndentedElement(servlet, "servlet-class", servletClass);
        if (loadOnStartup != null) {
            Dom4JUtils.addIndentedElement(servlet, "load-on-startup", loadOnStartup.toString());
        }
    }

    @Override
    public boolean addServletMapping(final Module module, final String servletName, final List<String> urlPatterns) {
        return update(module, doc -> {
            Element servletMapping = servletMappingFor(servletName, doc);
            if (servletMapping == null) {
                servletMapping = createServletMapping(doc, servletName);
            }
            appendUrlPatterns(servletMapping, urlPatterns);
        });
    }

    private Element servletMappingFor(final String servletName, final Document doc) {
        final String selector = String.format("/web-app/*[name()='servlet-mapping']/*[name()='servlet-name' and text()='%s']",
                servletName);
        return parentElementFor(selector, doc);
    }

    private Element createServletMapping(final Document doc, final String servletName) {
        final Element webApp = (Element)doc.getRootElement().selectSingleNode("/web-app");
        final Element servletMapping = Dom4JUtils.addIndentedSameNameSibling(webApp, "servlet-mapping", null);
        Dom4JUtils.addIndentedElement(servletMapping, "servlet-name", servletName);
        return servletMapping;
    }

    private void appendUrlPatterns(final Element servletMapping, final List<String> urlPatterns) {
        Set<String> existingUrlPatterns = servletMapping.selectNodes("./*[name()='url-pattern']")
                .stream()
                .map(Node::getText)
                .collect(Collectors.toSet());
        for (String pattern : urlPatterns) {
            if (!existingUrlPatterns.contains(pattern)) {
                Dom4JUtils.addIndentedElement(servletMapping, "url-pattern", pattern);
            }
        }
    }

    private Element parentElementFor(final String selector, final Document doc) {
        final Element element = (Element) doc.getRootElement().selectSingleNode(selector);
        return element != null ? element.getParent() : null;
    }

    private boolean update(final Module module, final Dom4JUtils.Modifier modifier) {
        final File webXml = projectService.getWebInfPathForModule(module).resolve("web.xml").toFile();

        return Dom4JUtils.update(webXml, modifier);
    }
}
