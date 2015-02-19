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
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.NodeIterable;

public class HippoDocumentReprentation {

    private String id;
    private String nodeName;
    private String displayName;
    private String nodePath;
    private boolean selectable;
    private boolean folder;
    private List<HippoDocumentReprentation> folders = new ArrayList<>();
    private List<HippoDocumentReprentation> documents = new ArrayList<>();


    public HippoDocumentReprentation() {
        super();
    }

    public HippoDocumentReprentation(final Node hippoDocumentNode) throws RepositoryException {
        this(hippoDocumentNode, true, true);
    }

    private HippoDocumentReprentation(final Node node, final boolean traverseChildren, final boolean folder) throws RepositoryException {
        if (!(node instanceof HippoNode)) {
            throw new ClientException("Expected object of class HippoNode but was of class " + node.getClass().getName(),
                    ClientError.UNKNOWN);
        }
        id = node.getIdentifier();
        nodeName = node.getName();
        displayName = ((HippoNode)node).getLocalizedName();
        nodePath = node.getPath();
        this.folder = folder;
        // to be done: Only if a link can be created for the document
        selectable = true;
        if (traverseChildren) {
            for (Node child : new NodeIterable(node.getNodes())) {
                if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    addFolder(child);
                }
                if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                    addDocument(child);
                }
                // else ignore
            }
        }
    }

    private void addFolder(final Node child) throws RepositoryException {
        HippoDocumentReprentation folder = new HippoDocumentReprentation(child, false, true);
        folders.add(folder);
    }

    private void addDocument(final Node child) throws RepositoryException {
        HippoDocumentReprentation document = new HippoDocumentReprentation(child, false, false);
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

    public List<HippoDocumentReprentation> getFolders() {
        return folders;
    }

    public void setFolders(final List<HippoDocumentReprentation> folders) {
        this.folders = folders;
    }

    public List<HippoDocumentReprentation> getDocuments() {
        return documents;
    }

    public void setDocuments(final List<HippoDocumentReprentation> documents) {
        this.documents = documents;
    }
}
