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
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.core.util.resource.UrlResourceStream;
import org.apache.wicket.core.util.resource.locator.ResourceNameIterator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.resource.IPropertiesFactory;
import org.apache.wicket.resource.Properties;
import org.apache.wicket.util.resource.IResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlLayoutDescriptor implements ILayoutDescriptor {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(XmlLayoutDescriptor.class);

    class LayoutPad implements ILayoutPad {
        private static final long serialVersionUID = 1L;

        String name;
        Orientation orientation;

        LayoutPad(String name, Orientation orientation) {
            this.name = name;
            this.orientation = orientation;
        }

        public String getName() {
            return name;
        }

        public ILayoutTransition getTransition(String name) {
            for (ILayoutTransition transition : transitions) {
                if (transition.getName().equals(name)) {
                    return transition;
                }
            }
            throw new RuntimeException("Transition " + name + " was not found");
        }

        public List<String> getTransitions() {
            List<String> result = new LinkedList<String>();
            for (LayoutTransition transition : transitions) {
                if (transition.getSource() == this) {
                    result.add(transition.getName());
                }
            }
            return result;
        }

        public Orientation getOrientation() {
            return orientation;
        }

        public boolean isList() {
            return orientation != null;
        }

    }

    static class LayoutTransition implements ILayoutTransition {
        private static final long serialVersionUID = 1L;

        private String name;
        private ILayoutPad source;
        private ILayoutPad target;

        LayoutTransition(String name, ILayoutPad source, ILayoutPad target) {
            this.name = name;
            this.source = source;
            this.target = target;
        }

        public String getName() {
            return name;
        }

        public ILayoutPad getSource() {
            return source;
        }

        public ILayoutPad getTarget() {
            return target;
        }

    }

    private IModel<ClassLoader> clModel;
    private String location;
    private String variant;
    private Map<String, ILayoutPad> pads;
    private List<LayoutTransition> transitions;

    /**
     * Constructor
     *
     * @param classLoaderModel read-only IModel that returns a ClassLoader
     * @param plugin
     */
    public XmlLayoutDescriptor(IModel<ClassLoader> classLoaderModel, String plugin) {
        this(classLoaderModel, plugin, null);
    }

    public XmlLayoutDescriptor(IModel<ClassLoader> classLoaderModel, String plugin, String variant) {
        this.clModel = classLoaderModel;
        this.location = plugin.replace('.', '/');
        this.variant = variant;

        pads = new LinkedHashMap<String, ILayoutPad>();
        transitions = new LinkedList<LayoutTransition>();

        // Get layout description.  Use the variant description if it is available.
        // Otherwise, fall back to the default.
        ClassLoader cl = clModel.getObject();
        InputStream stream = null;
        if (cl != null) {
            if (variant != null) {
                stream = cl.getResourceAsStream(location + "_" + variant + ".layout.xml");
            }
            if (stream == null) {
                stream = cl.getResourceAsStream(location + ".layout.xml");
            }
        }
        if (stream == null) {
            log.info("No layout descriptor found for " + location);
            return;
        }

        // parse input stream
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(stream);

            Element element = document.getDocumentElement();
            if (!"layout".equals(element.getNodeName())) {
                throw new RuntimeException("unable to parse layout: no layout node found");
            }

            NodeList nodes;

            nodes = element.getElementsByTagName("pad");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element padElement = (Element) nodes.item(i);
                String padName = padElement.getAttribute("name");
                ILayoutPad.Orientation orientation = null;
                if (padElement.hasAttribute("orientation")) {
                    orientation = ILayoutPad.Orientation.VERTICAL;
                    if ("horizontal".equals(padElement.getAttribute("orientation"))) {
                        orientation = ILayoutPad.Orientation.HORIZONTAL;
                    }
                }
                pads.put(padName, new LayoutPad(padName, orientation));
            }

            nodes = element.getElementsByTagName("transition");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element transitionElement = (Element) nodes.item(i);
                String name = transitionElement.getAttribute("name");
                String from = transitionElement.getAttribute("source");
                String to = transitionElement.getAttribute("target");
                transitions.add(new LayoutTransition(name, pads.get(from), pads.get(to)));
            }

        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Parser configuration error:", ex);
        } catch (SAXException ex) {
            throw new RuntimeException("SAX error:", ex);
        } catch (IOException e) {
            throw new RuntimeException("Error reading layout descriptor resource:", e);
        }
    }

    public Map<String, ILayoutPad> getLayoutPads() {
        return pads;
    }

    public IResource getIcon() {
        return new ResourceStreamResource() {

            @Override
            protected IResourceStream getResourceStream() {
                ClassLoader cl = clModel.getObject();
                URL url = null;
                if (variant != null) {
                    url = cl.getResource(location + "_" + variant + ".svg");
                    if (url == null) {
                        url = cl.getResource(location + "_" + variant + ".png");
                    }
                }
                if (url == null) {
                    url = cl.getResource(location + ".svg");
                }
                if (url == null) {
                    url = cl.getResource(location + ".png");
                }
                if (url != null) {
                    return new UrlResourceStream(url);
                } else {
                    cl = getClass().getClassLoader();
                    return new UrlResourceStream(cl.getResource(getClass().getPackage().getName().replace('.', '/')
                            + "/no-layout.png"));
                }
            }

        };
    }

    public String getPluginClass() {
        return location.replace('/', '.');
    }

    public String getVariant() {
        return variant;
    }

    /**
     * Determine the name by use of properties files.  The lookup procedure uses the
     * variant properties file, if available, falling back to the no-variant
     * <code>location + ".properties"</code> file.  The key that is used is the name
     * of the layout (i.e. the last part of its location path), initially tried with
     * the variant appended and falling back to the using the name itself as key if
     * this fails.  Finally, if no properties files are found, the name is returned.
     */
    public IModel<String> getName() {
        return new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                // Load the properties associated with the path
                IPropertiesFactory propertiesFactory = Application.get().getResourceSettings().getPropertiesFactory();

                Locale locale = Session.get().getLocale();
                String name = location.substring(location.lastIndexOf('/') + 1);
                ResourceNameIterator iterator = new ResourceNameIterator(location, null, variant, locale, null, false);
                while (iterator.hasNext()) {
                    String path = iterator.next();
                    final Properties props = propertiesFactory.load(null, path);
                    if (props != null) {
                        if (variant != null) {
                            // Lookup the value
                            String value = props.getString(name + "_" + variant);
                            if (value != null) {
                                return value;
                            }
                        }
                        String value = props.getString(name);
                        if (value != null) {
                            return value;
                        }
                    }
                }
                return name;
            }

        };
    }

}
