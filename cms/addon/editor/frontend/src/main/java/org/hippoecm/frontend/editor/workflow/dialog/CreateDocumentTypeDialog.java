/*
 *  Copyright 2009 Hippo.
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

import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.editor.tools.JcrTypeStore;
import org.hippoecm.frontend.editor.impl.JcrTemplateStore;
import org.hippoecm.frontend.editor.layout.ILayoutProvider;
import org.hippoecm.frontend.editor.workflow.NamespaceValidator;
import org.hippoecm.frontend.editor.workflow.action.NewDocumentTypeAction;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class CreateDocumentTypeDialog extends CreateTypeDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    class TypeDetailStep extends WizardStep {
        private static final long serialVersionUID = 1L;

        TextFieldWidget nameComponent;

        TypeDetailStep(NewDocumentTypeAction action) {
            super(new ResourceModel("type-detail-title"), new ResourceModel("type-detail-summary"));
            add(nameComponent = new TextFieldWidget("name", new PropertyModel(action, "name")));

            CheckGroup cg = new CheckGroup("checkgroup", new PropertyModel(action, "mixins"));
            add(cg);

            JcrTemplateStore templateStore = new JcrTemplateStore(new JcrTypeStore());

            cg.add(new DataView("mixins", new ListDataProvider(templateStore.getAvailableMixins())) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item item) {
                    String mixin = item.getModelObjectAsString();
                    item.add(new Check("check", item.getModel()));
                    IModel typeName = new TypeTranslator(new JcrNodeTypeModel(mixin)).getTypeName();
                    item.add(new Label("mixin", typeName));
                }

            });
        }
        
        @Override
        public void applyState() {
            try {
                // NamespaceValidator only allows programming by exception
                NamespaceValidator.checkName((String) ((PropertyModel) nameComponent.getModel()).getObject());
                setComplete(true);
            } catch (Exception ex) {
                setComplete(false);
            }
        }
    }

    public CreateDocumentTypeDialog(NewDocumentTypeAction action, ILayoutProvider layouts) {
        super(action, layouts);

        WizardModel wizardModel = new WizardModel() {
            @Override
            public boolean isNextAvailable() {
                return !isLastStep(getActiveStep());
            }
        };
        wizardModel.add(new TypeDetailStep(action));
        wizardModel.add(new SelectLayoutStep(new PropertyModel(action, "layout"), layouts));
        init(wizardModel);
    }

    public IModel getTitle() {
        return new StringResourceModel("new-document-type", this, null);
    }

}
