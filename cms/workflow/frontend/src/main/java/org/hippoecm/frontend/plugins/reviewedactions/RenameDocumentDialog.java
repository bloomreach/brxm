/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.Locale;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standardworkflow.RenameMessage;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.StringCodec;

public  class RenameDocumentDialog extends AbstractWorkflowDialog<Void> {

    private final IModel<String> title;
    private final IModel<StringCodec> codec;
    private final TextField<String> nameComponent;
    private final TextField<String> uriComponent;
    private boolean uriModified;

    public RenameDocumentDialog(StdWorkflow action, IModel<String> title, IModel<StringCodec> codecModel) {
        super(null, action);

        this.title = title;
        this.codec = codecModel;

        final PropertyModel<String> nameModel = new PropertyModel<>(action, "targetName");
        final PropertyModel<String> uriModel = new PropertyModel<>(action, "uriName");
        final PropertyModel<Map<Localized, String>> localizedNamesModel = new PropertyModel<>(action, "localizedNames");

        String s1 = nameModel.getObject();
        String s2 = uriModel.getObject();
        uriModified = !s1.equals(s2);

        nameComponent = new TextField<>("name", nameModel);
        nameComponent.setRequired(true);
        nameComponent.setLabel(Model.of(getString("name-label")));
        nameComponent.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (!uriModified) {
                    String encoded = codec.getObject().encode(nameModel.getObject());
                    uriModel.setObject(encoded);
                    target.add(uriComponent);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final RuntimeException e) {
                super.onError(target, e);

                //if we get past the super call, RunTimeException e is null and we are dealing with
                //either a validation or a conversion error, so we can/should update the URI component if needed
                String rawInput = nameComponent.getInput();
                if (!uriModified && Strings.isEmpty(rawInput)) {
                    uriModel.setObject("");
                    target.add(uriComponent);
                }
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(nameComponent.getPath(), Duration.milliseconds(500)));
            }
        });
        nameComponent.setOutputMarkupId(true);
        setFocus(nameComponent);
        add(nameComponent);

        add(uriComponent = new TextField<String>("uriinput", uriModel) {
            @Override
            public boolean isEnabled() {
                return uriModified;
            }
        });

        uriComponent.add(CssClass.append(new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return uriModified ? "grayedin" : "grayedout";
            }
        }));
        uriComponent.setOutputMarkupId(true);

        AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                uriModified = !uriModified;
                if (!uriModified) {
                    String uri = Strings.isEmpty(nameModel.getObject()) ? ""
                            : codec.getObject().encode(nameModel.getObject());
                    uriModel.setObject(uri);
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
        final RenameMessage message = new RenameMessage(cmsLocale, localizedNamesModel.getObject());
        if (message.shouldShow()) {
            warn(message.forDocument());
        }
    }

    @Override
    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM;
    }
}