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

package org.onehippo.cms7.essentials.dashboard.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

/**
 * ContextXMLUtils is used for manipulating Tomcat context.xml files
 */
public class ContextXMLUtils {
    private ContextXMLUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static Logger log = LoggerFactory.getLogger(ContextXMLUtils.class);

    static boolean hasResources(Document doc) {
        return !doc.selectNodes("//Context/Resource").isEmpty();
    }

    static boolean hasResource(Document doc, String resource) {
        return !doc.selectNodes("//Context/Resource[@name='" + resource + "']").isEmpty();
    }

    public static boolean hasResource(File contextXML, String resource) {
        Document doc;
        try {
            doc = new SAXReader().read(contextXML);
            return hasResource(doc, resource);
        } catch (DocumentException e) {
            log.error("Error reading {}", contextXML.getAbsolutePath(), e);
        }
        return false;
    }

    static Document addResource(Document doc, String name, String resourceBlob) {
        Element context = (Element) doc.selectSingleNode("//Context");

        SAXReader reader = new SAXReader();
        try {
            Document blob = reader.read(new StringReader(resourceBlob));
            Element e = blob.getRootElement();
            Dom4JUtils.addIndentedElement(context, e);
        } catch (DocumentException e) {
            log.error("Error adding resource {}", name, e);
        }
        return doc;
    }

    public static void addResource(File contextXML, String name, String resourceBlob) {
        Document doc;
        try {
            doc = new SAXReader().read(contextXML);
            addResource(doc, name, resourceBlob);
            writeResource(doc, contextXML);
        } catch (DocumentException | IOException e) {
            log.error("Error adding resource to {}", contextXML.getAbsolutePath(), e);
        }
    }

    static boolean hasEnvironment(Document doc, String environment) {
        return !doc.selectNodes("//Context/Environment[@name='" + environment + "']").isEmpty();
    }

    static Document removeEnvironment(Document doc, String environment) {
        Node env = doc.selectSingleNode("//Context/Environment[@name='" + environment + "']");
        if(env != null) {
            env.getParent().remove(env);
        }
        return doc;
    }

    /* Add an environment element to context.xml. Example:
        <Environment name="elasticsearch/targetingDS" type="java.lang.String"
                     value="{'indexName':'visits', 'locations':['http://localhost:9200/']}" />
     */
    static Document addEnvironment(Document doc, String name, String value, String type, boolean overwrite) {
        if(hasEnvironment(doc, name)) {
            if (overwrite) {
                removeEnvironment(doc, name);
            } else {
                return doc;
            }
        }
        Element context = (Element) doc.selectSingleNode("//Context");
        Dom4JUtils.addIndentedElement(context, "Environment", null)
                .addAttribute("name", name)
                .addAttribute("value", value)
                .addAttribute("type", type);
        return doc;
    }

    public static void addEnvironment(File contextXML, String name, String value, String type, boolean overwrite) {
        Document doc;
        try {
            doc = new SAXReader().read(contextXML);
            addEnvironment(doc, name, value, type, overwrite);
            writeResource(doc, contextXML);
        } catch (DocumentException | IOException e) {
            log.error("Error adding resource to {}", contextXML.getAbsolutePath(), e);
        }
    }

    static void writeResource(Document doc, File target) throws IOException {
        FileWriter writer = new FileWriter(target);
        doc.write(writer);
        writer.close();
    }
}
