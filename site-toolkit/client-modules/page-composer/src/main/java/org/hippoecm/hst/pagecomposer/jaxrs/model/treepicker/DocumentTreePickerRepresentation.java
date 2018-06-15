/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.platform.configuration.sitemap.HstSiteMapItemService;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation.ExpandedNodeHierarchy.createExpandedNodeHierarchy;
import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.hippoecm.repository.HippoStdNodeType.NT_PUBLISHABLESUMMARY;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public class DocumentTreePickerRepresentation extends AbstractTreePickerRepresentation {

    private static final Logger log = LoggerFactory.getLogger(DocumentTreePickerRepresentation.class);

    public static AbstractTreePickerRepresentation representRequestContentNode(final PageComposerContextService pageComposerContextService) throws RepositoryException {
        final ExpandedNodeHierarchy singleNodeHierarchy = ExpandedNodeHierarchy.createSingleNodeHierarchy(pageComposerContextService.getRequestConfigNode(NT_DOCUMENT));
        return new DocumentTreePickerRepresentation().represent(pageComposerContextService, singleNodeHierarchy, true, null);
    }

    /**
     * Returns the expanded parent tree representation for this {@link DocumentTreePickerRepresentation}
     * instance. An exceptional use case is that the <code>siteMapPathInfo</code> points to a sitemap item that does
     * not
     * have a relative content path at all. In that case, instead of the current {@link
     * DocumentTreePickerRepresentation}
     * instance is returned, a <strong>new</strong> instance {@link SiteMapTreePickerRepresentation} is returned!
     */
    public static AbstractTreePickerRepresentation representExpandedParentTree(final PageComposerContextService pageComposerContextService,
                                                                               final ResolvedMount resolvedMount,
                                                                               final String siteMapItemRefIdOrPath) throws RepositoryException {


        try {
            final Session jcrSession = pageComposerContextService.getRequestContext().getSession();
            if (!jcrSession.getNode(resolvedMount.getMount().getContentPath()).getIdentifier()
                    .equals(pageComposerContextService.getRequestConfigIdentifier())) {
                final String msg = String.format("Representing an expanded parent tree through './%s' is only supported with request " +
                        "identifier equal to the channel root content identifier", siteMapItemRefIdOrPath);
                throw new ClientException(msg, ClientError.INVALID_UUID);
            }

            final String siteMapPathInfo;
            // siteMapPathInfo can be by siteMapItemPath but can also be by refId. Hence, we first check whether we are
            // dealing with a siteMpaItem path of refId
            final HstSiteMapItem siteMapItemByRefId = resolvedMount.getMount().getHstSite().getSiteMap().getSiteMapItemByRefId(siteMapItemRefIdOrPath);
            if (siteMapItemByRefId != null) {
                siteMapPathInfo = HstSiteMapUtils.getPath(siteMapItemByRefId);
            } else {
                siteMapPathInfo = siteMapItemRefIdOrPath;
            }

            final ResolvedSiteMapItem resolvedSiteMapItem = resolvedMount.matchSiteMapItem(siteMapPathInfo);

            if (resolvedSiteMapItem.getPathInfo().equals(resolvedMount.getMount().getPageNotFound())) {
                // siteMapPathInfo item is resolved to the page not found item. this is an invalid item in the sitemap
                // item tree
                final String msg = String.format("For 'siteMapItemRefIdOrPath %s' the resolved sitemap item '%s' is the page not " +
                                "found sitemap item for which no tree picker representation can be created.",
                        siteMapItemRefIdOrPath, resolvedSiteMapItem.getHstSiteMapItem());
                throw new IllegalStateException(msg);
            }

            if (StringUtils.isEmpty(resolvedSiteMapItem.getRelativeContentPath())) {
                // if explicit sitemap item, return sitemap item representation
                // if sitemap item contains wildcards, the siteMapPathInfo is invalid as it cannot be represented in the
                // document OR sitemap tree

                if (resolvedSiteMapItem.getHstSiteMapItem().isExplicitPath()) {
                    return SiteMapTreePickerRepresentation.representExpandedParentTree(pageComposerContextService, resolvedSiteMapItem.getHstSiteMapItem());
                }
                final String msg = String.format("For 'siteMapPathInfo %s' the resolved sitemap item '%s' does not have a relative content path and " +
                        "is not an explicit sitemap item hence no tree picker representation can be created for it be " +
                        "created for it.", siteMapItemRefIdOrPath, resolvedSiteMapItem.getHstSiteMapItem());
                throw new IllegalStateException(msg);
            }

            final String contentRootPath = pageComposerContextService.getEditingMount().getContentPath();
            final String selectedPath = contentRootPath + "/" + resolvedSiteMapItem.getRelativeContentPath();
            final ExpandedNodeHierarchy expandedNodeHierarchy = createExpandedNodeHierarchy(jcrSession,
                    contentRootPath, Collections.singletonList(selectedPath));
            return new DocumentTreePickerRepresentation().represent(pageComposerContextService, expandedNodeHierarchy, true, selectedPath);

        } catch (ClientException e) {
            throw e;
        } catch (PathNotFoundException | MatchException | IllegalStateException e) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Exception trying to return document representation for siteMapPathInfo '%s'. Return root " +
                        "content folder representation instead", siteMapItemRefIdOrPath);
                log.info(msg, e);
            } else {
                log.info("Exception trying to return document representation for siteMapPathInfo '{}' : {}. Return root " +
                        "content folder representation instead", siteMapItemRefIdOrPath, e.toString());
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Exception trying to return document representation for siteMapPathInfo '%s'. Return root " +
                        "content folder representation instead", siteMapItemRefIdOrPath);
                log.warn(msg, e);
            } else {
                log.warn("Exception trying to return document representation for siteMapPathInfo '{}' : {}. Return root " +
                        "content folder representation instead", siteMapItemRefIdOrPath, e.toString());
            }
        }
        final ExpandedNodeHierarchy singleNodeHierarchy = ExpandedNodeHierarchy.createSingleNodeHierarchy(pageComposerContextService.getRequestConfigNode(NT_DOCUMENT));
        return new DocumentTreePickerRepresentation().represent(pageComposerContextService, singleNodeHierarchy, true, null);
    }

    private AbstractTreePickerRepresentation represent(final PageComposerContextService pageComposerContextService,
                                                       final ExpandedNodeHierarchy expandedNodeHierarchy,
                                                       final boolean includeChildren,
                                                       final String selectedPath) throws RepositoryException {

        final Node node = expandedNodeHierarchy.getNode();
        if (node.isNodeType(NT_DOCUMENT) && node.getParent().isNodeType(NT_HANDLE)) {
            throw new IllegalArgumentException(String.format("Node '%s' is document node. Representation only gets done until the '%s' node",
                    node.getPath(), NT_HANDLE));
        }
        setPickerType(PickerType.DOCUMENTS.getName());
        setId(node.getIdentifier());
        setNodeName(node.getName());
        setDisplayName(((HippoNode)node).getDisplayName());
        setNodePath(node.getPath());

        if (getNodePath().equals(selectedPath)) {
            setSelected(true);
        }

        if (includeChildren && !isSelected()) {
            setCollapsed(false);
        }

        if (node.isNodeType(NT_HANDLE)) {
            final Node document = JcrUtils.getNodeIfExists(node, node.getName());
            if (document != null && document.isNodeType(NT_PUBLISHABLESUMMARY)) {
                setState(document.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString());
            }
            setType(Type.DOCUMENT.getName());
        } else {
            setType(Type.FOLDER.getName());
        }


        final HstRequestContext requestContext = pageComposerContextService.getRequestContext();
        final HstLinkCreator linkCreator = requestContext.getHstLinkCreator();

        final Mount editingMount = pageComposerContextService.getEditingMount();
        final HstLink hstLink = linkCreator.create(node, editingMount);
        if (isFolder(node) && ((HstSiteMapItemService)hstLink.getHstSiteMapItem()).getExtension() != null ) {
            // The hstLink is for a wildcard/any matcher *with* an extension while 'node' represents a folder:
            // In general folder can be returned as a link with extension, eg, /news/2014.html if there is only a **.html
            // matcher but not a ** matcher. However the URL /news/2014.html most of the time results in an error page
            // because unintentional
            setSelectable(false);
        } else if (hstLink.isNotFound() || node.isSame(node.getSession().getNode(editingMount.getContentPath()))) {
            setSelectable(false);
        } else {
            setSelectable(true);
            if (StringUtils.isEmpty(hstLink.getPath())) {
                // homepage. However we need the sitemap reference path to the homepage sitemap item
                setPathInfo(HstSiteMapUtils.getPath(editingMount, editingMount.getHomePage()));
            } else {
                setPathInfo(hstLink.getPath());
            }
        }

        setLeaf(true);
        if (!node.isNodeType(NT_HANDLE)) {
            for (Node child : new NodeIterable(node.getNodes())) {
                try {
                    ExpandedNodeHierarchy childHierarchy = expandedNodeHierarchy.getChildren().get(child.getPath());
                    if (child.isNodeType(NT_DOCUMENT) || child.isNodeType("hippofacnav:facetnavigation")) {
                        setExpandable(true);
                        setLeaf(false);
                    } else if (child.isNodeType(NT_HANDLE)) {
                        // do nothing
                        setLeaf(false);
                    } else {
                        log.debug("Skipping child node '{}' that is not a folder or handle.", child.getPath());
                        continue;
                    }
                    if (isSelected()) {
                        log.debug("Item '{}' is selected so we do not load the children.", node.getPath());
                        continue;
                    }
                    if (childHierarchy == null) {
                        if (includeChildren) {
                            ExpandedNodeHierarchy childOnly = ExpandedNodeHierarchy.createSingleNodeHierarchy(child);
                            AbstractTreePickerRepresentation childRepresentation = new DocumentTreePickerRepresentation()
                                    .represent(pageComposerContextService, childOnly, false, selectedPath);
                            getItems().add(childRepresentation);
                        }
                    } else {
                        boolean includeChildrenForChild = !child.isNodeType(NT_HANDLE);
                        AbstractTreePickerRepresentation childRepresentation = new DocumentTreePickerRepresentation()
                                .represent(pageComposerContextService, childHierarchy, includeChildrenForChild, selectedPath);
                        getItems().add(childRepresentation);
                    }

                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Exception while trying to add child '{}'.", child.getPath(), e);
                    } else {
                        log.warn("Exception while trying to add child '{}' : {}", child.getPath(), e.toString());
                    }
                }
                // else ignore
            }
        }

        final boolean jcrOrder = node.getPrimaryNodeType().hasOrderableChildNodes();
        if (!jcrOrder && Type.FOLDER.getName().equals(getType())) {
            // order alphabetically, first folders then documents
            Collections.sort(getItems(), comparator);
        }

        return this;
    }

    private boolean isFolder(final Node node) throws RepositoryException {
        if (node.isNodeType(NT_FOLDER) || node.isNodeType(NT_DIRECTORY)) {
            return true;
        }
        return false;
    }

    public static class ExpandedNodeHierarchy {

        private Node node;
        private final Map<String, ExpandedNodeHierarchy> children = new HashMap<>();

        private ExpandedNodeHierarchy() {
        }

        public static ExpandedNodeHierarchy createSingleNodeHierarchy(final Node node) {
            ExpandedNodeHierarchy single = new ExpandedNodeHierarchy();
            single.node = node;
            return single;
        }

        public static ExpandedNodeHierarchy createExpandedNodeHierarchy(final Session session,
                                                                        final String rootContentPath,
                                                                        final List<String> expandedPaths) throws RepositoryException {
            ExpandedNodeHierarchy hierarchy = new ExpandedNodeHierarchy();

            hierarchy.node = session.getNode(rootContentPath);
            for (String expandedPath : expandedPaths) {
                if (expandedPath.equals(rootContentPath)) {
                    continue;
                }
                if (!expandedPath.startsWith(rootContentPath + "/")) {
                    log.warn("Cannot expand hierarchy to path '{}' because not a descendant of '{}'", expandedPath, rootContentPath);
                    continue;
                }

                String relativePath = expandedPath.substring(rootContentPath.length() + 1);
                appendChild(relativePath, hierarchy);
            }
            return hierarchy;
        }

        public Node getNode() {
            return node;
        }

        public Map<String, ExpandedNodeHierarchy> getChildren() {
            return children;
        }

        private static void appendChild(final String relativePath, final ExpandedNodeHierarchy parent) throws RepositoryException {
            String childName = StringUtils.substringBefore(relativePath, "/");
            if (!parent.node.hasNode(childName)) {
                log.info("Cannot find childName '{}' for node '{}'.", childName, parent.node.getPath());
                return;
            }

            Node child = parent.node.getNode(childName);

            ExpandedNodeHierarchy childHierarchy = parent.children.get(child.getPath());
            if (childHierarchy == null) {
                childHierarchy = new ExpandedNodeHierarchy();
                childHierarchy.node = child;
                parent.children.put(child.getPath(), childHierarchy);
            }

            final String remaining = StringUtils.substringAfter(relativePath, "/");
            if (StringUtils.isNotEmpty(remaining)) {
                childHierarchy.appendChild(remaining, childHierarchy);
            }
        }

    }
}
