/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelWizard extends Wizard {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RemodelWizard.class);

    private IDialogService dialogService;

    public RemodelWizard(String id) {
        super(id);
        setOutputMarkupId(true);
        WizardModel model = new WizardModel();
        model.add(new Step1());
        model.add(new Step2());
        init(model);
    }

    void setDialogService(IDialogService service) {
        this.dialogService = service;
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

    private static final class Step1 extends WizardStep {
        private static final long serialVersionUID = 1L;

    }

    private static final class Step2 extends WizardStep {
        private static final long serialVersionUID = 1L;

        public Step2() {
            add(new AjaxLazyLoadPanel("progress") {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getLazyLoadComponent(String id) {
                    Label result;
                    try {
                        RemodelDialog dialog = (RemodelDialog) findParent(RemodelDialog.class);
                        dialog.remodel();
                        result = new Label(id, new StringResourceModel("success", this));
                    } catch (Exception e) {
                        result = new Label(id, new StringResourceModel("failed", this));
                        log.error("Error during workflow execution", e);
                        error(e);
                    }
                    return result;
                }
            });
        }

        @Override
        protected void onBeforeRender() {
            super.onBeforeRender();
        }
    }

    private static final class ButtonBar extends Panel {
        private static final long serialVersionUID = 1L;

        public ButtonBar(String id, RemodelWizard wizard) {
            super(id);
            add(new NextLink("yes", wizard));
            add(new CancelLink("cancel", wizard));
            add(new FinishLink("finish", wizard));
        }
    }

}
