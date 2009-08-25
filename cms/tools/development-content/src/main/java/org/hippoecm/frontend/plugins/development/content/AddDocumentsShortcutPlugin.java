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

import java.util.Collection;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.NameSettings;
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

            setOkLabel(new StringResourceModel("start-add-content-label", AddDocumentsShortcutPlugin.this, null));

            add(new DevelopmentContentWizard("wizard", getPluginContext(), getPluginConfig()) {
                private static final long serialVersionUID = 1L;

                String folderUUID;
                NameSettings nameSettings = new NameSettings();
                SelectedTypesSettings typesSettings = new SelectedTypesSettings();        
                
                @Override
                protected IDynamicWizardStep createFirstStep() {
                    IModel folderModel = new PropertyModel(this, "folderUUID");
                    
                    return new ChooseFolderStep(null, folderModel) {
                        private static final long serialVersionUID = 1L;
                        
                        public IDynamicWizardStep next() {
                            return createSecondStep(this);
                        }
                    };
                }
                
                private IDynamicWizardStep createSecondStep(IDynamicWizardStep previousStep) {
                    return new SelectTypesStep(previousStep, typesSettings) {
                        private static final long serialVersionUID = 1L;

                        public IDynamicWizardStep next() {
                            return createThirdStep(this);
                        }

                        @Override
                        protected Collection<String> getTypes() {
                            return builder.getDocumentTypes(folderUUID);
                        }
                        
                    };
                }
                
                private IDynamicWizardStep createThirdStep(IDynamicWizardStep previousStep) {
                    return new NameSettingsStep(previousStep, nameSettings) {
                        private static final long serialVersionUID = 1L;
                        
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
                    String folderPath = builder.uuid2path(folderUUID);
                    if (typesSettings.isRandom()) {
                        builder.createRandomDocuments(folderPath, nameSettings);
                    } else {
                        builder.createDocuments(folderPath, typesSettings.getSelectedTypes(), nameSettings);
                    }
                    closeDialog();
                }

            });

        }

        public IModel getTitle() {
            return new StringResourceModel("add-content-label", AddDocumentsShortcutPlugin.this, null);
        }
    }

}
