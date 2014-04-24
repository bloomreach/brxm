/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_ABSTRACT_COMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.PROTOTYPE_META_PROPERTY_PRIMARY_CONTAINER;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;

public class PagesHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(PagesHelper.class);

    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    public Node create(final Node prototype, final String targetPageNodeName) throws RepositoryException {
        return create(prototype, targetPageNodeName, null, false);
    }


    public Node create(final Node pageOrPrototype,
                       final String targetPageNodeName,
                       final HstComponentConfiguration pageInstance,
                       final boolean skipContainerItems) throws RepositoryException {
        final String previewWorkspacePagesPath = getPreviewWorkspacePagesPath();

        final Session session = pageComposerContextService.getRequestContext().getSession();
        final String validTargetPageNodeName = getValidTargetPageNodeName(previewWorkspacePagesPath, targetPageNodeName, session);
        final Node newPage = JcrUtils.copy(session, pageOrPrototype.getPath(), previewWorkspacePagesPath + "/" + validTargetPageNodeName);

        if (newPage.isNodeType(HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META)) {
            newPage.removeMixin(HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META);
        }
        if (skipContainerItems) {
            removeContainerItems(newPage);
        }
        if (pageInstance != null) {
            // copy has been done from a page, not from a prototype. We need to check whether there
            // are no hst:containercomponentreference used. If so, we need to denormalize them. For that, we need the
            // pageInstance
            denormalizeContainerComponentReferences(newPage, pageInstance);
        }

        lockHelper.acquireLock(newPage, 0);
        // lock all available containers below newPage
        for (Node eachContainer : findContainers(newPage)) {
            lockHelper.acquireSimpleLock(eachContainer, 0);
        }

        return newPage;
    }

    public Node reapply(final Node newPrototypePage,
                        final Node oldPage,
                        final String targetPageNodeName) throws RepositoryException {

        // check if not locked
        lockHelper.acquireLock(oldPage, 0);
        Node newPage = create(newPrototypePage, targetPageNodeName, null, true);

        Node primaryContainer = findPrimaryContainer(newPage, getStringProperty(newPrototypePage, PROTOTYPE_META_PROPERTY_PRIMARY_CONTAINER, null));
        if (primaryContainer == null) {
            noContainerLeftInfoLog(newPrototypePage, oldPage);
            deletePageNodeIfNotReferencedAnyMore(oldPage);
            return newPage;
        }

        String newPagePathPrefix = "/" + HstNodeTypes.NODENAME_HST_PAGES + "/" + newPage.getName() + "/";
        String oldPagePathPrefix = "/" + HstNodeTypes.NODENAME_HST_PAGES + "/" + oldPage.getName() + "/";

        List<Node> existingContainers = findContainers(oldPage);
        Session session = oldPage.getSession();
        List<Node> nonRelocatedContainers = new ArrayList<>();
        for (Node existingContainer : existingContainers) {
            String targetContainerPath = existingContainer.getPath().replace(oldPagePathPrefix, newPagePathPrefix);
            if (session.nodeExists(targetContainerPath)) {
                Node targetContainer = session.getNode(targetContainerPath);
                moveContainerItems(existingContainer, targetContainer);
            } else {
                nonRelocatedContainers.add(existingContainer);
            }
        }

        if (!nonRelocatedContainers.isEmpty()) {
            log.info("Re-applying prototype results in moving container items that could not be" +
                    " relocated to the same containers:");
            for (Node container : nonRelocatedContainers) {
                log.info("Moving container items from '{}' to '{}'", container.getPath(),
                        primaryContainer.getPath());
                moveContainerItems(container, primaryContainer);
            }
        }

        deletePageNodeIfNotReferencedAnyMore(oldPage);
        return newPage;
    }


    private void removeContainerItems(final Node page) throws RepositoryException {
        final List<Node> containers = findContainers(page);
        for (Node container : containers) {
            for (Node child : new NodeIterable(container.getNodes())) {
                log.debug("Remove container item '{}' for container '{}'.", child.getName(), container.getPath());
                child.remove();
            }
        }
    }

    private void moveContainerItems(final Node from, final Node to) throws RepositoryException {
        Session session = from.getSession();
        for (Node fromChild : new NodeIterable(from.getNodes())) {
            String newName = fromChild.getName();
            int counter = 0;
            while (to.hasNode(newName)) {
                newName = fromChild.getName() + ++counter;
            }
            session.move(fromChild.getPath(), to.getPath() + "/" + newName);
        }
    }

    /**
     * @return the container that is marked to be the primary one or if none marked or found for the primary, return the
     * first container. If <code>newPage</code> does not contain any container, <code>null</code> is returned
     */
    private Node findPrimaryContainer(final Node page, final String primaryContainerRelPath) throws RepositoryException {
        final List<Node> pageContainers = findContainers(page);
        if (pageContainers.isEmpty()) {
            log.debug("No containers available on page '{}'", page.getPath());
            return null;
        } else if (primaryContainerRelPath != null) {
            int pageNodePathPrefixLength = page.getPath().length() + 1;
            for (Node pageContainer : pageContainers) {
                String relContainerPath = pageContainer.getPath().substring(pageNodePathPrefixLength);
                if (primaryContainerRelPath.equals(relContainerPath)) {
                    return pageContainer;
                }
            }
            log.warn("Could not find primary container '{}' for page '{}'. Return first container instead.",
                    primaryContainerRelPath, page.getPath());
            return pageContainers.get(0);
        } else {
            log.debug("No primary container configured on prototype. Return first container for page '{}' " +
                    "as primary container.", page.getPath());
            return pageContainers.get(0);
        }
    }

    private List<Node> findContainers(final Node node) throws RepositoryException {
        List<Node> existingContainers = new ArrayList<>();
        findContainers(node, existingContainers);
        return existingContainers;
    }

    private void findContainers(final Node node, List<Node> containers) throws RepositoryException {
        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT)) {
            containers.add(node);
            // container component nodes never have container component children
            return;
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            findContainers(child, containers);
        }
    }


    private void noContainerLeftInfoLog(final Node newPrototypePage, final Node oldPage) throws RepositoryException {
        log.info("Re-applying prototype '{}' that does not have container components. Container items " +
                "in the previous page definition will be deleted.", newPrototypePage.getPath());
        if (log.isInfoEnabled()) {
            List<Node> existingContainers = findContainers(oldPage);
            for (Node existingContainer : existingContainers) {
                for (Node containerItem : new NodeIterable(existingContainer.getNodes())) {
                    log.info("Container item '{}' will be removed since the new prototype does not " +
                            "have any containers.", containerItem.getPath());
                }
            }
        }
    }


    private void denormalizeContainerComponentReferences(final Node newPage,
                                                         final HstComponentConfiguration pageInstance) throws RepositoryException {

        final List<Node> containerComponentReferenceNodes = new ArrayList<>();
        populateComponentReferenceNodes(newPage, containerComponentReferenceNodes);

        if (containerComponentReferenceNodes.size() > 0) {
            final String pagePath = newPage.getPath();
            // start denormalization
            for (Node containerComponentReferenceNode : containerComponentReferenceNodes) {
                // at the location of containerComponentReferenceNode, in the pageConfig the HST has
                // used the referenced node: Hence, we can use the canonical identifier from that to
                // denormalize. The canonical identifier can even belong to inherited configurations
                final String absPath = containerComponentReferenceNode.getPath();
                final String relPath = absPath.substring(pagePath.length() + 1);
                containerComponentReferenceNode.remove();
                String[] elems = relPath.split("/");
                HstComponentConfiguration current = pageInstance;
                for (String elem : elems) {
                    if (current == null) {
                        break;
                    }
                    current = current.getChildByName(elem);
                }
                if (current == null) {
                    log.warn("Could not find hst component configuration for component reference node '{}', hence we " +
                            "cannot denormalize the reference. Instead, replace the reference with a empty container node.", absPath);
                    Node container = newPage.addNode(relPath, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
                    container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
                } else {
                    // current now contains the component that we need to denormalize
                    JcrUtils.copy(newPage.getSession(), current.getCanonicalStoredLocation(), absPath);
                    log.info("Succesfully denormalized '{}'", absPath);
                }
            }
        }
    }

    private void populateComponentReferenceNodes(final Node node,
                                                 final List<Node> containerComponentReferenceNodes) throws RepositoryException {
        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE)) {
            containerComponentReferenceNodes.add(node);
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            populateComponentReferenceNodes(child, containerComponentReferenceNodes);
        }
    }

    public void delete(final Node sitemapItemNodeToDelete) throws RepositoryException {
        final String componentConfigId = getStringProperty(sitemapItemNodeToDelete, SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, null);
        if (componentConfigId == null) {
            log.debug("No component id configured for '{}'. No page to delete.", sitemapItemNodeToDelete.getPath());
            return;
        }
        final String pageNodePath = getPreviewWorkspacePath() + "/" + componentConfigId;
        final Session session = pageComposerContextService.getRequestContext().getSession();
        if (!session.nodeExists(pageNodePath)) {
            log.info("No page found in hst:workspace for '{}' which is referenced by sitemap item '{}'. Skip deleting the page",
                    pageNodePath, sitemapItemNodeToDelete.getPath());
            return;
        }
        Node pageNode = session.getNode(pageNodePath);
        lockHelper.acquireLock(pageNode, 0);
        deletePageNodeIfNotReferencedAnyMore(pageNode);
    }

    private void deletePageNodeIfNotReferencedAnyMore(final Node pageNode) throws RepositoryException {
        String componentConfigurationIdForPage = HstNodeTypes.NODENAME_HST_PAGES + "/" + pageNode.getName();
        final Session session = pageNode.getSession();
        Node workspaceSiteMapNode = session.getNode(getPreviewWorkspacePath()).getNode(HstNodeTypes.NODENAME_HST_SITEMAP);
        Node nonWorkspaceSiteMapNode = null;
        final String nonWorkspaceSiteMapPath = pageComposerContextService.getEditingPreviewSite().getConfigurationPath()
                + "/" + HstNodeTypes.NODENAME_HST_SITEMAP;
        if (session.nodeExists(nonWorkspaceSiteMapPath)) {
            nonWorkspaceSiteMapNode = session.getNode(nonWorkspaceSiteMapPath);
        }

        final int inUseCounter = countIdReferencedByNumberOfSiteMapItems(workspaceSiteMapNode, nonWorkspaceSiteMapNode, componentConfigurationIdForPage);
        if (inUseCounter < 2) {
            deleteOrMarkDeletedIfLiveExists(pageNode);
        } else {
            log.info("Cannot delete page as it is in use by {} sitemap items.", inUseCounter);
        }
    }

    private int countIdReferencedByNumberOfSiteMapItems(final Node workspaceSiteMapNode,
                                                        final Node nonWorkspaceSiteMapNode,
                                                        final String componentConfigurationIdForPage) throws RepositoryException {
        int refCounter = countReferences(new NodeIterable(workspaceSiteMapNode.getNodes()), componentConfigurationIdForPage);
        if (nonWorkspaceSiteMapNode != null) {
            refCounter += countReferences(new NodeIterable(nonWorkspaceSiteMapNode.getNodes()), componentConfigurationIdForPage);
        }
        return refCounter;
    }

    private int countReferences(final NodeIterable nodes, final String componentConfigurationIdForPage) throws RepositoryException {
        int refCounter = 0;
        for (Node node : nodes) {
            final String compId = JcrUtils.getStringProperty(node, HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, null);
            if (componentConfigurationIdForPage.equals(compId)) {
                refCounter++;
            }
            refCounter += countReferences(new NodeIterable(node.getNodes()), componentConfigurationIdForPage);
        }
        return refCounter;
    }

    private String getValidTargetPageNodeName(final String previewWorkspacePagesPath, final String targetPageNodeName, final Session session) throws RepositoryException {
        String testTargetNodeName = targetPageNodeName;
        for (int counter = 1; !isValidTarget(session, testTargetNodeName, previewWorkspacePagesPath, getPreviewPagesPath()); counter++) {
            log.info("targetPageNodeName '{}' not valid. Trying next one.", targetPageNodeName);
            testTargetNodeName = targetPageNodeName + "-" + counter;
        }
        return testTargetNodeName;
    }

    private boolean isValidTarget(final Session session,
                                  final String testTargetNodeName,
                                  final String previewWorkspacePagesPath,
                                  final String previewPagesPath) throws RepositoryException {
        final String testWorkspaceTargetNodePath = previewWorkspacePagesPath + "/" + testTargetNodeName;
        if (session.nodeExists(testWorkspaceTargetNodePath)) {
            Node targetNode = session.getNode(testWorkspaceTargetNodePath);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                try {
                    lockHelper.acquireLock(targetNode, 0);
                } catch (ClientException e) {
                    return false;
                }
                targetNode.remove();
            } else {
                return false;
            }
        }
        // the targetNodeName does not yet exist in workspace pages. Confirm it does not exist non workspace pages
        return !session.nodeExists(previewPagesPath + "/" + testTargetNodeName);
    }

    private String getPreviewWorkspacePagesPath() {
        return getPreviewWorkspacePath() + "/" + NODENAME_HST_PAGES;
    }

    private String getPreviewPagesPath() {
        return pageComposerContextService.getEditingPreviewSite().getConfigurationPath()
                + "/" + NODENAME_HST_PAGES;
    }


    @Override
    protected String buildXPathQueryLockedNodesForUsers(final String previewConfigurationPath,
                                                        final List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewConfigurationPath));
        xpath.append("//element(*,");
        xpath.append(NODETYPE_HST_ABSTRACT_COMPONENT);
        xpath.append(")[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

}
