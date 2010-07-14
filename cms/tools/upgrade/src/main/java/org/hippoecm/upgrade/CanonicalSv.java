/*
 *  Copyright 2010 Hippo.
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CanonicalSv {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        if (args.length >= 1 && (args[0].startsWith("-h") || args[0].startsWith("--h"))) {
            System.err.println("Generate a canonical representation from a system view xml file.");
            System.err.println("  Child nodes and properties are ordered according to name and, if necessary, value.");
            System.err.println("  Ordering due to orderable node types is ignored.");
            System.err.println("");
            System.err.println("Usage: CanonicalSv [<file>]");
            System.err.println("  Where <file> is an xml file in the system view JCR format.");
            System.err.println("  With no file argument, standard input is used.");
            System.exit(0);
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;
        if (args.length >= 1) {
            File file = new File(args[0]);
            doc = db.parse(file);
        } else {
            doc = db.parse(System.in);
        }
        doc.getDocumentElement().normalize();
        Item root = new Item("root");
        process(doc.getDocumentElement(), root);
        Output output = new Output(System.out);
        output.render(root, "");
    }

    static String INDENT = "  ";

    static class Output {

        final PrintStream out;

        Output() {
            out = System.out;
        }

        Output(OutputStream stream) {
            out = new PrintStream(stream);
        }

        void render(Item item, String prefix) {
            out.println(prefix + "+ " + item.name + " [" + item.get("jcr:primaryType") + "]");
            for (Map.Entry<String, String> entry : item.entrySet()) {
                if ("jcr:primaryType".equals(entry.getKey())) {
                    continue;
                }
                out.println(prefix + INDENT + "- " + entry.getKey() + ": " + entry.getValue());
            }
            for (Map.Entry<String, Set<Item>> entry : item.children.entrySet()) {
                for (Item child : entry.getValue()) {
                    render(child, prefix + INDENT);
                }
            }
        }
    }

    static class Item extends TreeMap<String, String> implements Comparable<Item> {
        String name;
        SortedMap<String, Set<Item>> children = new TreeMap<String, Set<Item>>();

        Item(String name) {
            this.name = name;
        }

        void add(Item child) {
            if (!children.containsKey(child.name)) {
                children.put(child.name, new TreeSet<Item>());
            }
            children.get(child.name).add(child);
        }

        public int compareTo(Item o) {
            if (o == this) {
                return 0;
            }
            Iterator<Map.Entry<String, String>> thisIter = entrySet().iterator();
            Iterator<Map.Entry<String, String>> thatIter = o.entrySet().iterator();
            while (thisIter.hasNext()) {
                if (!thatIter.hasNext()) {
                    return 1;
                }
                Map.Entry<String, String> thisEntry = thisIter.next();
                Map.Entry<String, String> thatEntry = thatIter.next();
                int keyCmp = thisEntry.getKey().compareTo(thatEntry.getKey());
                if (keyCmp != 0) {
                    return keyCmp;
                }
                int valueCmp = thisEntry.getValue().compareTo(thatEntry.getValue());
                if (valueCmp != 0) {
                    return valueCmp;
                }
            }
            if (thatIter.hasNext()) {
                return -1;
            }
            return 1;
        }
    }

    static void process(Element xmlRoot, Item jcrRoot) {
        Item item = new Item(xmlRoot.getAttribute("sv:name"));

        NodeList nodes = xmlRoot.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if (element.getTagName().equals("sv:property")) {
                    String name = element.getAttribute("sv:name");
                    StringBuilder value = new StringBuilder();
                    boolean first = true;
                    NodeList values = element.getElementsByTagName("sv:value");
                    for (int j = 0; j < values.getLength(); j++) {
                        Node valueNode = values.item(j);
                        if (valueNode instanceof Element) {
                            Element valueEl = (Element) valueNode;
                            String content = valueEl.getTextContent();
                            if (first) {
                                first = false;
                            } else {
                                value.append(' ');
                            }
                            value.append(content);
                        }
                    }
                    item.put(name, value.toString());
                } else if (element.getTagName().equals("sv:node")) {
                    process(element, item);
                }
            }
        }

        jcrRoot.add(item);
    }

}
