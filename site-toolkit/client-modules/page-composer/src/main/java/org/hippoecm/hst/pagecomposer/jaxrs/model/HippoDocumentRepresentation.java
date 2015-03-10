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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.jackrabbit.commons.JcrUtils;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoDocumentRepresentation {

    private static final Logger log = LoggerFactory.getLogger(HippoDocumentRepresentation.class);

    private String id;
    private String nodeName;
    private String displayName;
    private String nodePath;
    private String pathInfo;
    private boolean selectable;
    private boolean selected;
    private boolean folder;
    private boolean hasFolders;
    private boolean hasDocuments;
    private List<HippoDocumentRepresentation> items = new ArrayList<>();

    @JsonIgnore
    transient private Map<String, HippoDocumentRepresentation> childMap = new HashMap<>();


    public HippoDocumentRepresentation() {
        super();
    }

    public HippoDocumentRepresentation(final PageComposerContextService pageComposerContextService) throws RepositoryException {
        represent(pageComposerContextService, pageComposerContextService.getRequestConfigNode(HippoNodeType.NT_DOCUMENT), true);
    }


    public HippoDocumentRepresentation represent(final PageComposerContextService pageComposerContextService, final String siteMapPathInfo) throws RepositoryException {

        HttpSession session = pageComposerContextService.getRequestContext().getServletRequest().getSession(false);
        try {
            if (session != null) {
                String renderingHost = (String) session.getAttribute(ContainerConstants.RENDERING_HOST);
                final VirtualHost virtualHost = pageComposerContextService.getRequestContext().getResolvedMount().getMount().getVirtualHost();
                final ResolvedMount resolvedMount = virtualHost.getVirtualHosts().matchMount(renderingHost, null, siteMapPathInfo);
                final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem(siteMapPathInfo);

                String selectedNodePath = pageComposerContextService.getEditingMount().getContentPath() + "/" + resolvedSiteMapItem.getRelativeContentPath();
                final Node selectedNode = pageComposerContextService.getRequestContext().getSession().getNode(selectedNodePath);
                final Node rootContentNode = pageComposerContextService.getRequestConfigNode(HippoNodeType.NT_DOCUMENT);

                if (!selectedNode.getPath().startsWith(rootContentNode.getPath()) && !selectedNode.getPath().equals(rootContentNode.getPath())) {
                    throw new IllegalStateException(String.format("Expected selected node '%s' to be a equal to or a descendant of '%s'.",
                            selectedNode.getPath(), rootContentNode.getPath()));
                }

                List<Node> representNodes = new ArrayList<>();
                Node currentNode = selectedNode;
                representNodes.add(currentNode);
                while (!currentNode.isSame(rootContentNode)) {
                    currentNode = currentNode.getParent();
                    representNodes.add(0, currentNode);
                }

                HippoDocumentRepresentation parent = null;
                HippoDocumentRepresentation root = null;
                for (Node representNode : representNodes) {
                    boolean loadChildren = !representNode.isNodeType(HippoNodeType.NT_HANDLE);
                    HippoDocumentRepresentation presentation = new HippoDocumentRepresentation().represent(pageComposerContextService, representNode, loadChildren);
                    if (parent == null) {
                        root = presentation;
                        if (representNode.isSame(selectedNode)) {
                            root.selected = true;
                        }
                    } else {
                        final HippoDocumentRepresentation unPopulatedChildPresentation = parent.childMap.get(representNode.getPath());
                        // replace this representation with the populated presentation we have here
                        unPopulatedChildPresentation.items = presentation.items;
                        if (representNode.isSame(selectedNode)) {
                            unPopulatedChildPresentation.selected = true;
                        }
                    }
                    parent = presentation;
                }
                return root;
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Exception trying to return document representation for pathInfo '%s'. Return root " +
                        "content folder representation instead", siteMapPathInfo);
                log.info(msg, e);
            } else {
                log.info("Exception trying to return document representation for pathInfo '{}' : {}. Return root " +
                        "content folder representation instead", siteMapPathInfo, e.toString());
            }
        }
        return represent(pageComposerContextService, pageComposerContextService.getRequestConfigNode(HippoNodeType.NT_DOCUMENT), true);
    }


    private HippoDocumentRepresentation represent(final PageComposerContextService pageComposerContextService,
                           final Node node,
                           final boolean includeChildren) throws RepositoryException {
        if (!(node instanceof HippoNode)) {
            throw new ClientException("Expected object of class HippoNode but was of class " + node.getClass().getName(),
                    ClientError.UNKNOWN);
        }
        id = node.getIdentifier();
        nodeName = node.getName();
        displayName = ((HippoNode)node).getLocalizedName();
        nodePath = node.getPath();
        this.folder = !node.isNodeType(HippoNodeType.NT_HANDLE);

        final HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();

        final HstLink hstLink = linkCreator.create(node, pageComposerContextService.getEditingMount());
        if (hstLink.isNotFound()) {
            selectable = false;
        } else {
            selectable = true;
            pathInfo = hstLink.getPath();
        }

        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                hasFolders = true;
                if (includeChildren) {
                    addFolder(pageComposerContextService, child);
                }
            }
            if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                hasDocuments = true;
                if (includeChildren) {
                    addDocument(pageComposerContextService, child);
                }
            }
            // else ignore
        }
        return this;
    }

    private void addFolder(final PageComposerContextService pageComposerContextService, final Node child) throws RepositoryException {
        HippoDocumentRepresentation folder =  new HippoDocumentRepresentation().represent(pageComposerContextService, child, false);
        items.add(folder);
        childMap.put(child.getPath(), folder);
    }

    private void addDocument(final PageComposerContextService pageComposerContextService, final Node child) throws RepositoryException {
        HippoDocumentRepresentation document =  new HippoDocumentRepresentation().represent(pageComposerContextService, child, false);
        items.add(document);
        childMap.put(child.getPath(), document);
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

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
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

    public List<HippoDocumentRepresentation> getItems() {
        return items;
    }

    public void setItems(final List<HippoDocumentRepresentation> items) {
        this.items = items;
    }
}
