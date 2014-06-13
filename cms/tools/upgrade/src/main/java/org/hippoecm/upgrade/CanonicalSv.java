/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.upgrade;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.upgrade.Value.ValueType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CanonicalSv {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        File file = null;
        String prefixPath = null;
        AbstractOutput output = new TextOutput(System.out);
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (args[i].equals("-j")) {
                    output = new JsonOutput(System.out);
                } else if (args[i].equals("-h")) {
                    System.err.println("Usage: CanonicalSv [OPTIONS] [<file>]");
                    System.err.println("Generate a canonical representation from a system view xml file.");
                    System.err.println("Child nodes and properties are ordered according to name and, if necessary, value.");
                    System.err.println("Ordering due to orderable node types is ignored.");
                    System.err.println();
                    System.err.println("  -h print this help text");
                    System.err.println("  -j output JSON formatted string");
                    System.err.println("  -p <path> qualified jcr path prefix of the original system view xml export");
                    System.err.println();
                    System.err.println("Example: CanonicalSv -p /hippo:configuration/hippo:frontend <file>");
                    System.err.println();
                    System.err.println("<file> is an xml file in the system view JCR format.");
                    System.err.println("With no file argument, standard input is used.");
                    System.exit(0);
                } else if (args[i].equals("-p")) {
                    i++;
                    if (i < args.length) {
                        prefixPath = args[i];
                    }
                }
            } else if (file == null) {
                file = new File(args[i]);
            } else {
                System.err.print("Two input files found; only one is supported");
                System.exit(-1);
            }
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;
        if (file != null) {
            doc = db.parse(file);
        } else {
            doc = db.parse(System.in);
        }
        doc.getDocumentElement().normalize();
        Item root = new Item(null);
        if (prefixPath != null) {
            root.setParentPath(prefixPath);
        }
        process(doc.getDocumentElement(), root);
        output.render(root.children.values().iterator().next().iterator().next());

    }

    static String INDENT = "  ";

    static void process(Element xmlRoot, Item jcrRoot) {
        Item item = new Item(xmlRoot.getAttribute("sv:name"));
        jcrRoot.add(item);

        NodeList nodes = xmlRoot.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals("sv:property")) {
                    String name = element.getAttribute("sv:name");
                    ValueType type = ValueType.fromString(element.getAttribute("sv:type"));
                    List<Value> values = new LinkedList<Value>();
                    NodeList valueNodes = element.getElementsByTagName("sv:value");
                    for (int j = 0; j < valueNodes.getLength(); j++) {
                        Node valueNode = valueNodes.item(j);
                        if (valueNode instanceof Element) {
                            Element valueEl = (Element) valueNode;
                            String content = valueEl.getTextContent();
                            values.add(new Value(content, type));
                        }
                    }
                    item.put(name, values.toArray(new Value[values.size()]));
                } else if (element.getTagName().equals("sv:node")) {
                    process(element, item);
                }
            }
        }
    }

}
