/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standardworkflow;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialogRestyling;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.i18n.types.SortedTypeChoiceRenderer;
import org.hippoecm.frontend.plugins.standardworkflow.components.LanguageField;
import org.hippoecm.frontend.plugins.standardworkflow.validators.AddDocumentValidator;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.repository.api.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDocumentDialog extends AbstractWorkflowDialogRestyling<AddDocumentArguments> {
    private static Logger log = LoggerFactory.getLogger(AddDocumentDialog.class);

    private final NameUriField nameUriContainer;

    private IModel<String> title;
    private LanguageField languageField;
    private final IModel<StringCodec> nodeNameCodecModel;

    public AddDocumentDialog(AddDocumentArguments addDocumentModel, IModel<String> title, String category,
                             Set<String> prototypes, boolean translated, final IWorkflowInvoker invoker,
                             IModel<StringCodec> nodeNameCodec, ILocaleProvider localeProvider, final WorkflowDescriptorModel workflowDescriptorModel) {
        super(Model.of(addDocumentModel), invoker);
        this.title = title;
        this.nodeNameCodecModel = nodeNameCodec;

        final PropertyModel<String> prototypeModel = new PropertyModel<>(addDocumentModel, "prototype");

        add(nameUriContainer = new NameUriField("name-url", this.nodeNameCodecModel));

        final IModel<String> documentType = new StringResourceModel("document-type", this, null);
        final Label typeLabel = new Label("typelabel", documentType);
        add(typeLabel);

        if (prototypes.size() > 1) {
            final List<String> prototypesList = new LinkedList<>(prototypes);
            final DropDownChoice<String> folderChoice;
            SortedTypeChoiceRenderer typeChoiceRenderer = new SortedTypeChoiceRenderer(this, prototypesList);
            add(folderChoice = new DropDownChoice<>("prototype", prototypeModel, typeChoiceRenderer, typeChoiceRenderer));
            folderChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(folderChoice);
                }
            });
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);
            folderChoice.setLabel(documentType);

            // while not a prototype chosen, disable ok button
            add(new EmptyPanel("notypes").setVisible(false));
        } else if (prototypes.size() == 1) {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototypeModel.setObject(prototypes.iterator().next());
            add(new EmptyPanel("notypes").setVisible(false));
            typeLabel.setVisible(false);
        } else {
            // if the folderWorkflowPlugin.templates.get(category).size() = 0 you cannot add this
            // category currently.
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototypeModel.setObject(null);
            add(new Label("notypes", "There are no types available for : [" + category
                    + "] First create document types please."));
            nameUriContainer.getNameComponent().setVisible(false);
            typeLabel.setVisible(false);
        }

        languageField = new LanguageField("language", new PropertyModel<>(addDocumentModel, "language"), localeProvider);
        if (!translated) {
            languageField.setVisible(false);
        }
        add(languageField);

        add(new AddDocumentValidator(nameUriContainer, workflowDescriptorModel) {
            @Override
            protected void showError(final String key, final Object... parameters) {
                error(new StringResourceModel(key, AddDocumentDialog.this, null, parameters).getObject());
            }
        });
    }

    @Override
    protected void onOk() {
        AddDocumentArguments addDocumentArguments = getModel().getObject();
        addDocumentArguments.setUriName(nameUriContainer.getUrl());
        addDocumentArguments.setTargetName(nameUriContainer.getName());
        super.onOk();
    }
    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM_AUTO;
    }

    public LanguageField getLanguageField() {
        return languageField;
    }

    @Override
    protected void onDetach() {
        nodeNameCodecModel.detach();
        super.onDetach();
    }
}
