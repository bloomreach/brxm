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
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_ABSTRACT_COMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;

public class PagesHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(PagesHelper.class);

    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    public Node create(final Node prototype, final Node siteMapNode) throws RepositoryException {
        return create(prototype, null, siteMapNode);
    }


    public Node create(final Node nodePageToCopy,
                       final HstComponentConfiguration pageConfigToCopy,
                       final Node siteMapNode) throws RepositoryException {
        String previewWorkspacePagesPath = getPreviewWorkspacePagesPath();
        final String targetPageNodeName = getSiteMapPathPrefixPart(siteMapNode) + "-" + nodePageToCopy.getName();
        final Session session = pageComposerContextService.getRequestContext().getSession();
        final String validTargetPageNodeName = getValidTargetPageNodeName(previewWorkspacePagesPath, targetPageNodeName, session);
        Node newPage = JcrUtils.copy(session, nodePageToCopy.getPath(), previewWorkspacePagesPath + "/" + validTargetPageNodeName);
        if (pageConfigToCopy != null) {
            // copy has been done from a page, not from a prototype. We need to check whether there
            // are no hst:containercomponentreference used. If so, we need to denormalize them. For that, we need the
            // pageConfigToCopy
            denormalizeContainerComponentReferences(newPage, pageConfigToCopy);
        }

        lockHelper.acquireLock(newPage, 0);
        // lock all available containers below newPage
        doLockContainers(newPage);

        return newPage;
    }

    private void denormalizeContainerComponentReferences(final Node newPage,
                                                         final HstComponentConfiguration pageConfig) throws RepositoryException {

        final List<Node> containerComponentReferenceNodes = new ArrayList<>();
        populateComponentReferenceNodes(newPage, containerComponentReferenceNodes);

        if (containerComponentReferenceNodes.size() > 0) {
            String pagePath = newPage.getPath();
            // start denormalization
            for (Node containerComponentReferenceNode : containerComponentReferenceNodes) {
                // at the location of containerComponentReferenceNode, in the pageConfig the HST has
                // used the referenced node: Hence, we can use the canonical identifier from that to
                // denormalize. The canonical identifier can even belong to inherited configurations
                final String absPath = containerComponentReferenceNode.getPath();
                final String relPath = absPath.substring(pagePath.length() + 1);
                containerComponentReferenceNode.remove();
                String[] elems = relPath.split("/");
                HstComponentConfiguration current = pageConfig;
                for (String elem : elems) {
                    if (current == null) {
                        log.warn("Could not find hst component configuration for component reference node '{}', hence we " +
                                "cannot denormalize the reference. Instead, replace the reference with a empty container node.");
                        Node container = newPage.addNode(relPath, HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
                        container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
                        break;
                    }
                    current = current.getChildByName(elem);
                }

                if (current != null) {
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

    private void doLockContainers(final Node node) throws RepositoryException {
        if (node.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)) {
            lockHelper.acquireSimpleLock(node, 0);
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            doLockContainers(child);
        }
    }

    public void delete(final Node sitemapItemNodeToDelete) throws RepositoryException {
        final String componentConfigId = JcrUtils.getStringProperty(sitemapItemNodeToDelete, SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, null);
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
        deleteOrMarkDeletedIfLiveExists(pageNode);
    }

    private String getSiteMapPathPrefixPart(final Node siteMapNode) throws RepositoryException {
        Node crNode = siteMapNode;
        StringBuilder siteMapPathPrefixBuilder = new StringBuilder();
        while (crNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAPITEM)) {
            if (siteMapPathPrefixBuilder.length() > 0) {
                siteMapPathPrefixBuilder.insert(0, "-");
            }
            siteMapPathPrefixBuilder.insert(0, crNode.getName());
            crNode = crNode.getParent();
        }
        return siteMapPathPrefixBuilder.toString();
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

    protected String getPreviewWorkspacePath() {
        return pageComposerContextService.getEditingPreviewSite().getConfigurationPath() + "/" + NODENAME_HST_WORKSPACE;
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
