/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.tools.importer.conversion;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.tools.importer.api.Content;
import org.hippoecm.tools.importer.api.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * All documents are set to type "defaultcontent:article".
 */
public class DefaultcontentConverter extends AbstractConverter {

    private static Logger log = LoggerFactory.getLogger(DefaultcontentConverter.class);

    final static String SVN_ID = "$Id$";

    @Override
    public Node convert(Context context, Content content) throws IOException, RepositoryException {
        String name = content.getName();

        String title = null;
        String introduction = null;

        Document xmlDoc = parse(content);
        if (xmlDoc == null) {
            log.warn("Source not parsed, skipping : " + context.buildPath(name));
            return null;
        }

        NodeList nodes;

        nodes = xmlDoc.getElementsByTagName("title");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element titleElem = (Element) nodes.item(i);
            org.w3c.dom.Node childNode = titleElem.getFirstChild();
            if (childNode instanceof Text) {
                title = childNode.getNodeValue();
            }
        }

        nodes = xmlDoc.getElementsByTagName("summary");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element titleElem = (Element) nodes.item(i);
            org.w3c.dom.Node childNode = titleElem.getFirstChild();
            if (childNode instanceof Text) {
                introduction = childNode.getNodeValue();
            }
        }

        Node doc = context.createDocument(content);
        if (doc != null) {
            doc.setProperty("hippostd:state", "unpublished");
            if (title == null) {
                log.warn("Title not found, skipping : " + context.buildPath(name));
                return doc;
            } else {
                doc.setProperty("defaultcontent:title", title);
            }
            if (introduction == null) {
                log.warn("Introduction not found, skipping : " + context.buildPath(name));
                return doc;
            } else {
                doc.setProperty("defaultcontent:introduction", introduction);
            }
        }
        return doc;
    }

    public String[] getNodeTypes() {
        return new String[] { "defaultcontent:article" };
    }

}
