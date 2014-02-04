/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standardworkflow.RenameMessage;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;

public  class RenameDocumentDialog extends AbstractWorkflowDialog<Void> {
        private IModel<String> title;
        private TextField nameComponent;
        private TextField uriComponent;
        private boolean uriModified;
        private IPluginContext context;

        public RenameDocumentDialog(StdWorkflow action, IModel<String> title, IPluginContext context) {
            super(null, action);

            this.title = title;
            this.context = context;

            final PropertyModel<String> nameModel = new PropertyModel<>(action, "targetName");
            final PropertyModel<String> uriModel = new PropertyModel<>(action, "uriName");
            final PropertyModel<Map<Localized, String>> localizedNamesModel = new PropertyModel<>(action, "localizedNames");

            String s1 = nameModel.getObject();
            String s2 = uriModel.getObject();
            uriModified = !s1.equals(s2);

            nameComponent = new TextField<>("name", nameModel);
            nameComponent.setRequired(true);
            nameComponent.setLabel(new StringResourceModel("name-label", this, null));
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

            uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
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
                        uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                                nameModel.getObject()));
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

        protected StringCodec getNodeNameCodec() {
            ISettingsService settingsService = context.getService(ISettingsService.SERVICE_ID,
                    ISettingsService.class);
            StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
            return stringCodecFactory.getStringCodec("encoding.node");
        }

        @Override
        public IModel<String> getTitle() {
            return title;
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }
    }