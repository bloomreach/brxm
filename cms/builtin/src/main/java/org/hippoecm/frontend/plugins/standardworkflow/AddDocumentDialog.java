/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.i18n.types.SortedTypeChoiceRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standardworkflow.components.LanguageField;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.repository.api.StringCodec;

public class AddDocumentDialog extends AbstractWorkflowDialog<AddDocumentArguments> {
    private IModel title;
    private TextField nameComponent;
    private TextField uriComponent;
    private boolean uriModified = false;
    private LanguageField languageField;
    private final IModel<StringCodec> nodeNameCodecModel;

    public AddDocumentDialog(AddDocumentArguments addDocumentModel, IModel title, String category, Set<String> prototypes, boolean translated, final IWorkflowInvoker invoker, IModel<StringCodec> nodeNameCodec, ILocaleProvider localeProvider) {
        super(Model.of(addDocumentModel), invoker);
        this.title = title;
        this.nodeNameCodecModel = nodeNameCodec;

        final PropertyModel<String> nameModel = new PropertyModel<String>(addDocumentModel, "targetName");
        final PropertyModel<String> uriModel = new PropertyModel<String>(addDocumentModel, "uriName");
        final PropertyModel<String> prototypeModel = new PropertyModel<String>(addDocumentModel, "prototype");

        nameComponent = new TextField<String>("name", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            public String getObject() {
                return nameModel.getObject();
            }

            public void setObject(String object) {
                nameModel.setObject(object);
                if (!uriModified) {
                    uriModel.setObject(getNodeNameCoded().encode(nameModel.getObject()));
                }
            }

            public void detach() {
                nameModel.detach();
            }

        });
        nameComponent.setRequired(true);
        nameComponent.setLabel(new StringResourceModel("name-label", this, null));
        nameComponent.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!uriModified) {
                    target.add(uriComponent);
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(AddDocumentDialog.this.getPath(), Duration.milliseconds(500)));
            }
        });
        nameComponent.setOutputMarkupId(true);
        setFocus(nameComponent);
        add(nameComponent);

        final Label typelabel;
        add(typelabel = new Label("typelabel", new StringResourceModel("document-type", this, null)));

        if (prototypes.size() > 1) {
            final List<String> prototypesList = new LinkedList<String>(prototypes);
            final DropDownChoice<String> folderChoice;
            SortedTypeChoiceRenderer typeChoiceRenderer = new SortedTypeChoiceRenderer(this, prototypesList);
            add(folderChoice = new DropDownChoice<String>("prototype", prototypeModel, typeChoiceRenderer, typeChoiceRenderer));
            folderChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(folderChoice);
                }
            });
            folderChoice.setNullValid(false);
            folderChoice.setRequired(true);
            folderChoice.setLabel(new StringResourceModel("document-type", this, null));
            // while not a prototype chosen, disable ok button
            Component notypes;
            add(notypes = new EmptyPanel("notypes"));
            notypes.setVisible(false);
        } else if (prototypes.size() == 1) {
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototypeModel.setObject(prototypes.iterator().next());
            Component notypes;
            add(notypes = new EmptyPanel("notypes"));
            notypes.setVisible(false);
            typelabel.setVisible(false);
        } else {
            // if the folderWorkflowPlugin.templates.get(category).size() = 0 you cannot add this
            // category currently.
            Component component;
            add(component = new EmptyPanel("prototype"));
            component.setVisible(false);
            prototypeModel.setObject(null);
            add(new Label("notypes", "There are no types available for : [" + category
                    + "] First create document types please."));
            nameComponent.setVisible(false);
            typelabel.setVisible(false);
        }

        add(uriComponent = new TextField<String>("uriinput", uriModel) {
            @Override
            public boolean isEnabled() {
                return uriModified;
            }
        });

        uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return uriModified ? "grayedin" : "grayedout";
            }
        }));
        uriComponent.setRequired(true);
        uriComponent.setLabel(new StringResourceModel("url-label", this, null));
        uriComponent.setOutputMarkupId(true);

        AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                uriModified = !uriModified;
                if (!uriModified) {
                    uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCoded().encode(
                            nameModel.getObject()));
                    uriComponent.modelChanged();
                } else {
                    target.focusComponent(uriComponent);
                }
                target.add(AddDocumentDialog.this);
            }
        };
        uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return uriModified ? getString("url-reset") : getString("url-edit");
            }
        }));
        add(uriAction);

        languageField = new LanguageField("language", new PropertyModel<String>(addDocumentModel, "language"), localeProvider);
        if (!translated) {
            languageField.setVisible(false);
        }
        add(languageField);
    }

    @Override
    public IModel getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return MEDIUM;
    }

    public LanguageField getLanguageField() {
        return languageField;
    }

    public TextField getUriComponent() {
        return uriComponent;
    }

    public TextField getNameComponent() {
        return nameComponent;
    }

    protected StringCodec getNodeNameCoded() {
        return nodeNameCodecModel.getObject();
    }

    @Override
    protected void onDetach() {
        nodeNameCodecModel.detach();
        super.onDetach();
    }
}
