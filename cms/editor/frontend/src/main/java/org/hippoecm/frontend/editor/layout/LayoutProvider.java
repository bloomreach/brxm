/*
 *  Copyright 2009-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.layout;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LayoutProvider implements ILayoutProvider {

    private static final Logger log = LoggerFactory.getLogger(LayoutProvider.class);

    private static class LayoutEntry implements Serializable {

        String plugin;
        String variant;

        LayoutEntry(final String plugin, final String variant) {
            this.plugin = plugin;
            this.variant = variant;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof LayoutEntry) {
                final LayoutEntry that = (LayoutEntry) obj;
                return new EqualsBuilder().append(plugin, that.plugin).append(variant, that.variant).isEquals();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return plugin.hashCode() ^ (variant != null ? variant.hashCode() : 0);
        }
    }

    private Map<String, LayoutEntry> layouts;
    private IModel<ClassLoader> classLoaderModel;

    public LayoutProvider(final IModel<ClassLoader> loaderModel) {
        this.classLoaderModel = loaderModel;

        layouts = new LinkedHashMap<>();

        final ClassLoader loader = classLoaderModel.getObject();
        if (loader == null) {
            log.error("No class-loader could be obtained from the user session, skip reading layout extensions.");
            return;
        }

        try {

            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setValidating(false);

            for (Enumeration<URL> iter = loader.getResources("hippoecm-layouts.xml"); iter.hasMoreElements();) {
                final URL configurationURL = iter.nextElement();
                loadLayouts(documentBuilderFactory, configurationURL);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error while reading layouts extension", e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Parser configuration error:", e);
        } catch (SAXException e) {
            throw new IllegalStateException("SAX error:", e);
        }
    }

    private void loadLayouts(final DocumentBuilderFactory documentBuilderFactory, final URL configurationURL) throws IOException, ParserConfigurationException, SAXException {
        try (InputStream stream = configurationURL.openStream()) {
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(stream);

            final Element element = document.getDocumentElement();
            if (!"layouts".equals(element.getNodeName())) {
                throw new IllegalStateException("unable to parse layout: no layout node found");
            }

            final NodeList nodes = element.getElementsByTagName("layout");
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element padElement = (Element) nodes.item(i);
                loadLayoutEntries(configurationURL, padElement);
            }
        }
    }

    private void loadLayoutEntries(final URL configurationURL, final Element padElement) {
        final NodeList plugins = padElement.getElementsByTagName("plugin");
        if (plugins.getLength() != 1) {
            throw new IllegalStateException("Invalid layout, 0 or more than 1 plugin child nodes found at " +
                    configurationURL);
        }

        final Element pluginElement = (Element) plugins.item(0);
        final Node childNode = pluginElement.getFirstChild();
        final String plugin = childNode.getNodeValue();
        addLayoutEntry(plugin, null);

        final NodeList variants = padElement.getElementsByTagName("variant");
        for (int j = 0; j < variants.getLength(); j++) {
            final Element variantElement = (Element) variants.item(j);
            final Node variantNode = variantElement.getFirstChild();
            final String variant = variantNode.getNodeValue();
            addLayoutEntry(plugin, variant);
        }
    }

    protected void addLayoutEntry(final String plugin, final String variant) {
        String key;
        if (variant == null) {
            key = plugin;
        } else {
            key = plugin + "_" + variant;
        }

        layouts.put(key, new LayoutEntry(plugin, variant));
    }

    public List<String> getLayouts() {
        return new ArrayList<>(layouts.keySet());
    }

    public ILayoutDescriptor getDescriptor(final String layout) {
        final LayoutEntry entry = layouts.get(layout);
        if (entry != null && entry.variant != null) {
            return new XmlLayoutDescriptor(classLoaderModel, entry.plugin, entry.variant);
        }
        return new XmlLayoutDescriptor(classLoaderModel, layout);
    }

}
