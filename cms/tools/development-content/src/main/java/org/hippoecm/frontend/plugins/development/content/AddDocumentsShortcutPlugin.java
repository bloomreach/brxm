/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.development.content;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.DocumentSettings;
import org.hippoecm.frontend.plugins.development.content.wizard.DevelopmentContentWizard;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.skin.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDocumentsShortcutPlugin extends RenderPlugin {

    static final Logger log = LoggerFactory.getLogger(AddDocumentsShortcutPlugin.class);

    private static final ResourceReference STYLE_CSS = new CssResourceReference(AddDocumentsShortcutPlugin.class, "style.css");

    ContentBuilder builder;

    public AddDocumentsShortcutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        builder = new ContentBuilder();

        final AjaxLink link = new AjaxLink("link") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(new Dialog());
            }
        };
        add(link);
        link.add(HippoIcon.fromSprite("icon", Icon.PLUS));
    }

    private String translate(final String key) {
        return getString(key);
    }

    public class Dialog extends AbstractDialog {

        public Dialog() {
            setOkVisible(false);
            setCancelVisible(false);

            add(CssClass.append("add-documents-dialog"));
            add(new AddDocumentsWizard("wizard", getPluginContext(), getPluginConfig()));
        }

        @Override
        public void renderHead(final IHeaderResponse response) {
            super.renderHead(response);
            response.render(CssHeaderItem.forReference(STYLE_CSS));
        }

        @Override
        public IModel<String> getTitle() {
            return Model.of(translate("add-content-label"));
        }

        @Override
        public IValueMap getProperties() {
            return new ValueMap("width=500,height=355");
        }

        class AddDocumentsWizard extends DevelopmentContentWizard {

            DocumentSettings settings;

            public AddDocumentsWizard(String id, IPluginContext context, IPluginConfig config) {
                super(id, context, config);
            }

            @Override
            protected IDynamicWizardStep createFirstStep() {
                settings = new DocumentSettings();

                return new ChooseFolderStep(null, PropertyModel.of(settings, "folderUUID")) {

                    @Override
                    protected String getStepTitle() {
                        return translate("wizard.step.1.title");
                    }

                    public IDynamicWizardStep next() {
                        return createSecondStep(this);
                    }

                    @Override
                    public boolean isNextAvailable() {
                        return super.isNextAvailable() && builder.getDocumentTypes(settings.folderUUID).size() > 0;
                    }
                };
            }

            private IDynamicWizardStep createSecondStep(IDynamicWizardStep previousStep) {
                return new SelectTypesStep(previousStep, settings.nodeTypes) {

                    @Override
                    protected String getStepTitle() {
                        return translate("wizard.step.2.title");
                    }

                    public IDynamicWizardStep next() {
                        return createThirdStep(this);
                    }

                    @Override
                    protected List<ContentBuilder.CategoryType> getTypes() {
                        return builder.getDocumentTypes(settings.folderUUID);
                    }
                };
            }

            private IDynamicWizardStep createThirdStep(IDynamicWizardStep previousStep) {
                return new DocumentSettingsStep(previousStep, settings) {

                    @Override
                    protected String getStepTitle() {
                        return translate("wizard.step.3.title");
                    }

                    public IDynamicWizardStep next() {
                        return createFourthStep(this);
                    }
                };
            }

            private IDynamicWizardStep createFourthStep(IDynamicWizardStep previousStep) {
                return new NameSettingsStep(previousStep, settings.naming) {

                    @Override
                    protected String getStepTitle() {
                        return translate("wizard.step.4.title");
                    }

                    public boolean isLastStep() {
                        return true;
                    }

                    public IDynamicWizardStep next() {
                        return null;
                    }
                };
            }

            @Override
            public void onFinish() {
                builder.createDocuments(settings);
                closeDialog();
            }

            @Override
            public void onCancel() {
                closeDialog();
            }
        }
    }
}
