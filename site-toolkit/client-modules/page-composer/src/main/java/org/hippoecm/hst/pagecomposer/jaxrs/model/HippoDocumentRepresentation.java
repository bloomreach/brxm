/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.NodeIterable;

public class HippoDocumentRepresentation {

    private String id;
    private String nodeName;
    private String displayName;
    private String nodePath;
    private boolean selectable;
    private boolean folder;
    private boolean hasFolders;
    private boolean hasDocuments;
    private List<HippoDocumentRepresentation> folders = new ArrayList<>();
    private List<HippoDocumentRepresentation> documents = new ArrayList<>();


    public HippoDocumentRepresentation() {
        super();
    }

    public HippoDocumentRepresentation(final Node hippoDocumentNode) throws RepositoryException {
        this(hippoDocumentNode, true, true);
    }

    private HippoDocumentRepresentation(final Node node, final boolean includeChildren, final boolean folder) throws RepositoryException {
        if (!(node instanceof HippoNode)) {
            throw new ClientException("Expected object of class HippoNode but was of class " + node.getClass().getName(),
                    ClientError.UNKNOWN);
        }
        id = node.getIdentifier();
        nodeName = node.getName();
        displayName = ((HippoNode)node).getLocalizedName();
        nodePath = node.getPath();
        this.folder = folder;

        // to be done: selectable Only if a link can be created for the document
        selectable = true;

        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                hasFolders = true;
                if (includeChildren) {
                    addFolder(child);
                }
            }
            if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                hasDocuments = true;
                if (includeChildren) {
                    addDocument(child);
                }
            }
            // else ignore
        }

    }

    private void addFolder(final Node child) throws RepositoryException {
        HippoDocumentRepresentation folder =  new HippoDocumentRepresentation(child, false, true);
        folders.add(folder);
    }

    private void addDocument(final Node child) throws RepositoryException {
        HippoDocumentRepresentation document =  new HippoDocumentRepresentation(child, false, false);
        documents.add(document);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(final String nodePath) {
        this.nodePath = nodePath;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(final boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(final boolean folder) {
        this.folder = folder;
    }

    public boolean isHasFolders() {
        return hasFolders;
    }

    public void setHasFolders(final boolean hasFolders) {
        this.hasFolders = hasFolders;
    }

    public boolean isHasDocuments() {
        return hasDocuments;
    }

    public void setHasDocuments(final boolean hasDocuments) {
        this.hasDocuments = hasDocuments;
    }

    public List<HippoDocumentRepresentation> getFolders() {
        return folders;
    }

    public void setFolders(final List<HippoDocumentRepresentation> folders) {
        this.folders = folders;
    }

    public List<HippoDocumentRepresentation> getDocuments() {
        return documents;
    }

    public void setDocuments(final List<HippoDocumentRepresentation> documents) {
        this.documents = documents;
    }
}
