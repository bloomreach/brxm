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
package org.hippoecm.tools.migration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import nl.hippo.webdav.batchprocessor.OperationOnDeletedNodeException;
import nl.hippo.webdav.batchprocessor.ProcessingException;

import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.PropertyName;
import org.hippoecm.tools.migration.jcr.JCRHelper;
import org.hippoecm.tools.migration.webdav.WebdavHelper;

/**
 * The SimpleDocumentConverter converts the all the webdav nodes to a similar
 * structor in the JCR repository. It also creates the extractors and the
 * published version of the documents.
 */
public class SimpleDocumentConverter extends AbstractDocumentConverter implements DocumentConverter {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Author hashmap to speedup import
     */
    protected HashMap authorMap = new HashMap();

    /**
     * Author basenode
     */
    protected Node authorBase;

    /**
     * Post setup hook (before processing loop)
     * @throws RepositoryException
     */
    public void postSetupHook() throws RepositoryException {
        authorBase = JCRHelper.checkAndCreatePath(getJcrSession(), AUTHOR_BASEPATH, this);
    }

    /**
     * The main converter loop
     */
    public void convertNodeToJCR(nl.hippo.webdav.batchprocessor.Node webdavNode, String nodeName, javax.jcr.Node parent)
            throws RepositoryException, ProcessingException, OperationOnDeletedNodeException, IOException {

        // create a basic document
        javax.jcr.Node current = JCRHelper.createDefaultDocument(getJcrSession(), parent, nodeName);

        // Set the content
        int contentLength = 0;
        String contentLengthAsString = webdavNode.getProperty(DAV_NAMESPACE, "getcontentlength").getPropertyAsString();
        try {
            contentLength = Integer.parseInt(contentLengthAsString);
        } catch (NumberFormatException e) {
           // ignore, no content
        }
        if (contentLength > 0) {
            JCRHelper.setDocumentContent(getJcrSession(), current, new ByteArrayInputStream(webdavNode.getContents()));
        }

        // publication holders
        boolean isPublished = false;
        Calendar publicationDate = null;

        // author holders
        boolean hasAuthor = false;
        String author = null;

        // Add metadata properties
        Iterator webdavPropertyNames = webdavNode.propertyNamesIterator();
        while (webdavPropertyNames.hasNext()) {
            PropertyName webdavPropertyName = (PropertyName) webdavPropertyNames.next();
            String webdavPropertyNamespace = webdavPropertyName.getNamespaceURI();

            // only copy properties in the hippo namespace
            if (webdavPropertyNamespace.equals(HIPPO_NAMESPACE)) {
                String name = webdavPropertyName.getLocalName();
                Property webdavProperty = webdavNode.getProperty(webdavPropertyNamespace, name);

                if (name.equals("publicationDate")) {
                    isPublished = true;
                    publicationDate = WebdavHelper.getCalendarFromProperty(webdavProperty, PUBLICATIONDATE_FORMAT);
                } else if(name.equals("createdBy") || name.equals("lastWorkflowUser")) {
                    hasAuthor = true;
                    author = webdavProperty.getPropertyAsString();
                } else {
                    Value value = getJcrSession().getValueFactory().createValue(webdavProperty.getPropertyAsString());
                    // don't set empty properties
                    if (!"".equals(value.getString())) {
                        current.setProperty(name, value);
                    }
                }
            }
        }

        if (hasAuthor && author != null) {
            if (!authorMap.containsKey(author)) {
                JCRHelper.createDefaultAuthor(getJcrSession(), authorBase, author);
                authorMap.put(author, true);
            }
            JCRHelper.addAuhtorToDocument(getJcrSession(), authorBase, current, author);
        }
        if (isPublished) {
            //JCRHelper.createPublishDocument(getJcrSession(), current, publicationDate);
        }

    }

    public void setMixinsPlusProps(Node node) throws  RepositoryException {
        node.addMixin("mix:referenceable");
    }


}
