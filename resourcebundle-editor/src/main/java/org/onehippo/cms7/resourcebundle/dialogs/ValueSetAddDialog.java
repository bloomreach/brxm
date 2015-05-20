/*
 * Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.onehippo.cms7.resourcebundle.ResourceBundlePlugin;
import org.onehippo.cms7.resourcebundle.data.Bundle;
import org.onehippo.cms7.resourcebundle.data.ValueSet;
import org.onehippo.cms7.resourcebundle.validators.ValueSetNameValidator;

/**
 * @version "$Id$"
 */
public class ValueSetAddDialog extends Dialog<String> {

    private static final long serialVersionUID = 1L;
    private final ResourceBundlePlugin plugin;
    private ValueSet valueSet;

    public ValueSetAddDialog(ResourceBundlePlugin plugin, Bundle bundle) {

        this.plugin = plugin;
        valueSet = bundle.newValueSet();

        add(new Label("label", new StringResourceModel("dialog.valueset.add.label", plugin, null)));
        add(new TextField<>("name", new PropertyModel<String>(valueSet, "displayName")).setRequired(true)
                .add(new ValueSetNameValidator(plugin, valueSet)));

        setFocusOnCancel();
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("dialog.valueset.add.title", plugin, null);
    }

    public ValueSet getValueSet() {
        return valueSet;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.MEDIUM_AUTO;
    }
}
