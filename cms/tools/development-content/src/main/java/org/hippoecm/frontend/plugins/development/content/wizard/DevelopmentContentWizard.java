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

package org.hippoecm.frontend.plugins.development.content.wizard;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.wicket.IClusterable;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.development.content.ContentBuilder.NameSettings;
import org.hippoecm.frontend.plugins.standards.wizard.AjaxWizard;
import org.hippoecm.frontend.plugins.yui.tree.YuiJcrTree;

public abstract class DevelopmentContentWizard extends AjaxWizard {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    IPluginConfig config;
    IPluginContext context;

    public DevelopmentContentWizard(String id, IPluginContext context, IPluginConfig config) {
        super(id, false);

        this.context = context;
        this.config = config;

        init(new DynamicWizardModel(createFirstStep()));

    }

    protected abstract IDynamicWizardStep createFirstStep();

    protected abstract class ChooseFolderStep extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;
        
        IModel model;

        public ChooseFolderStep(IDynamicWizardStep previousStep, IModel model) {
            super(previousStep);
            
            this.model = model;

            add(new YuiJcrTree("mytree", context, config) {
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

    public static class SelectedTypesSettings implements IClusterable {
        private static final long serialVersionUID = 1L;

        Collection<String> selectedTypes = new LinkedList<String>();
        boolean random = true;

        public void setSelectedTypes(Collection<String> selectedTypes) {
            this.selectedTypes = selectedTypes;
        }

        public void setRandom(boolean random) {
            this.random = random;
        }

        public Collection getSelectedTypes() {
            return selectedTypes;
        }

        public boolean isRandom() {
            return random;
        }
    }

    protected abstract class SelectTypesStep extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        SelectedTypesSettings settings;

        public SelectTypesStep(IDynamicWizardStep previousStep, final SelectedTypesSettings settings) {
            super(previousStep);
            this.settings = settings;

            final WebMarkupContainer container = new WebMarkupContainer("typesContainer");
            container.setOutputMarkupId(true);
            add(container);

            CheckGroup group = new CheckGroup("typesGroup", new PropertyModel(settings, "selectedTypes")) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return !settings.isRandom();
                }

            };
            container.add(group);

            //group.add(new CheckGroupSelector("groupselector"));
            final ListView typesListView = new ListView("types", getTypesModel()) {
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
                    target.addComponent(container);
                }
            };

            randomDocs.setOutputMarkupId(true);
            add(randomDocs);
        }

        public boolean isLastStep() {
            return false;
        }

        private IModel getTypesModel() {
            return new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return getTypes();
                }
            };
        }

        protected abstract Collection<String> getTypes();
    }

    protected abstract class NameSettingsStep extends DynamicWizardStep {
        private static final long serialVersionUID = 1L;

        public NameSettingsStep(IDynamicWizardStep previousStep, NameSettings nameSettings) {
            super(previousStep);

            RequiredTextField tf;
            add(tf = new RequiredTextField("minLength", new PropertyModel(nameSettings, "minLength"), Integer.class));
            tf.add(NumberValidator.range(1, 256));

            add(tf = new RequiredTextField("maxLength", new PropertyModel(nameSettings, "maxLength"), Integer.class));
            tf.add(NumberValidator.range(1, 256));

            add(tf = new RequiredTextField("amount", new PropertyModel(nameSettings, "amount"), Integer.class));
            tf.add(NumberValidator.range(1, 256));

        }

    }
}
