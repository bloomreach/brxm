/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDocumentWorkflowPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(AbstractDocumentWorkflowPlugin.class);

    public AbstractDocumentWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
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
                    folderModel = new JcrNodeModel(node.getParent().getParent());
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Could not determine folder path", ex);
        }
        return folderModel;
    }

    protected IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
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
    }IModel<String> getDocumentName() {
        try {
            return (new NodeTranslator(new JcrNodeModel(((WorkflowDescriptorModel) getDefaultModel()).getNode())))
                    .getNodeName();
        } catch (RepositoryException ex) {
            try {
                return new Model<String>(((WorkflowDescriptorModel) getDefaultModel()).getNode().getName());
            } catch (RepositoryException e) {
                return new StringResourceModel("unknown", this, null);
            }
        }
    }

    protected DocumentWorkflow getWorkflow() {
        WorkflowManager manager = UserSession.get().getWorkflowManager();
        WorkflowDescriptorModel wdm = getModel();
        WorkflowDescriptor workflowDescriptor = wdm.getObject();
        try {
            if (workflowDescriptor != null) {
                return (DocumentWorkflow) manager.getWorkflow(workflowDescriptor);
            }
        } catch (RepositoryException e) {
            log.error("Unable to retrieve workflow", e);
        }
        return null;
    }

    protected Map<String, Serializable> getHints() {
        DocumentWorkflow workflow = getWorkflow();
        if (workflow != null) {
            try {
                return workflow.hints();
            } catch (WorkflowException | RemoteException | RepositoryException e) {
                log.error("Unable to retrieve workflow hints", e);
            }
        }
        return Collections.emptyMap();
    }
}
