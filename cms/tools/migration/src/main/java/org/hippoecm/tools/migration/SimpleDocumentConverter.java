/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.migration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import nl.hippo.webdav.batchprocessor.OperationOnDeletedNodeException;
import nl.hippo.webdav.batchprocessor.ProcessingException;

import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;

/**
 * The SimpleDocumentConverter convert the all the webdav nodes to an identical
 * structor in the JCR repository
 * 
 */
public class SimpleDocumentConverter extends AbstractDocumentConverter implements DocumentConverter {
    
    /**
     * Setup hook
     */
    public void postSetupHook() {
    }
    
    /**
     * The main converter loop
     */
    public void convertNodeToJCR(nl.hippo.webdav.batchprocessor.Node webdavNode, String nodeName, javax.jcr.Node parent)
            throws RepositoryException, ProcessingException, OperationOnDeletedNodeException, IOException {

        // Overwrite existing nodes
        if (parent.hasNode(nodeName)) {
            parent.getNode(nodeName).remove();
        }

        // Create the new JCR node
        javax.jcr.Node current = parent.addNode(nodeName);

        //current.addMixin("mix:referenceable");
        current.setProperty("published", false);

        // Set the content
        String contentLengthAsString = webdavNode.getProperty(DAV_NAMESPACE, "getcontentlength").getPropertyAsString();
        int contentLength = Integer.parseInt(contentLengthAsString);
        if (contentLength > 0) {
            byte[] content = webdavNode.getContents();
            current.setProperty("content", jcrSession.getValueFactory().createValue(new ByteArrayInputStream(content)));
        }

        // Add metadata properties
        Iterator webdavPropertyNames = webdavNode.propertyNamesIterator();
        while (webdavPropertyNames.hasNext()) {
            PropertyName webdavPropertyName = (PropertyName) webdavPropertyNames.next();
            String webdavPropertyNamespace = webdavPropertyName.getNamespaceURI();
            if (webdavPropertyNamespace.equals(HIPPO_NAMESPACE)) {
                String name = webdavPropertyName.getLocalName();
                Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);

                if (name.equals("publicationDate")) {
                    current.setProperty("published", true);
                    current.setProperty("publicationdate", getCalendarFromProperty(webdavProperty,
                            PUBLICATIONDATE_FORMAT));

                } else {
                    Value value = jcrSession.getValueFactory().createValue(webdavProperty.getPropertyAsString());
                    current.setProperty(name, value);
                }
            } else {
                /* //Example to split out dates to day, month, year.
                 String name = webdavPropertyName.getLocalName();
                 if (name.equals("creationdate")) {
                 Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);
                 Date d = new Date();
                 try
                 {
                 d = CREATIONDATE_FORMAT.parse(webdavProperty.getPropertyAsString());
                 Calendar c = Calendar.getInstance();
                 c.setTime(d);
                 current.setProperty("year", c.get(Calendar.YEAR));
                 current.setProperty("month", 1 + c.get(Calendar.MONTH));
                 current.setProperty("day", c.get(Calendar.DAY_OF_MONTH));
                 } catch (java.text.ParseException e) {
                 }
                 
                 }
                 */

            }
        }

    }


}
