/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LayoutProvider implements ILayoutProvider {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LayoutProvider.class);

    private static class LayoutEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        String plugin;
        String variant;

        LayoutEntry(String plugin, String variant) {
            this.plugin = plugin;
            this.variant = variant;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof LayoutEntry) {
                LayoutEntry that = (LayoutEntry) obj;
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

    public LayoutProvider(IModel<ClassLoader> loaderModel) {
        this.classLoaderModel = loaderModel;

        layouts = new TreeMap<String, LayoutEntry>();
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);

        final ClassLoader loader = classLoaderModel.getObject();
        if (loader == null) {
            log.error("No class-loader could be obtained from the user session, skip reading layout extensions.");
            return;
        }

        try {
            for (Enumeration<URL> iter = loader.getResources("hippoecm-layouts.xml"); iter.hasMoreElements();) {
                URL configurationURL = iter.nextElement();
                InputStream stream = configurationURL.openStream();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document document = db.parse(stream);

                Element element = document.getDocumentElement();
                if (!"layouts".equals(element.getNodeName())) {
                    throw new RuntimeException("unable to parse layout: no layout node found");
                }

                NodeList nodes = element.getElementsByTagName("layout");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element padElement = (Element) nodes.item(i);

                    NodeList plugins = padElement.getElementsByTagName("plugin");
                    if (plugins.getLength() != 1) {
                        throw new RuntimeException("Invalid layout, 0 or more than 1 plugin child nodes found at " + configurationURL);
                    }

                    Element pluginElement = (Element) plugins.item(0);
                    Node childNode = pluginElement.getFirstChild();
                    String plugin = childNode.getNodeValue();
                    addLayoutEntry(plugin, null);

                    NodeList variants = padElement.getElementsByTagName("variant");
                    for (int j = 0; j < variants.getLength(); j++) {
                        Element variantElement = (Element) variants.item(j);
                        Node variantNode = variantElement.getFirstChild();
                        String variant = variantNode.getNodeValue();
                        addLayoutEntry(plugin, variant);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while reading layouts extension", e);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Parser configuration error:", ex);
        } catch (SAXException ex) {
            throw new RuntimeException("SAX error:", ex);
        }
    }

    protected void addLayoutEntry(String plugin, String variant) {
        String key;
        if (variant == null) {
            key = plugin;
        } else {
            key = plugin + "_" + variant;
        }

        layouts.put(key, new LayoutEntry(plugin, variant));
    }
    
    public List<String> getLayouts() {
        return new ArrayList<String>(layouts.keySet());
    }

    public ILayoutDescriptor getDescriptor(String layout) {
        LayoutEntry entry = layouts.get(layout);
        if (entry != null && entry.variant != null) {
            return new XmlLayoutDescriptor(classLoaderModel, entry.plugin, entry.variant);
        }
        return new XmlLayoutDescriptor(classLoaderModel, layout);
    }

}
