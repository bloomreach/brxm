/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.reviewedactions;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.frontend.util.DocumentUtils;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.util.WorkflowUtils;
import org.onehippo.repository.branch.BranchConstants;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDocumentWorkflowPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(AbstractDocumentWorkflowPlugin.class);
    private BranchIdModel branchIdModel;

    /**
     * Detaches all models
     */
    @Override
    public void detachModels() {
        super.detachModels();
        if (branchIdModel!=null){
            branchIdModel.detach();
        }
    }

    public AbstractDocumentWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
        try {
            branchIdModel = new BranchIdModel(context, getWorkflow().getNode().getIdentifier());
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
    }

    protected BranchIdModel getBranchIdModel() {
        return branchIdModel;
    }

    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }

    protected JcrNodeModel getFolder() {
        JcrNodeModel folderModel = new JcrNodeModel("/");
        try {
            WorkflowDescriptorModel wdm = getModel();
            if (wdm != null) {
                Node node = wdm.getNode();
                if (node != null) {
                    folderModel = new JcrNodeModel(node.getParent());
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Could not determine folder path", ex);
        }
        return folderModel;
    }

    protected Node getVariant(final Node handle, final WorkflowUtils.Variant variant) throws RepositoryException {
        final Optional<Node> optional = WorkflowUtils.getDocumentVariantNode(handle, variant);
        if(optional.isPresent()) {
            return optional.get();
        }
        throw new ItemNotFoundException("No " + variant + " variant found under path: " + handle.getPath());
    }

    protected IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    protected StringCodec getLocalizeCodec() {
        return CodecUtils.getDisplayNameCodec(getPluginContext());
    }

    protected StringCodec getNodeNameCodec(final Node node) {
        return CodecUtils.getNodeNameCodec(getPluginContext(), node);
    }

    protected void hideOrDisable(Map<String, Serializable> info, String key, StdWorkflow... actions) {
        if (info.containsKey(key)) {
            if (info.get(key) instanceof Boolean && !(Boolean) info.get(key)) {
                for (StdWorkflow action : actions) {
                    action.setEnabled(false);
                }
            }
        } else {
            for (StdWorkflow action : actions) {
                action.setVisible(false);
            }
        }
    }

    protected boolean isActionAllowed(Map<String, Serializable> info, String key) {
        return (info.containsKey(key) && info.get(key) instanceof Boolean && (Boolean) info.get(key));
    }

    protected void hideIfNotAllowed(Map<String, Serializable> info, String key, StdWorkflow... actions) {
        if (!isActionAllowed(info, key)) {
            for (StdWorkflow action : actions) {
                action.setVisible(false);
            }
        }
    }

    /**
     * Use the IBrowseService to select the node referenced by parameter path
     *
     * @param nodeModel Absolute path of node to browse to
     * @throws javax.jcr.RepositoryException
     */
    protected void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        UserSession.get().getJcrSession().refresh(false);

        getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class)
                .browse(nodeModel);
    }

    protected IModel<String> getDocumentName() {
        try {
            final IModel<String> result = DocumentUtils.getDocumentNameModel(((WorkflowDescriptorModel) getDefaultModel()).getNode());
            if (result != null) {
                return result;
            }
        } catch (RepositoryException ignored) {
        }
        return new StringResourceModel("unknown", this);
    }

    protected DocumentWorkflow getWorkflow() {
        return getModel().getWorkflow();
    }

    protected Map<String, Serializable> getHints() {
        String branchId = BranchConstants.MASTER_BRANCH_ID;
        if (branchIdModel != null){
            branchId = branchIdModel.getBranchId();
        }
        log.debug("Get hints for branchId:{}", branchId);
        return getModel().getHints(branchId);

    }

    protected String getBranchId() {
        BranchIdModel branchIdModel = getBranchIdModel();
        return branchIdModel == null ? BranchConstants.MASTER_BRANCH_ID : branchIdModel.getBranchId();
    }
}
