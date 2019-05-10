/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.editor.NamespaceValidator;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.action.NewDocumentTypeAction;
import org.hippoecm.frontend.widgets.RequiredTextFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class CreateDocumentTypeDialog extends CreateTypeDialog {

    class TypeDetailStep extends WizardStep {

        private TextFieldWidget nameComponent;

        TypeDetailStep(NewDocumentTypeAction action, final List<String> candidatesSuperTypes) {
            super(new ResourceModel("type-detail-title"), null);

            final DropDownChoice<String> superTypeChoice = new DropDownChoice<>("super-type-list",
                    new PropertyModel<>(action, "superType"), candidatesSuperTypes);
            add(superTypeChoice);

            IModel<String> nameLabel = new ResourceModel("name-caption");
            nameComponent = new RequiredTextFieldWidget("name", PropertyModel.of(action, "name"), nameLabel);
            nameComponent.getFormComponent().add(NamespaceValidator.createNameValidator());
            add(nameComponent);

            setFocus(nameComponent.getFormComponent());
        }

        @Override
        public void applyState() {
            setComplete(nameComponent.getFormComponent().isValid());
        }
    }

    public CreateDocumentTypeDialog(NewDocumentTypeAction action, ILayoutProvider layouts) {
        super(action);

        setTitleKey("new-document-type");

        WizardModel wizardModel = new WizardModel() {
            @Override
            public boolean isNextAvailable() {
                return !isLastStep(getActiveStep());
            }
        };
        wizardModel.add(new TypeDetailStep(action, action.documentTypes));
        wizardModel.add(new SelectLayoutStep(PropertyModel.of(action, "layout"), layouts));
        init(wizardModel);
    }
}
