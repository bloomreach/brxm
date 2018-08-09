/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.i18n.types.SortedTypeChoiceRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standardworkflow.components.LanguageField;
import org.hippoecm.frontend.plugins.standardworkflow.validators.AddDocumentValidator;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.widgets.NameUriField;
import org.hippoecm.repository.api.StringCodec;

public class AddDocumentDialog extends WorkflowDialog<AddDocumentArguments> {

    private final NameUriField nameUriContainer;

    private IModel<String> title;
    private LanguageField languageField;
    private final IModel<StringCodec> nodeNameCodecModel;

    public AddDocumentDialog(AddDocumentArguments addDocumentModel, IModel<String> title, String category,
                             Set<String> prototypes, boolean translated, final IWorkflowInvoker invoker,
                             IModel<StringCodec> nodeNameCodec, ILocaleProvider localeProvider, final WorkflowDescriptorModel workflowDescriptorModel) {
        super(invoker, Model.of(addDocumentModel));

        this.title = title;
        this.nodeNameCodecModel = nodeNameCodec;

        final PropertyModel<String> prototypeModel = new PropertyModel<>(addDocumentModel, "prototype");

        add(nameUriContainer = new NameUriField("name-url", this.nodeNameCodecModel));

        // The dialog produces ajax requests in NameUriField and OK/Cancel dialog buttons, which may cause Wicket
        // exceptions when typing very fast. Thus it needs to use a dedicated ajax channel with ACTIVE behavior when
        // some AJAX requests may be sent after dialog is closed.
        final AjaxChannel activeAjaxChannel = new AjaxChannel(getMarkupId(), AjaxChannel.Type.ACTIVE);
        setAjaxChannel(activeAjaxChannel);
        nameUriContainer.setAjaxChannel(activeAjaxChannel);

        final IModel<String> documentType = new StringResourceModel("document-type", this);
        final Label typeLabel = new Label("typelabel", documentType);
        add(typeLabel);

        if (prototypes.size() > 1) {
            final List<String> prototypesList = new LinkedList<>(prototypes);
            final DropDownChoice<String> folderChoice;
            SortedTypeChoiceRenderer typeChoiceRenderer = new SortedTypeChoiceRenderer(this, prototypesList);
            add(folderChoice = new DropDownChoice<>("prototype", prototypeModel, typeChoiceRenderer, typeChoiceRenderer));
            folderChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
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

        add(new AddDocumentValidator(this, nameUriContainer, workflowDescriptorModel));

        add(CssClass.append("add-document-dialog"));

        setSize(DialogConstants.MEDIUM_AUTO);
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

    public LanguageField getLanguageField() {
        return languageField;
    }

    @Override
    protected void onDetach() {
        nodeNameCodecModel.detach();
        super.onDetach();
    }

    //TODO: This override method should be moved to the ancestor class in CMS7-9429
    @Override
    protected FeedbackPanel newFeedbackPanel(String id) {
        return new FeedbackPanel(id);
    }
}
