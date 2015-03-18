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

package org.hippoecm.frontend.plugins.development.content.wizard;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
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
import org.hippoecm.frontend.plugins.development.content.ContentBuilder;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.DocumentSettings;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.FolderSettings;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.NameSettings;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.NodeTypeSettings;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.wizard.AjaxWizard;
import org.hippoecm.frontend.plugins.yui.tree.YuiJcrTree;

public abstract class DevelopmentContentWizard extends AjaxWizard {

    IPluginConfig config;
    IPluginContext context;
    Component buttons;

    public DevelopmentContentWizard(String id, IPluginContext context, IPluginConfig config) {
        super(id, false);

        this.context = context;
        this.config = config;

        init(new DynamicWizardModel(createFirstStep()));
    }

    @Override
    protected Component newButtonBar(final String id) {
        buttons = super.newButtonBar(id);
        buttons.add(CssClass.append("hippo-window-buttons"));
        return buttons;
    }

    protected abstract IDynamicWizardStep createFirstStep();

    protected abstract class Step extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        public Step(IDynamicWizardStep previousStep) {
            super(previousStep);

            String title = getStepTitle();
            if (title != null) {
                setTitleModel(new Model<>(title));

            }

            String sum = getStepSummary();
            if (sum != null) {
                setSummaryModel(new Model<>(sum));

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

        IModel<String> model;

        public ChooseFolderStep(IDynamicWizardStep previousStep, IModel<String> model) {
            super(previousStep);

            this.model = model;

            add(new YuiJcrTree("mytree", config, model) {

                @Override
                protected void onClick(AjaxRequestTarget target, String uuid) {
                    ChooseFolderStep.this.model.setObject(uuid);
                    ChooseFolderStep.this.onClick(target, uuid);
                    target.add(buttons);
                }

                @Override
                protected void onDblClick(AjaxRequestTarget target, String uuid) {
                    ChooseFolderStep.this.onDblClick(target, uuid);
                    target.add(buttons);
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

        @Override
        public boolean isNextAvailable() {
            return StringUtils.isNotEmpty(model.getObject());
        }
    }

    protected abstract class SelectTypesStep extends Step {

        NodeTypeSettings settings;

        public SelectTypesStep(IDynamicWizardStep previousStep, final NodeTypeSettings settings) {
            super(previousStep);
            this.settings = settings;

            final WebMarkupContainer container = new WebMarkupContainer("typesContainer");
            container.setOutputMarkupId(true);
            container.add(AttributeModifier.replace("style", new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return settings.isRandom() ? "display: none;" : "display: block;";
                }
            }));
            add(container);

            CheckGroup group = new CheckGroup<>("typesGroup", new PropertyModel<>(settings, "types"));
            container.add(group);

            final ListView<ContentBuilder.CategoryType> typesListView = new ListView<ContentBuilder.CategoryType>("types", getTypesModel()) {
                @Override
                protected void populateItem(ListItem<ContentBuilder.CategoryType> item) {
                    IModel<ContentBuilder.CategoryType> m = item.getModel();
                    item.add(new Check<>("check", m));
                    item.add(new Label("name", m));
                }
            };
            group.add(typesListView);

            AjaxCheckBox randomDocs = new AjaxCheckBox("randomDocs", PropertyModel.of(settings, "random")) {
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

        private IModel<List<ContentBuilder.CategoryType>> getTypesModel() {
            return new AbstractReadOnlyModel<List<ContentBuilder.CategoryType>>() {
                @Override
                public List<ContentBuilder.CategoryType> getObject() {
                    return getTypes();
                }
            };
        }

        protected abstract List<ContentBuilder.CategoryType> getTypes();
    }

    protected abstract class NameSettingsStep extends Step {

        public NameSettingsStep(IDynamicWizardStep previousStep, NameSettings nameSettings) {
            super(previousStep);

            RequiredTextField<Integer> tf;
            add(tf = new RequiredTextField<>("minLength", PropertyModel.of(nameSettings, "minLength"), Integer.class));
            tf.add(new RangeValidator<>(1, 256));

            add(tf = new RequiredTextField<>("maxLength", PropertyModel.of(nameSettings, "maxLength"), Integer.class));
            tf.add(new RangeValidator<>(1, 256));
        }
    }

    protected abstract class FolderSettingsStep extends Step {

        public FolderSettingsStep(IDynamicWizardStep previousStep, FolderSettings folderSettings) {
            super(previousStep);

            RequiredTextField<Integer> tf;
            add(tf = new RequiredTextField<>("depth", PropertyModel.of(folderSettings, "depth"), Integer.class));
            tf.add(new RangeValidator<>(0, 35));

            add(tf = new RequiredTextField<>("minimumChildNodes", PropertyModel.of(folderSettings, "minimumChildNodes"),
                    Integer.class));
            tf.add(new RangeValidator<>(1, 256));

            add(tf = new RequiredTextField<>("maximumChildNodes", PropertyModel.of(folderSettings, "maximumChildNodes"),
                    Integer.class));
            tf.add(new RangeValidator<>(1, 256));
        }
    }

    protected abstract class DocumentSettingsStep extends Step {

        public DocumentSettingsStep(IDynamicWizardStep previousStep, DocumentSettings documentSettings) {
            super(previousStep);

            RequiredTextField<Integer> tf;
            add(tf = new RequiredTextField<>("amount", PropertyModel.of(documentSettings, "amount"), Integer.class));
            tf.add(new RangeValidator<>(0, 500));
            tf.add(new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    onChangeAmount(target);
                }
            });

            AjaxCheckBox checkbox = new AjaxCheckBox("addTags", PropertyModel.of(documentSettings, "addTags")) {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                }
            };
            add(checkbox);
        }

        protected void onChangeAmount(final AjaxRequestTarget target) {
        }
    }
}
