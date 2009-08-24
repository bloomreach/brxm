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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddContentShortcutPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AddContentShortcutPlugin.class);

    private static final List<String> TEST_TYPES = new ArrayList<String>();
    {
        TEST_TYPES.add(new String("news"));
        TEST_TYPES.add(new String("article"));
    }

    ContentBuilder builder;

    public AddContentShortcutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        builder = new ContentBuilder();

        add(new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(new AddContentShortcutPlugin.Dialog());
            }

        });
    }

    public class Dialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        String folder = "/content/documents/news";
        Collection<String> selectedTypes = new LinkedList<String>();
        int minLength = 20;
        int maxLength = 35;
        int amount = 5;
        boolean randomDocuments = true;

        CheckGroup group;

        public Dialog() {
            setOkLabel(new StringResourceModel("start-add-content-label", AddContentShortcutPlugin.this, null));

            final WebMarkupContainer container = new WebMarkupContainer("typesContainer");

            RequiredTextField tf;
            add(tf = new RequiredTextField("folder", new PropertyModel(this, "folder")));
            tf.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(container);
                }
            });


            group = new CheckGroup("typesGroup", selectedTypes);
            group.setOutputMarkupId(true);

            container.setOutputMarkupId(true);
            container.add(group);
            
            add(container);
            //group.add(new CheckGroupSelector("groupselector"));
            final ListView typesListView = new ListView("types", getTypes()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem item) {
                    IModel m = item.getModel();
                    item.add(new Check("check", m));
                    item.add(new Label("name", m));
                }
            };
            typesListView.setOutputMarkupId(true);
            
            group.add(typesListView);

            CheckBox randomDocs = new CheckBox("randomDocs", new PropertyModel(this, "randomDocuments"));
            randomDocs.setOutputMarkupId(true);
            randomDocs.add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;
                
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    container.setVisible(!randomDocuments);
                    typesListView.setEnabled(!randomDocuments);
                    typesListView.setVisible(!randomDocuments);
                    target.addComponent(container);
                }
            });
            add(randomDocs);

            add(tf = new RequiredTextField("minLength", new PropertyModel(this, "minLength"), Integer.class));
            tf.add(NumberValidator.range(1, 256));

            add(tf = new RequiredTextField("maxLength", new PropertyModel(this, "maxLength"), Integer.class));
            tf.add(NumberValidator.range(1, 256));

            add(tf = new RequiredTextField("amount", new PropertyModel(this, "amount"), Integer.class));
            tf.add(NumberValidator.range(1, 256));

        }

        public IModel getTitle() {
            return new StringResourceModel("add-content-label", AddContentShortcutPlugin.this, null);
        }

        @Override
        protected void onOk() {
            if (randomDocuments) {
                builder.createRandomDocuments(folder, minLength, maxLength, amount);
            } else {
                builder.createDocuments(folder, selectedTypes, minLength, maxLength, amount);
            }
        }

        public IModel getTypes() {
            return new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return builder.getDocumentTypes(folder);
                }
            };
        }
    }

}
