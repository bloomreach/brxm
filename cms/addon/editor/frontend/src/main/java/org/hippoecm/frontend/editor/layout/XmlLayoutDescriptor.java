/*
 *  Copyright 2009 Hippo.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.UrlResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlLayoutDescriptor implements ILayoutDescriptor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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

    private IModel clModel;
    private String name;
    private Map<String, ILayoutPad> pads;
    private List<LayoutTransition> transitions;

    /**
     * Constructor
     * 
     * @param classLoaderModel read-only IModel that returns a ClassLoader
     * @param layout
     */
    public XmlLayoutDescriptor(IModel/*<ClassLoader>*/classLoaderModel, String layout) {
        this.clModel = classLoaderModel;
        this.name = layout.replace('.', '/');

        pads = new TreeMap<String, ILayoutPad>();
        transitions = new LinkedList<LayoutTransition>();

        ClassLoader cl = (ClassLoader) clModel.getObject();
        InputStream stream = cl.getResourceAsStream(name + ".layout.xml");
        if (stream == null) {
            log.error("No layout descriptor found for " + name);
            return;
        }

        // parse input stream
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
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

    public IResourceStream getIcon() {
        ClassLoader cl = (ClassLoader) clModel.getObject();
        URL url = cl.getResource(name + ".png");
        if (url != null) {
            return new UrlResourceStream(url);
        } else {
            cl = getClass().getClassLoader();
            return new UrlResourceStream(cl.getResource(getClass().getPackage().getName().replace('.', '/')
                    + "/no-layout.png"));
        }
    }

    public String getPluginClass() {
        return name.replace('/', '.');
    }

}
