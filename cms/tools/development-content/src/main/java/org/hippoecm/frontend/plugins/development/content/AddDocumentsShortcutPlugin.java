/*
 *  Copyright 2008 Hippo.
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
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.DocumentSettings;
import org.hippoecm.frontend.plugins.development.content.wizard.DevelopmentContentWizard;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDocumentsShortcutPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AddDocumentsShortcutPlugin.class);

    ContentBuilder builder;

    public AddDocumentsShortcutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        builder = new ContentBuilder();

        add(new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(new AddDocumentsShortcutPlugin.Dialog());
            }

        });
    }

    public class Dialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        public Dialog() {
            add(HeaderContributor.forCss(AddDocumentsShortcutPlugin.class, "style.css"));

            setOkVisible(false);
            setCancelVisible(false);

            add(new AddDocumentsWizard("wizard", getPluginContext(), getPluginConfig()));

        }

        public IModel getTitle() {
            return new StringResourceModel("add-content-label", AddDocumentsShortcutPlugin.this, null);
        }

        @Override
        public IValueMap getProperties() {
            return new ValueMap("width=500,height=355");
        }
        
        class AddDocumentsWizard extends DevelopmentContentWizard {
            private static final long serialVersionUID = 1L;
            
            DocumentSettings settings;

            public AddDocumentsWizard(String id, IPluginContext context, IPluginConfig config) {
                super(id, context, config);
            }

            @Override
            protected IDynamicWizardStep createFirstStep() {
                settings = new DocumentSettings();
                
                return new ChooseFolderStep(null, new PropertyModel(settings, "folderUUID")) {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                    protected String getStepTitle() {
                        return new StringResourceModel("wizard.step.1.title", AddDocumentsShortcutPlugin.this, null).getString();
                    }

                    public IDynamicWizardStep next() {
                        return createSecondStep(this);
                    }
                };
            }

            private IDynamicWizardStep createSecondStep(IDynamicWizardStep previousStep) {
                return new SelectTypesStep(previousStep, settings.nodeTypes) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String getStepTitle() {
                        return new StringResourceModel("wizard.step.2.title", AddDocumentsShortcutPlugin.this, null).getString();
                    }

                    public IDynamicWizardStep next() {
                        return createThirdStep(this);
                    }

                    @Override
                    protected List<String> getTypes() {
                        return builder.getDocumentTypes(settings.folderUUID);
                    }

                };
            }

            private IDynamicWizardStep createThirdStep(IDynamicWizardStep previousStep) {
                return new DocumentSettingsStep(previousStep, settings) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String getStepTitle() {
                        return new StringResourceModel("wizard.step.3.title", AddDocumentsShortcutPlugin.this, null).getString();
                    }

                    public IDynamicWizardStep next() {
                        return createFourthStep(this);
                    }

                };
            }

            private IDynamicWizardStep createFourthStep(IDynamicWizardStep previousStep) {
                return new NameSettingsStep(previousStep, settings.naming) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String getStepTitle() {
                        return new StringResourceModel("wizard.step.4.title", AddDocumentsShortcutPlugin.this, null).getString();
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
