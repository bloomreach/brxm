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

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadWizard extends Wizard {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UploadWizard.class);

    private UploadForm form;
    private IDialogService dialogService;
    private UploadDialog uploadDialog;

    public UploadWizard(String id, IDialogService dialogService, UploadDialog uploadDialog) {
        super(id, false);
        setOutputMarkupId(true);
        this.dialogService = dialogService;
        this.uploadDialog = uploadDialog;
        WizardModel wizardModel = new WizardModel();
        wizardModel.add(new Step1());
        wizardModel.add(new Step2());
        init(wizardModel);
    }

    @Override
    public void onCancel() {
        onFinish();
    }

    @Override
    public void onFinish() {
        dialogService.close();
        try {
            JcrNodeModel gallery = (JcrNodeModel)uploadDialog.getModel();
            gallery.getNode().getSession().refresh(true);
            uploadDialog.getJcrService().flush(gallery);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    protected Component newButtonBar(String id) {
        return new ButtonBar(id, this);
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
            add(new MultiLineLabel("description", new PropertyModel(form, "description")));
            add(new Label("message", new StringResourceModel("upload-another-label", this, null)));
        }
    }

}
