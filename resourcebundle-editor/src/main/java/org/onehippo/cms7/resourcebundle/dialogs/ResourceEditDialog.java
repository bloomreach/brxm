/*
 * Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.resourcebundle.dialogs;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.onehippo.cms7.resourcebundle.ResourceBundlePlugin;
import org.onehippo.cms7.resourcebundle.data.Resource;
import org.onehippo.cms7.resourcebundle.data.ValueSet;

public class ResourceEditDialog extends Dialog<Resource> {

    private ResourceBundlePlugin plugin;

    public ResourceEditDialog(final ResourceBundlePlugin plugin, final Resource resource) {
        this.plugin = plugin;

        setSize(DialogConstants.MEDIUM_AUTO);

        final String valueLabel = new StringResourceModel("dialog.resource.value.label", plugin, null).getObject();

        add(new Label("key-label", new StringResourceModel("dialog.resource.key.label", plugin, null)));
        add(new Label("key-value", resource.getKey())
                .add(plugin.makeTitle(plugin, "dialog.resource.edit.key.hint")));

        add(new Label("desc-label", new StringResourceModel("dialog.resource.desc.label", plugin, null)));
        add(new TextArea<>("desc-value", new PropertyModel<>(resource, "description")));

        add(new ListView<ValueSet>("repeater", resource.getBundle().getValueSets()) {
            @Override
            protected void populateItem(final ListItem<ValueSet> item) {
                final ValueSet valueSet = item.getModelObject();

                item.add(new Label("value-label", valueLabel));
                item.add(new Label("value-set-label", valueSet.getDisplayName()));
                item.add(new TextField<>("value-set-value", new Model<String>() {

                    @Override
                    public String getObject() {
                        return resource.getValue(valueSet.getName());
                    }

                    @Override
                    public void setObject(String value) {
                        resource.setValue(valueSet.getName(), value);
                    }
                }));

                item.setRenderBodyOnly(true);
            }
        });

        setFocusOnCancel();
    }

    public IModel<String> getTitle() {
        return new StringResourceModel("dialog.resource.edit.title", plugin, null);
    }
}
