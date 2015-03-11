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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreePickerRepresentation {

    private static final Logger log = LoggerFactory.getLogger(TreePickerRepresentation.class);

    private String id;
    private String nodeName;
    private String displayName;
    private String nodePath;
    private String pathInfo;
    private boolean selectable;
    private boolean selected;
    private boolean folder;
    private boolean containsFolders;
    private boolean containsDocuments;
    private List<TreePickerRepresentation> items = new ArrayList<>();

    @JsonIgnore
    transient private Map<String, TreePickerRepresentation> childMap = new HashMap<>();


    public TreePickerRepresentation() {
        super();
    }

    public TreePickerRepresentation(final PageComposerContextService pageComposerContextService) throws RepositoryException {
        represent(pageComposerContextService, pageComposerContextService.getRequestConfigNode(HippoNodeType.NT_DOCUMENT), true);
    }

    public TreePickerRepresentation representExpandedParentTree(final PageComposerContextService pageComposerContextService,
                                                                   final String siteMapPathInfo) throws RepositoryException {

        HttpSession session = pageComposerContextService.getRequestContext().getServletRequest().getSession(false);
        try {
            if (session != null) {
                String renderingHost = (String) session.getAttribute(ContainerConstants.RENDERING_HOST);
                final VirtualHost virtualHost = pageComposerContextService.getRequestContext().getResolvedMount().getMount().getVirtualHost();
                final ResolvedMount resolvedMount = virtualHost.getVirtualHosts().matchMount(renderingHost, null, siteMapPathInfo);

                final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem(siteMapPathInfo);

                if (resolvedSiteMapItem.getPathInfo().equals(resolvedMount.getMount().getPageNotFound())) {
                    // siteMapPathInfo item is resolved to the page not found item. this is an invalid item in the sitemap
                    // item tree
                    final String msg = String.format("For 'siteMapPathInfo %s' the resolved sitemap item '%s' is the page not " +
                                    "found sitemap item for which no tree picker representation can be created.",
                            siteMapPathInfo, resolvedSiteMapItem.getHstSiteMapItem());
                    throw new IllegalStateException(msg);
                }

                if (StringUtils.isEmpty(resolvedSiteMapItem.getRelativeContentPath())) {
                    // if explicit sitemap item, return sitemap item representation
                    // if sitemap item contains wildcards, the siteMapPathInfo is invalid as it cannot be represented in the
                    // document OR sitemap tree
                    if (resolvedSiteMapItem.getHstSiteMapItem().isExplicitElement()) {
                        return representExpandedParentTree(pageComposerContextService, resolvedSiteMapItem.getHstSiteMapItem());
                    }
                    final String msg = String.format("For 'siteMapPathInfo %s' the resolved sitemap item '%s' does not have a relative content path and " +
                            "is not an explicit sitemap item hence no tree picker representation can be created for it be " +
                            "created for it.", siteMapPathInfo, resolvedSiteMapItem.getHstSiteMapItem());
                    throw new IllegalStateException(msg);
                }

                final Node rootContentNode = pageComposerContextService.getRequestContext().getSession().getNode(pageComposerContextService.getEditingMount().getContentPath());
                final Node selectedNode = rootContentNode.getNode(resolvedSiteMapItem.getRelativeContentPath());

                List<Node> representNodes = new ArrayList<>();
                Node currentNode = selectedNode;
                representNodes.add(currentNode);
                while (!currentNode.isSame(rootContentNode)) {
                    currentNode = currentNode.getParent();
                    representNodes.add(0, currentNode);
                }

                TreePickerRepresentation parent = null;
                TreePickerRepresentation root = null;
                for (Node representNode : representNodes) {
                    boolean loadChildren = !representNode.isNodeType(HippoNodeType.NT_HANDLE);
                    TreePickerRepresentation presentation = new TreePickerRepresentation().represent(pageComposerContextService, representNode, loadChildren);
                    if (parent == null) {
                        root = presentation;
                        if (representNode.isSame(selectedNode)) {
                            root.selected = true;
                        }
                    } else {
                        final TreePickerRepresentation unPopulatedChildPresentation = parent.childMap.get(representNode.getPath());
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
        } catch (PathNotFoundException | MatchException | IllegalStateException e) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Exception trying to return document representation for siteMapPathInfo '%s'. Return root " +
                        "content folder representation instead", siteMapPathInfo);
                log.info(msg, e);
            } else {
                log.info("Exception trying to return document representation for siteMapPathInfo '{}' : {}. Return root " +
                        "content folder representation instead", siteMapPathInfo, e.toString());
            }
        }
          catch (Exception e) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Exception trying to return document representation for siteMapPathInfo '%s'. Return root " +
                        "content folder representation instead", siteMapPathInfo);
                log.warn(msg, e);
            } else {
                log.warn("Exception trying to return document representation for siteMapPathInfo '{}' : {}. Return root " +
                        "content folder representation instead", siteMapPathInfo, e.toString());
            }
        }
        return represent(pageComposerContextService, pageComposerContextService.getRequestConfigNode(HippoNodeType.NT_DOCUMENT), true);
    }

    private TreePickerRepresentation representExpandedParentTree(final PageComposerContextService pageComposerContextService,
                                                                 final HstSiteMapItem hstSiteMapItem) {
        // TODO HSTTWO-3225
        return new TreePickerRepresentation();
    }


    private TreePickerRepresentation represent(final PageComposerContextService pageComposerContextService,
                                                  final Node node,
                                                  final boolean includeChildren) throws RepositoryException {
        if (!(node instanceof HippoNode)) {
            throw new ClientException("Expected object of class HippoNode but was of class " + node.getClass().getName(),
                    ClientError.UNKNOWN);
        }
        id = node.getIdentifier();
        nodeName = node.getName();
        displayName = ((HippoNode) node).getLocalizedName();
        nodePath = node.getPath();
        this.folder = !node.isNodeType(HippoNodeType.NT_HANDLE);

        final HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();

        final Mount editingMount = pageComposerContextService.getEditingMount();
        final HstLink hstLink = linkCreator.create(node, editingMount);
        if (hstLink.isNotFound() || node.isSame(node.getSession().getNode(editingMount.getContentPath()))) {
            selectable = false;
        } else {
            selectable = true;
            pathInfo = hstLink.getPath();
            if (StringUtils.isEmpty(pathInfo)) {
                // homepage. However we need the sitemap reference path to the homepage sitemap item
                pathInfo = HstSiteMapUtils.getPath(editingMount, editingMount.getHomePage());
            }
        }

        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                containsFolders = true;
                if (includeChildren) {
                    addFolder(pageComposerContextService, child);
                }
            }
            if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                containsDocuments = true;
                if (includeChildren) {
                    addDocument(pageComposerContextService, child);
                }
            }
            // else ignore
        }

        final boolean jcrOrder = node.getPrimaryNodeType().hasOrderableChildNodes();
        if (!jcrOrder && isFolder()) {
            // order alphabetically, first folders then documents
            Collections.sort(items, new HippoDocumentRepresentationComparator());
        }

        return this;
    }

    private void addFolder(final PageComposerContextService pageComposerContextService, final Node child) throws RepositoryException {
        TreePickerRepresentation folder = new TreePickerRepresentation().represent(pageComposerContextService, child, false);
        items.add(folder);
        childMap.put(child.getPath(), folder);
    }

    private void addDocument(final PageComposerContextService pageComposerContextService, final Node child) throws RepositoryException {
        TreePickerRepresentation document = new TreePickerRepresentation().represent(pageComposerContextService, child, false);
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

    public boolean isContainsFolders() {
        return containsFolders;
    }

    public void setContainsFolders(final boolean containsFolders) {
        this.containsFolders = containsFolders;
    }

    public boolean isContainsDocuments() {
        return containsDocuments;
    }

    public void setContainsDocuments(final boolean containsDocuments) {
        this.containsDocuments = containsDocuments;
    }

    public List<TreePickerRepresentation> getItems() {
        return items;
    }

    public void setItems(final List<TreePickerRepresentation> items) {
        this.items = items;
    }

    class HippoDocumentRepresentationComparator implements Comparator<TreePickerRepresentation> {
        @Override
        public int compare(final TreePickerRepresentation o1, final TreePickerRepresentation o2) {
            if (o1.isFolder()) {
                if (!o2.isFolder()) {
                    // folders are ordered first
                    return -1;
                }
            }
            if (o2.isFolder()) {
                if (!o1.isFolder()) {
                    // folders are ordered first
                    return 1;
                }
            }
            // both are a folder or both are a document. Return lexical sorting on displayname
            return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
        }
    }
}
