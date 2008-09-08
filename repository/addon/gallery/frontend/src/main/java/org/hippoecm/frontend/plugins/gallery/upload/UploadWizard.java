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

import org.apache.wicket.Component;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.dialog.IDialogService;

public class UploadWizard extends Wizard {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

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
        dialogService.close();
    }

    @Override
    public void onFinish() {
        dialogService.close();
    }

    @Override
    protected Component newButtonBar(String id) {
        return new ButtonBar(id, this);
    }

    private class Step1 extends WizardStep {
        private static final long serialVersionUID = 1L;

        public Step1() {
            super();
            add(new UploadForm("form", uploadDialog));
        }
    }

    private class Step2 extends WizardStep {
        private static final long serialVersionUID = 1L;

        public Step2() {
            super();
            add(new Label("message", "Do you want to upload another file?"));
        }
    }

}
