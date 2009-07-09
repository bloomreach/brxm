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
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class LayoutProvider implements ILayoutProvider {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LayoutProvider.class);

    private List<String> layouts;
    private IModel classLoaderModel;

    public LayoutProvider(IModel loaderModel) {
        this.classLoaderModel = loaderModel;

        ClassLoader loader = (ClassLoader) classLoaderModel.getObject();
        layouts = new LinkedList<String>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);

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

                    Node childNode = padElement.getFirstChild();
                    if (childNode instanceof Text) {
                        String layout = childNode.getNodeValue();
                        layouts.add(layout);
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

    public List<String> getLayouts() {
        return layouts;
    }

    public ILayoutDescriptor getDescriptor(String layout) {
        return new XmlLayoutDescriptor(classLoaderModel, layout);
    }

}
