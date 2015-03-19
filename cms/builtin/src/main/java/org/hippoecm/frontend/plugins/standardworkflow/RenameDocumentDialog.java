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

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
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
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameDocumentDialog extends AbstractWorkflowDialog<RenameDocumentArguments> {
    private static Logger log = LoggerFactory.getLogger(RenameDocumentDialog.class);

    private final IModel<String> title;
    private final TextField nameComponent;
    private final TextField uriComponent;
    private final String originalUriName;
    private final String originalTargetName;
    private boolean uriModified;
    private final IModel<StringCodec> nodeNameCodecModel;

    public RenameDocumentDialog(RenameDocumentArguments renameDocumentModel, IModel<String> title,
                                IWorkflowInvoker invoker, IModel<StringCodec> nodeNameCodec, final WorkflowDescriptorModel workflowDescriptorModel) {
        super(Model.of(renameDocumentModel), invoker);
        this.title = title;
        this.nodeNameCodecModel = nodeNameCodec;

        final PropertyModel<String> nameModel = new PropertyModel<>(renameDocumentModel, "targetName");
        final PropertyModel<String> uriModel = new PropertyModel<>(renameDocumentModel, "uriName");

        originalUriName = uriModel.getObject();
        originalTargetName = nameModel.getObject();

        uriModified = !StringUtils.equals(originalTargetName, originalUriName);

        nameComponent = new TextField<>("name", nameModel);
        nameComponent.setRequired(true);
        nameComponent.setLabel(Model.of(getString("name-label")));
        nameComponent.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!uriModified) {
                    uriModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
                    target.add(uriComponent);
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(RenameDocumentDialog.this.getPath(), Duration.milliseconds(500)));
            }
        });

        nameComponent.setOutputMarkupId(true);
        setFocus(nameComponent);
        add(nameComponent);

        uriComponent = new TextField<String>("uriinput", uriModel) {
            @Override
            public boolean isEnabled() {
                return uriModified;
            }
        };

        uriComponent.setLabel(Model.of(getString("url-label")));
        add(uriComponent);

        uriComponent.add(CssClass.append(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return uriModified ? "grayedin" : "grayedout";
            }
        }));
        uriComponent.setOutputMarkupId(true);
        uriComponent.setRequired(true);

        AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                uriModified = !uriModified;
                if (!uriModified) {
                    uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                            nameModel.getObject()));
                    uriComponent.modelChanged();
                } else {
                    target.focusComponent(uriComponent);
                }
                target.add(RenameDocumentDialog.this);
            }
        };
        uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return uriModified ? getString("url-reset") : getString("url-edit");
            }
        }));
        add(uriAction);

        final Locale cmsLocale = UserSession.get().getLocale();
        final RenameMessage message = new RenameMessage(cmsLocale, renameDocumentModel.getLocalizedNames());
        if (message.shouldShow()) {
            warn(message.forFolder());
        }

        add(new RenameDocumentValidator(uriComponent, nameComponent, workflowDescriptorModel));
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }

    protected StringCodec getNodeNameCodec() {
        return nodeNameCodecModel.getObject();
    }

    @Override
    protected void onDetach() {
        nodeNameCodecModel.detach();
        super.onDetach();
    }

    private class RenameDocumentValidator extends DocumentFormValidator {
        private final TextField uriComponent;
        private final TextField nameComponent;
        private final WorkflowDescriptorModel workflowDescriptorModel;

        public RenameDocumentValidator(final TextField uriComponent, final TextField nameComponent, WorkflowDescriptorModel workflowDescriptorModel) {
            this.uriComponent = uriComponent;
            this.nameComponent = nameComponent;
            this.workflowDescriptorModel = workflowDescriptorModel;
        }

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent<?>[]{uriComponent, nameComponent};
        }

        @Override
        public void validate(final Form<?> form) {
            String newUriName = uriComponent.getValue().toLowerCase();
            String newLocalizedName = nameComponent.getValue();
            try {
                final Node parentNode = workflowDescriptorModel.getNode().getParent();

                if (StringUtils.equals(newUriName, originalUriName)) {
                    if (StringUtils.equals(newLocalizedName, originalTargetName)) {
                        error(getString("error-same-names"));
                    } else if (existedLocalizedName(parentNode, newLocalizedName)) {
                        error(new StringResourceModel("error-localized-name-exists", RenameDocumentDialog.this,
                                null, new Object[]{newLocalizedName}).getObject());
                    }
                } else if (parentNode.hasNode(newUriName)) {
                    error(new StringResourceModel("error-sns-node-exists",
                            RenameDocumentDialog.this, null, new Object[]{newUriName}).getObject());
                } else if (!StringUtils.equals(newLocalizedName, originalTargetName) &&
                            existedLocalizedName(parentNode, newLocalizedName)) {
                    error(new StringResourceModel("error-localized-name-exists", RenameDocumentDialog.this,
                            null, new Object[]{newLocalizedName}).getObject());
                }
            } catch (RepositoryException e) {
                log.error("validation error", e);
                error(getString("error-validation-names"));
            }
        }
    }
}
