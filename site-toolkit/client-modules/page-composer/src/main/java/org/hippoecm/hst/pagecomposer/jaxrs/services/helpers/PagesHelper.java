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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagesHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(PagesHelper.class);

    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    public Node create(final Node prototypePage, final Node siteMapNode) throws RepositoryException {
        validatePrototypePage(prototypePage);
        String previewWorkspacePagesPath = getPreviewWorkspacePagesPath();
        final String targetPageNodeName = getSitemapPathPrefixPart(siteMapNode) + "-" + prototypePage.getName();
        final Session session = pageComposerContextService.getRequestContext().getSession();
        int counter = 0;
        while (!isValidateTarget(session, targetPageNodeName, counter, previewWorkspacePagesPath, getPreviewPagesPath())) {
            log.info("targetPageNodeName '{}' not valid. Trying next one.", targetPageNodeName);
            counter++;
        }
        final String validTargetPageNodeName;
        if (counter == 0) {
            validTargetPageNodeName = targetPageNodeName;
        } else {
            validTargetPageNodeName = targetPageNodeName + "-" + counter;
        }
        Node newPage = JcrUtils.copy(session, prototypePage.getPath(), previewWorkspacePagesPath + "/" + validTargetPageNodeName);
        lockHelper.acquireLock(newPage);
        return newPage;
    }


    public void delete(final Node sitemapItemNodeToDelete) throws RepositoryException {
        final String componentConfigId = JcrUtils.getStringProperty(sitemapItemNodeToDelete, HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, null);
        if (componentConfigId == null){
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
        lockHelper.acquireLock(pageNode);
        deleteOrMarkDeletedIfLiveExists(pageNode);
    }

    // TODO when implementing re-applying a prototype to a page, ensure the page is from the workspace!

    private void validatePrototypePage(final Node component) throws RepositoryException {
        if (!component.isNodeType("hst:abstractcomponent")) {
            throw new ClientException("Expected node of subtype 'hst:abstractcomponent'", ClientError.INVALID_NODE_TYPE);
        }
        if (component.isNodeType(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT)) {
            String message = String.format("Prototype page is not allowed to contain nodes of type '%s' but there is one " +
                    "at '%s'. Prototype page cannot be used", HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, component.getPath());
            throw new ClientException(message, ClientError.INVALID_NODE_TYPE);
        }
        for (Node child : new NodeIterable(component.getNodes())) {
            validatePrototypePage(child);
        }
    }

    private String getSitemapPathPrefixPart(final Node siteMapNode) throws RepositoryException {
        Node crNode = siteMapNode;
        StringBuilder sitemapPathPrefixBuilder = new StringBuilder();
        while (crNode.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAPITEM)) {
            if (sitemapPathPrefixBuilder.length() > 0 ) {
                sitemapPathPrefixBuilder.insert(0,"-");
            }
            sitemapPathPrefixBuilder.insert(0,crNode.getName());
            crNode = crNode.getParent();
        }
        return sitemapPathPrefixBuilder.toString();
    }

    private boolean isValidateTarget(final Session session,
                                final String targetNodeName,
                                final int counter,
                                final String previewWorkspacePagesPath,
                                final String previewPagesPath) throws RepositoryException {
        String testTargetNodeName = targetNodeName;
        if (counter > 0) {
            testTargetNodeName = testTargetNodeName +"-" + counter;
        }

        String testWorkspaceTargetNodePath = previewWorkspacePagesPath + "/" + testTargetNodeName;
        if (session.nodeExists(testWorkspaceTargetNodePath)) {
            Node targetNode = session.getNode(testWorkspaceTargetNodePath);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                try {
                    lockHelper.acquireLock(targetNode);
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
        return pageComposerContextService.getEditingPreviewSite().getConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
    }

    private String getPreviewWorkspacePagesPath() {
        return getPreviewWorkspacePath() + "/" + HstNodeTypes.NODENAME_HST_PAGES;
    }

    private String getPreviewPagesPath() {
        return pageComposerContextService.getEditingPreviewSite().getConfigurationPath()
                + "/" + HstNodeTypes.NODENAME_HST_PAGES;
    }


    @Override
    protected String buildXPathQueryLockedWorkspaceNodesForUsers(final String previewWorkspacePath,
                                                                 final List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewWorkspacePath + "/" + HstNodeTypes.NODENAME_HST_PAGES));
        // /element to get direct children below pages and *not* //element
        xpath.append("/element(*,");
        xpath.append(HstNodeTypes.NODETYPE_HST_COMPONENT);
        xpath.append(")[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

}
