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
package org.hippoecm.tools.importer;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.tools.importer.api.Content;
import org.hippoecm.tools.importer.api.Context;
import org.hippoecm.tools.importer.api.Mapping;
import org.hippoecm.tools.importer.api.PathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default importer implementation.
 */
public class ContextImpl implements Context {

    final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(ContextImpl.class);

    private boolean overwrite = true;
    private Mapping mapping;
    private String defaultFolderType = "hippostd:folder";
    private Node root;
    private Node parent;

    public ContextImpl(Node rootNode, Mapping mapping, boolean overwrite) throws RepositoryException {
        this.overwrite = overwrite;
        this.mapping = mapping;
        this.root = rootNode;
        if (mapping != null) {
            String path = mapping.getPath();
            String parentPath = PathHelper.getParent(path);
            if (parentPath != null && !"".equals(parentPath)) {
                if (parentPath.startsWith("/")) {
                    parent = root.getNode(PathHelper.encodePath(parentPath.substring(1)));
                } else {
                    parent = root.getNode(PathHelper.encodePath(parentPath));
                }
            } else {
                parent = root;
            }
        }
    }

    public Node createDocument(Content source) throws RepositoryException, IOException {
        String name = source.getName();
        if (mapping != null) {
            String path = mapping.getPath();
            if (path.lastIndexOf('/') >= 0) {
                name = path.substring(path.lastIndexOf('/') + 1);
            } else {
                name = path;
            }
        }

        // build document
        return createDocumentNode(parent, name, mapping.getNodeType());
    }

    /**
     * Create a folder
     * @param parent
     * @param name the name will be encode
     * @return the folder node
     * @throws RepositoryException
     */
    public Node createFolder(Content source) throws RepositoryException {
        String name = source.getName();
        return createFolderNode(parent, name, mapping.getNodeType());
    }

    /*
     * {@inheritDoc}
     */
    public Node createPath(String path) throws RepositoryException {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if ("".equals(path) || "/".equals(path)) {
            return root;
        } else {
            String[] elements = path.split("\\/");
            Node parent = root;
            for (int i = 0; i < elements.length; i++) {
                if (parent.hasNode(NodeNameCodec.encode(elements[i]))) {
                    parent = parent.getNode(NodeNameCodec.encode(elements[i]));
                } else {
                    parent = createFolderNode(parent, elements[i], defaultFolderType);
                }
            }
            return parent;
        }
    }

    /**
     * Create a document with handle
     * @param parent
     * @param name the name will be encode
     * @param type
     * @return the handle
     * @throws RepositoryException
     */
    protected Node createDocumentNode(Node parent, String name, String type) throws RepositoryException {
        String encoded = NodeNameCodec.encode(name);

        if (parent.hasNode(encoded)) {
            if (overwrite) {
                log.info("Removing document: " + buildPath(encoded));
                parent.getNode(encoded).remove();
            } else {
                log.info("Skipping document: " + buildPath(encoded));
                return parent.getNode(encoded);
            }
        }
        log.info("Creating document: " + buildPath(encoded));

        // add handle
        Node handle = parent.addNode(encoded, "hippo:handle");
        handle.addMixin("hippo:hardhandle");

        // add document node
        Node document = handle.addNode(encoded, type);
        document.addMixin("hippo:harddocument");

        return document;
    }

    protected Node createFolderNode(Node parent, String name, String type) throws RepositoryException {
        String encoded = NodeNameCodec.encode(name);
        if (parent.hasNode(encoded)) {
            return parent.getNode(encoded);
        }
        log.info("Creating folder: " + buildPath(encoded));
        Node folder = parent.addNode(encoded, type);
        folder.addMixin("hippo:harddocument");
        return folder;
    }

    protected void setDefaultFolderType(String type) {
        this.defaultFolderType = type;
    }

    /**
     * Helper method for creating absolute paths, used for logging.
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    public String buildPath(String name) throws RepositoryException {
        StringBuffer buf = new StringBuffer();
        if (parent != null && parent.getDepth() > 0) {
            buf.append(parent.getPath());
        }
        buf.append('/').append(name);
        return buf.toString();
    }

}
