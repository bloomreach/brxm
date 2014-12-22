/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.onehippo.cms7.resourcebundle.ResourceBundlePlugin;
import org.onehippo.cms7.resourcebundle.data.Bundle;
import org.onehippo.cms7.resourcebundle.data.Resource;
import org.onehippo.cms7.resourcebundle.data.ValueSet;
import org.onehippo.cms7.resourcebundle.validators.ResourceKeyValidator;

public class ResourceCopyDialog extends AbstractDialog<Resource> {

    private ResourceBundlePlugin plugin;
    private boolean isAdd;
    private Resource resource;

    public ResourceCopyDialog(final ResourceBundlePlugin plugin, Bundle bundle, Resource originalResource) {
        this.plugin = plugin;

        isAdd = originalResource == null;
        resource = isAdd ? bundle.newResource() : bundle.copyResource(originalResource);
        final String valueLabel = new StringResourceModel("dialog.resource.value.label", plugin, null).getObject();

        add(new Label("key-label", new StringResourceModel("dialog.resource.key.label", plugin, null)));
        add(new TextField<>("key-value", new PropertyModel<String>(resource, "key")).setRequired(true)
                .add(new ResourceKeyValidator(plugin, resource)));

        add(new Label("desc-label", new StringResourceModel("dialog.resource.desc.label", plugin, null)));
        add(new TextArea<>("desc-value", new PropertyModel<String>(resource, "description")));

        add(new ListView<ValueSet>("repeater", bundle.getValueSets()) {
            private static final long serialVersionUID = 1L;

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
            }
        });

        setFocusOnCancel();
    }

    public IModel<String> getTitle() {
        String key = isAdd ? "dialog.resource.add.title" : "dialog.resource.copy.title";
        return new StringResourceModel(key, plugin, null);
    }

    protected Resource getResource() {
        return resource;
    }
}
