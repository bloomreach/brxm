/*
 *  Copyright 2008 Hippo.
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
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadWizard extends Wizard {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UploadWizard.class);

    private UploadForm form;
    private IDialogService dialogService;
    private UploadDialog uploadDialog;

    public UploadWizard(String id, UploadDialog uploadDialog) {
        super(id, false);
        setOutputMarkupId(true);
        this.uploadDialog = uploadDialog;
        WizardModel wizardModel = new WizardModel();
        if(uploadDialog.getGalleryNode() == null) {
            wizardModel.add(new Step0());
        }
        WizardStep step1;
        wizardModel.add(step1 = new Step1());
        wizardModel.add(new Step2());
        wizardModel.setActiveStep(step1);
        init(wizardModel);
    }

    void setDialogService(IDialogService dialogService) {
        this.dialogService = dialogService;
    }

    @Override
    public void onCancel() {
        onFinish();
    }

    @Override
    public void onFinish() {
        dialogService.close();
        try {
            JcrNodeModel gallery = new JcrNodeModel(uploadDialog.getGalleryNode());
            gallery.getNode().getSession().refresh(true);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected Component newButtonBar(String id) {
        return new ButtonBar(id, this);
    }

    @SuppressWarnings("unchecked")
    protected IModel<Node> getModel() {
        return (IModel<Node>) getDefaultModel();
    }
    
    private class Step0 extends WizardStep {
        private static final long serialVersionUID = 1L;

        public Step0() {
            super();
            IPluginConfigService pluginConfigService = uploadDialog.pluginContext.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
            IClusterConfig cluster = pluginConfigService.getCluster("cms-pickers/folders");
            IClusterControl control = uploadDialog.pluginContext.newCluster(cluster, uploadDialog.pluginConfig.getPluginConfig("cluster.options"));
            IClusterConfig decorated = control.getClusterConfig();
            String modelServiceId = decorated.getString("model.folder");
            ModelReference<Node> modelService = new ModelReference<Node>(modelServiceId, UploadWizard.this.getModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void setModel(IModel<Node> model) {
                    if (model != null && model instanceof JcrNodeModel && ((JcrNodeModel)model).getNode() != null) {
                        Step0.this.setComplete(true);
                        uploadDialog.setGalleryNode((Node) model.getObject());
                    } else {
                        Step0.this.setComplete(false);
                    }
                    super.setModel(model);
                }
            };
            modelService.init(uploadDialog.pluginContext);

            control.start();

            IRenderService renderer = uploadDialog.pluginContext.getService(decorated.getString("wicket.id"), IRenderService.class);
            renderer.bind(null, "picker");
            add(renderer.getComponent());
        }
    }

    private class Step1 extends WizardStep {
        private static final long serialVersionUID = 1L;

        public Step1() {
            super();
            form = new UploadForm("form", uploadDialog);
            add(form);
        }
    }

    private class Step2 extends WizardStep {
        private static final long serialVersionUID = 1L;

        public Step2() {
            super();
            add(new Label("status", new StringResourceModel("upload-successful-label", this, null)));
            add(new MultiLineLabel("description", new PropertyModel<String>(form, "description")));
            add(new Label("message", new StringResourceModel("upload-another-label", this, null)));
        }
    }
}
