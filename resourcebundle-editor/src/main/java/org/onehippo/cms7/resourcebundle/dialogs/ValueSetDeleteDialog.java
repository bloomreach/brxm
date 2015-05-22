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

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.onehippo.cms7.resourcebundle.ResourceBundlePlugin;
import org.onehippo.cms7.resourcebundle.data.Bundle;
import org.onehippo.cms7.resourcebundle.data.ValueSet;

public class ValueSetDeleteDialog extends Dialog<String> {

    private ResourceBundlePlugin plugin;
    private ValueSet selectedValueSet;

    public ValueSetDeleteDialog(final ResourceBundlePlugin plugin, Bundle bundle) {

        setSize(DialogConstants.MEDIUM_AUTO);

        List<ValueSet> valueSets = bundle.getMutableValueSets();

        this.plugin = plugin;
        this.selectedValueSet = valueSets.get(0);

        add(new Label("label", new StringResourceModel("dialog.valueset.delete.label", plugin, null)));
        add(new DropDownChoice<>("dropdown",
                                 new PropertyModel<>(this, "selectedValueSet"),
                                 valueSets,
                                 plugin.getValueSetRenderer())
            .setNullValid(false)
            .setRequired(true)
        );

        setFocusOnCancel();
    }

    @Override
    public IModel<String> getTitle() {
        return new StringResourceModel("dialog.valueset.delete.title", plugin, null);
    }

    public ValueSet getValueSet() {
        return selectedValueSet;
    }
}
