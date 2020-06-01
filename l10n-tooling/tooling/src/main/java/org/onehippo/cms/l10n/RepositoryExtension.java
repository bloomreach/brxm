/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RepositoryExtension {
    
    private final Document document;
    private Collection<String> items = new ArrayList<>();
    
    private RepositoryExtension(final Document document) {
        this.document = document;
        final NodeList nodes = document.getDocumentElement().getElementsByTagName("sv:node");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element elt = (Element) nodes.item(i);
            items.add(elt.getAttribute("sv:name"));
        }
    }
    
    public void removeResourceBundlesItem(ResourceBundle bundle) throws IOException {
        final String bundlesItemName = getItemName(bundle);
        Element eltToRemove = null;
        if (items.contains(bundlesItemName)) {
            final NodeList nodes = document.getDocumentElement().getElementsByTagName("sv:node");
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element itemElt = (Element) nodes.item(i);
                final String itemName = itemElt.getAttribute("sv:name");
                if (itemName.equals(bundlesItemName)) {
                    eltToRemove = itemElt;
                }
            }
        }
        if (eltToRemove != null) {
            document.getDocumentElement().removeChild(eltToRemove);
            items.remove(bundlesItemName);
        }
    }
    
    public void addResourceBundlesItem(ResourceBundle bundle) throws IOException {
        final String template = IOUtils.toString(getClass().getResourceAsStream("resourcebundles-item-template.xml"), StandardCharsets.UTF_8);
        final String item = template.replace("${itemName}", getItemName(bundle)).replace("${fileName}", bundle.getFileName());
        final Document itemDoc = parse(IOUtils.toInputStream(item, StandardCharsets.UTF_8));
        final Node itemElt = document.importNode(itemDoc.getDocumentElement(), true);
        document.getDocumentElement().appendChild(itemElt);
        items.add(getItemName(bundle));
    }
    
    public boolean containsResourceBundle(ResourceBundle bundle) {
        return items.contains(getItemName(bundle));
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public int getSize() {
        return items.size();
    }

    private String getItemName(final ResourceBundle bundle) {
        return bundle.getModuleName() + "-" + StringUtils.substringBefore(bundle.getFileName().replace('/', '-'), ".json");
    }

    public static RepositoryExtension load(final File file) throws IOException {
        try (final FileInputStream in = new FileInputStream(file)) {
            return load(in);
        }
    }
    
    private static RepositoryExtension load(final InputStream in) throws IOException {
        return new RepositoryExtension(parse(in));
    }

    private static Document parse(final InputStream in) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }
    
    public static RepositoryExtension create() throws IOException {
        try (final InputStream in = RepositoryExtension.class.getResourceAsStream("hippoecm-extension.xml")) {
            return load(in);
        }
    }
    
    public void save(final File file) throws IOException {
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            final Transformer transformer = factory.newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(file));
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }
    
}
