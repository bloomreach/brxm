/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.development.content.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardModel;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardStep;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.DocumentSettings;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.FolderSettings;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.NameSettings;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.NodeTypeSettings;
import org.hippoecm.frontend.plugins.standards.wizard.AjaxWizard;
import org.hippoecm.frontend.plugins.yui.tree.YuiJcrTree;

import java.util.Collection;
import java.util.List;

public abstract class DevelopmentContentWizard extends AjaxWizard {
    private static final long serialVersionUID = 1L;


    IPluginConfig config;
    IPluginContext context;

    public DevelopmentContentWizard(String id, IPluginContext context, IPluginConfig config) {
        super(id, false);

        this.context = context;
        this.config = config;

        init(new DynamicWizardModel(createFirstStep()));

    }

    protected abstract IDynamicWizardStep createFirstStep();

    protected abstract class Step extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        public Step(IDynamicWizardStep previousStep) {
            super(previousStep);

            String title = getStepTitle();
            if (title != null) {
                setTitleModel(new Model<String>(title));

            }

            String sum = getStepSummary();
            if (sum != null) {
                setSummaryModel(new Model<String>(sum));

            }
        }

        protected String getStepTitle() {
            return null;
        }

        protected String getStepSummary() {
            return null;
        }

        public boolean isLastStep() {
            return false;
        }
    }

    protected abstract class ChooseFolderStep extends Step {
        private static final long serialVersionUID = 1L;

        IModel<String> model;

        public ChooseFolderStep(IDynamicWizardStep previousStep, IModel<String> model) {
            super(previousStep);

            this.model = model;

            add(new YuiJcrTree("mytree", config, model) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onClick(AjaxRequestTarget target, String uuid) {
                    ChooseFolderStep.this.model.setObject(uuid);
                    ChooseFolderStep.this.onClick(target, uuid);
                }

                @Override
                protected void onDblClick(AjaxRequestTarget target, String uuid) {
                    ChooseFolderStep.this.onDblClick(target, uuid);
                }
            });
        }

        protected void onDblClick(AjaxRequestTarget target, String uuid) {
        }

        protected void onClick(AjaxRequestTarget target, String uuid) {
        }

        public boolean isLastStep() {
            return false;
        }
    }

    protected abstract class SelectTypesStep extends Step {
        private static final long serialVersionUID = 1L;

        NodeTypeSettings settings;

        public SelectTypesStep(IDynamicWizardStep previousStep, final NodeTypeSettings settings) {
            super(previousStep);
            this.settings = settings;

            final WebMarkupContainer container = new WebMarkupContainer("typesContainer");
            container.setOutputMarkupId(true);
            add(container);

            CheckGroup group = new CheckGroup<String>("typesGroup", new PropertyModel<Collection<String>>(settings, "types")) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return !settings.isRandom();
                }

            };
            container.add(group);

            //group.add(new CheckGroupSelector("groupselector"));
            final ListView<String> typesListView = new ListView<String>("types", getTypesModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem item) {
                    IModel m = item.getModel();
                    item.add(new Check("check", m));
                    item.add(new Label("name", m));
                }
            };
            group.add(typesListView);

            AjaxCheckBox randomDocs = new AjaxCheckBox("randomDocs", new PropertyModel(settings, "random")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(container);
                }
            };

            randomDocs.setOutputMarkupId(true);
            add(randomDocs);
        }

        public boolean isLastStep() {
            return false;
        }

        private IModel<List<String>> getTypesModel() {
            return new AbstractReadOnlyModel<List<String>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public List<String> getObject() {
                    return getTypes();
                }
            };
        }

        protected abstract List<String> getTypes();
    }

    protected abstract class NameSettingsStep extends Step {
        private static final long serialVersionUID = 1L;

        public NameSettingsStep(IDynamicWizardStep previousStep, NameSettings nameSettings) {
            super(previousStep);

            RequiredTextField<Integer> tf;
            add(tf = new RequiredTextField<Integer>("minLength", new PropertyModel<Integer>(nameSettings, "minLength"), Integer.class));
            tf.add(new RangeValidator<Integer>(1, 256));

            add(tf = new RequiredTextField<Integer>("maxLength", new PropertyModel<Integer>(nameSettings, "maxLength"), Integer.class));
            tf.add(new RangeValidator<Integer>(1, 256));
        }
    }

    protected abstract class FolderSettingsStep extends Step {
        private static final long serialVersionUID = 1L;

        public FolderSettingsStep(IDynamicWizardStep previousStep, FolderSettings folderSettings) {
            super(previousStep);

            RequiredTextField<Integer> tf;
            add(tf = new RequiredTextField<Integer>("depth", new PropertyModel<Integer>(folderSettings, "depth"), Integer.class));
            tf.add(new RangeValidator<Integer>(0, 35));

            add(tf = new RequiredTextField<Integer>("minimumChildNodes", new PropertyModel<Integer>(folderSettings, "minimumChildNodes"),
                    Integer.class));
            tf.add(new RangeValidator<Integer>(1, 256));

            add(tf = new RequiredTextField<Integer>("maximumChildNodes", new PropertyModel<Integer>(folderSettings, "maximumChildNodes"),
                    Integer.class));
            tf.add(new RangeValidator<Integer>(1, 256));
        }

    }

    protected abstract class DocumentSettingsStep extends Step {
        private static final long serialVersionUID = 1L;

        public DocumentSettingsStep(IDynamicWizardStep previousStep, DocumentSettings documentSettings) {
            super(previousStep);

            RequiredTextField<Integer> tf;
            add(tf = new RequiredTextField<Integer>("amount", new PropertyModel<Integer>(documentSettings, "amount"), Integer.class));
            tf.add(new RangeValidator<Integer>(0, 500));

            AjaxCheckBox checkbox = new AjaxCheckBox("addTags", new PropertyModel<Boolean>(documentSettings, "addTags")) {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                }
            };
            add(checkbox);
        }
    }
}
