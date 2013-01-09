/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.upload;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(UploadDialog.class);

    UploadWizard wizard;
    IPluginConfig pluginConfig;
    IPluginContext pluginContext;

    public UploadDialog(IPluginContext context, IPluginConfig config) {
        this(context, config, null);
    }

    public UploadDialog(IPluginContext context, IPluginConfig config, IModel model) {
        super(model);
        setOkVisible(false);
        setCancelVisible(false);
        pluginConfig = config;
        pluginContext = context;
        add(wizard = new UploadWizard("wizard", this));
    }

    @Override
    public void setDialogService(IDialogService dialogService) {
        super.setDialogService(dialogService);
        wizard.setDialogService(dialogService);
    }

    String getWorkflowCategory() {
        String workflowCats = pluginConfig.getString("workflow.categories");
        return workflowCats;
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = pluginContext
                .getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = pluginContext
                .getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    IWizardModel getWizardModel() {
        return wizard.getWizardModel();
    }

    public IModel getTitle() {
        return new StringResourceModel(pluginConfig.getString("option.text", ""), this, null);
    }

    void setGalleryNode(Node node) {
        setModel(new JcrNodeModel(node));
    }

    Node getGalleryNode() {
        Object modelObject = getModelObject();
        Node node = null;
        try {
            if (modelObject != null && modelObject instanceof Node) {
                WorkflowManager manager = UserSession.get().getWorkflowManager();
                Workflow workflow = manager.getWorkflow(getWorkflowCategory(), (Node) modelObject);
                if (workflow instanceof GalleryWorkflow) {
                    return (Node) modelObject;
                }
            }
            String location = pluginConfig.getString("option.location");
            if (location != null) {
                while (location.startsWith("/")) {
                    location = location.substring(1);
                }
                javax.jcr.Session jcrSession = UserSession.get().getJcrSession();
                if (jcrSession.getRootNode().hasNode(location)) {
                    node = jcrSession.getRootNode().getNode(location);
                }
            }
        } catch (PathNotFoundException e) {
            log.error("Cannot locate default upload directory " + e.getMessage());
        } catch (RepositoryException e) {
            log.error("Error while accessing upload directory " + e.getMessage());
        }
        return node;
    }
}
