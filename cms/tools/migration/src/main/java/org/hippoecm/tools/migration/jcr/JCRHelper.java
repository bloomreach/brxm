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
package org.hippoecm.tools.migration.jcr;

import java.io.InputStream;
import java.util.Calendar;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoSession;

/**
 * JCR Helper methods for creating content in a JCR Repository
 */
public class JCRHelper {

    // nodetypes
    private static final String HANDLE_NODETYPE = "hippo:handle";
    private static final String SELECTOR_NODETYPE = "hippo:facetselect";
    private static final String DOCUMENT_NODETYPE = "hippo:document";
    private static final String AUTHOR_NODETYPE = "hipposample:author";

    // properties
    private static final String STATE_PROPERTY = "state";
    private static final String CONTENT_PROPERTY = "content";
    private static final String DOCBASE_PROPERTY = "hippo:docbase";
    private static final String FACETS_PROPERTY = "hippo:facets";
    private static final String VALUES_PROPERTY = "hippo:values";
    private static final String MODES_PROPERTY = "hippo:modes";


    /**
     * Check the JCR import root path and create the path if it doesn't exist
     * @throws RepositoryException
     */
    public static Node checkAndCreatePath(Session session, String path) throws RepositoryException {

        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }

        if ("".equals(path)) {
            throw new IllegalArgumentException("Path cannot be / or empty");
        }


        javax.jcr.Node current = (javax.jcr.Node) session.getRootNode();

        try {
            return current.getNode(path);
        } catch(PathNotFoundException e) {
            // ok, we should create it
        }

        // TODO: can be optimized
        String currentPath = "";
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {

            String nodeName = st.nextToken();

            if (nodeName == null || "".equals(nodeName)) {
                continue;
            }
            System.out.println("Checking for: " + currentPath + "/" + nodeName);

            // add node if it doesn't exist
            if (!current.hasNode(nodeName)) {
                current.addNode(nodeName);
                System.out.println("Added node for jcrPath: " + currentPath);
            }
            currentPath += "/" + nodeName;

            // shift to child node
            current = current.getNode(nodeName);
        }
        return current;
    }

    /**
     * Create an standard author with hippo:handle
     * @param session
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    public static Node createDefaultAuthor(Session session, Node parent, String name) throws RepositoryException {
        Node handle = JCRHelper.createHandle(session, parent, name);
        // Overwrite existing nodes
        if (handle.hasNode(name)) {
            handle.getNode(name).remove();
        }
        Node node = handle.addNode(name, AUTHOR_NODETYPE);
        return node;
    }

    /**
     * Create statndard document with a hippo:handle
     * @param session
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    public static Node createDefaultDocument(Session session, Node parent, String name) throws RepositoryException {
        Node handle = JCRHelper.createHandle(session, parent, name);
        // Overwrite existing nodes
        if (handle.hasNode(name)) {
            handle.getNode(name).remove();
        }
        Node doc = handle.addNode(name, DOCUMENT_NODETYPE);
        // create default props
        doc.setProperty(STATE_PROPERTY, "unpublished");
        return doc;
    }

    /**
     * create a hippo:handle, return the current handle if the node already exists
     * @param session
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    public static Node createHandle(Session session, Node parent, String name) throws RepositoryException {
        if (parent == null) {
            throw new RepositoryException("Parent node null while creating handle!");
        }
        if (parent.hasNode(name)) {
            if (parent.getNode(name).getPrimaryNodeType().getName().equals(HANDLE_NODETYPE) ) {
                return parent.getNode(name);
            } else {
                parent.getNode(name).remove();
            }
        }
        return parent.addNode(name, HANDLE_NODETYPE);
    }

    /**
     * Set the content property (of a document)
     * @param session
     * @param doc
     * @param content
     * @throws RepositoryException
     */
    public static void setDocumentContent(Session session, Node doc, InputStream content) throws RepositoryException {
        doc.setProperty(CONTENT_PROPERTY, session.getValueFactory().createValue(content));
    }

    /**
     * Add an author to a document, create "authors" unstructured node if it't not yet created
     * @param session
     * @param authorBase
     * @param doc
     * @param author
     * @throws RepositoryException
     */
    public static void addAuhtorToDocument(Session session, Node authorBase, Node doc, String author) throws RepositoryException {        // TODO: authors is hardcoded
        if (!doc.hasNode("authors")) {
            doc.addNode("authors");
        }
        Node authorsNode = doc.getNode("authors");

        // author already set for this doc
        if (authorsNode.hasNode(author)) {
            return;
        }

        // Make sure basepath starts with a slash
        Node selector = authorsNode.addNode(author, SELECTOR_NODETYPE);
        // map directly on author handle
        selector.setProperty(DOCBASE_PROPERTY, authorBase.getPath() + '/' + author);
        selector.setProperty(MODES_PROPERTY, "select");
        selector.setProperty(FACETS_PROPERTY, "name");
        selector.setProperty(VALUES_PROPERTY, author);
    }

    /**
     * Create a published version of the document
     * @param session
     * @param doc
     * @param publicationDate
     * @return
     * @throws RepositoryException
     */
    public static Node createPublishDocument(Session session, Node doc, Calendar publicationDate) throws RepositoryException {
        Node published = ((HippoSession)session).copy(doc, doc.getPath());
        // states [published, unpublished, stale, draft]
        published.setProperty(STATE_PROPERTY, "published");
        return published;
    }
}
