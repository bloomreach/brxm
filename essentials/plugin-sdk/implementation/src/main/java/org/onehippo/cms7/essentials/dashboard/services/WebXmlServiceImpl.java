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

package org.onehippo.cms7.essentials.dashboard.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.service.WebXmlService;
import org.onehippo.cms7.essentials.dashboard.utils.Dom4JUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import groovy.lang.Singleton;

/**
 * WebXmlServiceImpl provides the actual implementation of a WebXmlService as an injectable Spring bean.
 * It shall *not* maintain any state (instance variables), so that it can be used by multiple clients
 * simultaneously.
 */
@Service
@Singleton
public class WebXmlServiceImpl implements WebXmlService {
    private static final Logger LOG = LoggerFactory.getLogger(WebXmlServiceImpl.class);

    public boolean addHstBeanClassPattern(final PluginContext context, final String pattern) {
        final String paramName = "hst-beans-annotated-classes";
        final String webXmlPath = ProjectUtils.getWebXmlPath(context, TargetPom.SITE);
        if (webXmlPath == null) {
            LOG.warn("Failed to add HST bean class pattern, module 'site' has no web.xml file.");
            return false;
        }

        final File webXml = new File(webXmlPath);
        try {
            Document doc = new SAXReader().read(webXml);
            Element contextParameter = contextParameterFor(paramName, doc);
            if (contextParameter != null) {
                final Element parameterValue = (Element) contextParameter.selectSingleNode("./*[name()='param-value']");
                final String value = parameterValue.getText();
                final String[] mappings = value.trim().split("\\s*,\\s*");
                for (String mapping : mappings) {
                    if (mapping.equals(pattern)) {
                        LOG.debug("HST bean class pattern '{}' already in place.", pattern);
                        return true;
                    }
                }
                parameterValue.setText(addPattern(pattern, value, mappings));
            } else {
                createContextParameter(doc, paramName, pattern);
            }
            write(webXml, doc);
            return true;
        } catch (DocumentException | IOException e) {
            LOG.error("Failed to add HST bean class pattern '{}'.", pattern, e);
        }
        return false;
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

    private static Element contextParameterFor(final String parameterName, final Document doc) {
        final String selector = String.format("/web-app/*[name()='context-param']/*[name()='param-name' and text()='%s']",
                parameterName);
        return parentElementFor(selector, doc);
    }

    private static void createContextParameter(final Document doc, final String name, final String value) {
        final Element webApp = (Element) doc.getRootElement().selectSingleNode("/web-app");
        final Element contextParam = Dom4JUtils.addIndentedSameNameSibling(webApp, "context-param", null);
        Dom4JUtils.addIndentedElement(contextParam, "param-name", name);
        Dom4JUtils.addIndentedElement(contextParam, "param-value", value);
    }

    private static Element parentElementFor(final String selector, final Document doc) {
        final Element element = (Element) doc.getRootElement().selectSingleNode(selector);
        return element != null ? element.getParent() : null;
    }

    private static void write(final File webXml, final Document doc) throws IOException {
        FileWriter writer = new FileWriter(webXml);
        doc.write(writer);
        writer.close();
    }
}
