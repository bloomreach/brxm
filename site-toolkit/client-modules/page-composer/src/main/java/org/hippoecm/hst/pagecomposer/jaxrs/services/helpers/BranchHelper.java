/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.branch.WorkspaceHasher;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_OF;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_BRANCH;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_UPSTREAM;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils.createMandatoryWorkspaceNodesIfMissing;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils.createPreviewConfiguration;

public class BranchHelper {

    private static final Logger log = LoggerFactory.getLogger(BranchHelper.class);

    private PageComposerContextService pageComposerContextService;
    private WorkspaceHasher workspaceHasher;

    public void setPageComposerContextService(PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    public void setWorkspaceHasher(final WorkspaceHasher workspaceHasher) {
        this.workspaceHasher = workspaceHasher;
    }

    /**
     * creates {@code branchId} for mount that is being edited. This method does not persist the JCR changes
     */
    public void createBranch(final String branchId, final Session session) throws RepositoryException, ClientException {
        final Mount editingMount = pageComposerContextService.getEditingMount();

        assertChannelIsMaster(editingMount);
        assertValidBranchName(branchId);

        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();
        final String liveConfigurationPath = pageComposerContextService.getEditingLiveConfigurationPath();

        if (!editingPreviewSite.hasPreviewConfiguration()) {
            // when creating a branch, there must be a -preview version of the to-be-branched-configuration
            createPreviewConfiguration(liveConfigurationPath, session);
        }

        final Node liveMasterConfigurationNode = session.getNode(liveConfigurationPath);

        final Node liveBranchConfigurationNode = createLiveBranchConfiguration(branchId, liveMasterConfigurationNode);

        createPreviewBranchConfiguration(liveBranchConfigurationNode, liveMasterConfigurationNode.getName());

        // to the live configuration add hst:upstream with hashes which is required for later merging
        createLiveBranchUpstreamWorkspace(liveBranchConfigurationNode);

        log.info("Branch '{}' created.", liveBranchConfigurationNode.getName());
    }

    private Node createLiveBranchConfiguration(final String branchId, final Node liveMasterConfigurationNode) throws RepositoryException {

        final String liveConfigName = liveMasterConfigurationNode.getName();
        final String liveBranchName = liveConfigName + "-" + branchId;
        final String liveConfigurationPath = liveMasterConfigurationNode.getPath();

        assertBranchDoesNotExist(liveConfigurationPath, liveMasterConfigurationNode, liveBranchName);

        final Session session = liveMasterConfigurationNode.getSession();
        assertWorkspaceExists(liveConfigurationPath, session);

        final Node liveBranchConfigNode = liveMasterConfigurationNode.getParent().addNode(liveBranchName, NODETYPE_HST_CONFIGURATION);
        liveBranchConfigNode.addMixin(MIXINTYPE_HST_BRANCH);
        liveBranchConfigNode.setProperty(BRANCH_PROPERTY_BRANCH_OF, liveConfigName);
        liveBranchConfigNode.setProperty(BRANCH_PROPERTY_BRANCH_ID, branchId);
        liveBranchConfigNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../" + liveConfigName});
        // TODO if 'master config node' has inheritance(s) that point to hst:workspace, then most likely that should be copied
        // TODO as well to the preview config, see HSTTWO-3965

        if (session.nodeExists(liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE)) {
            JcrUtils.copy(session, liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE, liveBranchConfigNode.getPath() + "/" + NODENAME_HST_WORKSPACE);
        }
        createMandatoryWorkspaceNodesIfMissing(liveBranchConfigNode.getPath(), session);
        return liveBranchConfigNode;
    }

    private void createPreviewBranchConfiguration(final Node liveBranchConfigurationNode, final String liveMasterConfigurationName) throws RepositoryException {
        final Session session = liveBranchConfigurationNode.getSession();
        // we need for branches directly a preview as well otherwise we can't select this branch via #selectBranch : That is
        // because the 'editingMount' is decorated to preview and hence will return only the preview channels
        JcrUtils.copy(session, liveBranchConfigurationNode.getPath(), liveBranchConfigurationNode.getPath() + "-preview");
        // TODO if 'master config node' has inheritance(s) that point to hst:workspace, then most likely that should be copied
        // TODO as well to the preview config, see HSTTWO-3965
        final Node previewBranchNode = session.getNode(liveBranchConfigurationNode.getPath() + "-preview");
        previewBranchNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM,
                new String[]{"../" + liveBranchConfigurationNode.getName()});
        previewBranchNode.setProperty(BRANCH_PROPERTY_BRANCH_OF, liveMasterConfigurationName + "-preview");
    }

    private void createLiveBranchUpstreamWorkspace(final Node liveBranchConfigurationNode) throws RepositoryException {
        final Session session = liveBranchConfigurationNode.getSession();
        JcrUtils.copy(session, liveBranchConfigurationNode.getPath() + "/" + NODENAME_HST_WORKSPACE,
                liveBranchConfigurationNode.getPath() + "/" + NODENAME_HST_UPSTREAM);
        Node upstream = session.getNode(liveBranchConfigurationNode.getPath() + "/" + NODENAME_HST_UPSTREAM);
        workspaceHasher.hash(upstream, true);
    }

    private void assertBranchDoesNotExist(final String liveConfigurationPath, final Node liveMasterConfigurationNode, final String liveBranchName) throws RepositoryException {
        if (liveMasterConfigurationNode.getParent().hasNode(liveBranchName)) {
            throw new ClientException(String.format("Branch '%s' cannot be created or '%s' because it exists already",
                    liveBranchName, liveConfigurationPath), ClientError.ITEM_EXISTS);
        }
    }


    private void assertChannelIsMaster(final Mount editingMount) {
        final Channel channel = editingMount.getChannel();
        if (channel.getBranchId() != null) {
            throw new ClientException(String.format("Only branching from master is currently supported. Cannot branch " +
                            "from '%s' which is currently rendered. First select master before branching",
                    channel.getHstConfigPath()), ClientError.BRANCHING_NOT_ALLOWED);
        }
    }

    private void assertValidBranchName(final String branchId) {
        if (StringUtils.isBlank(branchId) || branchId.equals("preview")) {
            throw new ClientException(String.format("Invalid branchId '%s'", branchId), ClientError.INVALID_NAME);
        }
        final String encoded = NodeNameCodec.encode(branchId);
        if (!branchId.equals(encoded)) {
            throw new ClientException(String.format("Invalid branchId '%s'", branchId), ClientError.INVALID_NAME);
        }
    }
    private void assertWorkspaceExists(final String liveConfigurationPath, final Session session) throws RepositoryException {
        if (!session.nodeExists(liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE)) {
            throw new ClientException(String.format("Configuration '%s' does not contain '%s'. Cannot branch. Add workspace first.",
                    liveConfigurationPath), ClientError.BRANCHING_NOT_ALLOWED);
        }
    }

}
