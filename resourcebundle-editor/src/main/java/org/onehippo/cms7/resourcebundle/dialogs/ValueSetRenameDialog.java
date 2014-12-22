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

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.onehippo.cms7.resourcebundle.ResourceBundlePlugin;
import org.onehippo.cms7.resourcebundle.data.Bundle;
import org.onehippo.cms7.resourcebundle.data.ValueSet;
import org.onehippo.cms7.resourcebundle.validators.ValueSetNameValidator;

/**
 * @version "$Id$"
 */
public class ValueSetRenameDialog extends AbstractDialog<String> {

    private static final long serialVersionUID = 1L;
    private ResourceBundlePlugin plugin;
    private ValueSet selectedValueSet;
    private String newName;

    public ValueSetRenameDialog(final ResourceBundlePlugin plugin, Bundle bundle) {

        List<ValueSet> valueSets = bundle.getMutableValueSets();
        this.plugin = plugin;
        selectedValueSet = valueSets.get(0);
        newName = "";

        final TextField<String> newNameField = new TextField<String>("name", new PropertyModel<String>(this, "newName"));
        newNameField.setRequired(true);
        newNameField.add(new ValueSetNameValidator(plugin, selectedValueSet));
        newNameField.setOutputMarkupId(true);

        add(new Label("label", new StringResourceModel("dialog.valueset.rename.label", plugin, null)));
        add(new DropDownChoice<>("dropdown",
                new PropertyModel<ValueSet>(this, "selectedValueSet"),
                valueSets,
                plugin.getValueSetRenderer())
                .setNullValid(false)
                .setRequired(true)
        );
        add(newNameField);

        setFocusOnCancel();
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("dialog.valueset.rename.title", plugin, null);
    }

    public ValueSet getValueSet() {
        selectedValueSet.setDisplayName(newName);
        return selectedValueSet;
    }
}
