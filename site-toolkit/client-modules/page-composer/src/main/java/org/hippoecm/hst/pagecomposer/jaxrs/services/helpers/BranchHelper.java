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
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_ID;
import static org.hippoecm.hst.configuration.HstNodeTypes.BRANCH_PROPERTY_BRANCH_OF;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_BRANCH;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils.createPreviewConfiguration;

public class BranchHelper {

    private static final Logger log = LoggerFactory.getLogger(BranchHelper.class);

    private PageComposerContextService pageComposerContextService;

    public void setPageComposerContextService(PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    /**
     * creates {@code branchId} for mount that is being edited. This method does not persist the JCR changes
     */
    public void createBranch(final String branchId, final Session session) throws RepositoryException, ClientException {
        final Mount editingMount = pageComposerContextService.getEditingMount();
        Channel channel = editingMount.getChannel();
        if (channel.getBranchId() != null) {
            throw new ClientException(String.format("Only branching from master is currently supported. Cannot branch " +
                            "from '%s' which is currently rendered. First select master before branching",
                    channel.getHstConfigPath()), ClientError.BRANCHING_NOT_ALLOWED);
        }
        assertValidBranchName(branchId);

        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();

        final String liveConfigurationPath = pageComposerContextService.getEditingLiveConfigurationPath();

        if (!editingPreviewSite.hasPreviewConfiguration()) {
            // when creating a branch, there must be a -preview version of the to-be-branched-configuration
            createPreviewConfiguration(liveConfigurationPath, session);
        }

        Node liveMasterConfigurationNode = session.getNode(liveConfigurationPath);
        String liveConfigName = StringUtils.substringAfterLast(liveConfigurationPath, "/");
        String liveBranchName = liveConfigName + "-" + branchId;
        if (liveMasterConfigurationNode.getParent().hasNode(liveBranchName)) {
            throw new ClientException(String.format("Branch '%s' cannot be created or '%s' because it exists already",
                    liveBranchName, liveConfigurationPath), ClientError.ITEM_EXISTS);
        }
        Node liveBranchConfigNode = liveMasterConfigurationNode.getParent().addNode(liveBranchName, NODETYPE_HST_CONFIGURATION);
        liveBranchConfigNode.addMixin(MIXINTYPE_HST_BRANCH);
        liveBranchConfigNode.setProperty(BRANCH_PROPERTY_BRANCH_OF, liveConfigName);
        liveBranchConfigNode.setProperty(BRANCH_PROPERTY_BRANCH_ID, branchId);
        liveBranchConfigNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../" + liveConfigName});
        // TODO if 'master config node' has inheritance(s) that point to hst:workspace, then most likely that should be copied
        // TODO as well to the preview config, see HSTTWO-3965
        if (session.nodeExists(liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE)) {
            JcrUtils.copy(session, liveConfigurationPath + "/" + NODENAME_HST_WORKSPACE, liveBranchConfigNode.getPath() + "/" + NODENAME_HST_WORKSPACE);
        }
        HstConfigurationUtils.createMandatoryWorkspaceNodesIfMissing(liveBranchConfigNode.getPath(), session);

        // we need for branches directly a preview as well otherwise we can't select this branch via #selectBranch : That is
        // because the 'editingMount' is decorated to preview and hence will return only the preview channels
        JcrUtils.copy(session, liveBranchConfigNode.getPath(), liveBranchConfigNode.getPath() + "-preview");
        // TODO if 'master config node' has inheritance(s) that point to hst:workspace, then most likely that should be copied
        // TODO as well to the preview config, see HSTTWO-3965
        Node previewBranchNode = session.getNode(liveBranchConfigNode.getPath() + "-preview");
        previewBranchNode.setProperty(GENERAL_PROPERTY_INHERITS_FROM,
                new String[]{"../" + liveBranchConfigNode.getName()});
        previewBranchNode.setProperty(BRANCH_PROPERTY_BRANCH_OF, liveConfigName + "-preview");

        log.info("Branch '{}' created.", liveBranchConfigNode.getName());
    }

    private void assertValidBranchName(final String branchId) {
        if (StringUtils.isBlank(branchId) || branchId.equals("preview")) {
            throw new ClientException(String.format("Invalid branchId '%s'", branchId), ClientError.INVALID_NAME);
        }
        String encoded = NodeNameCodec.encode(branchId);
        if (!branchId.equals(encoded)) {
            throw new ClientException(String.format("Invalid branchId '%s'", branchId), ClientError.INVALID_NAME);
        }
    }
}
