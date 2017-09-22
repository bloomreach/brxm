/*
 * Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.demo.plugin;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.onehippo.taxonomy.plugin.TaxonomyEditorPlugin;
import org.onehippo.taxonomy.plugin.api.EditableCategory;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.api.TaxonomyException;

/**
 * AdditionalFieldCustomTaxonomyEditorPlugin
 * <P>
 * This extended plugin is an example to show how you can add a custom field
 * in the category editing page.
 * So, this extended plugin adds 'fulldescription' field in addition to the
 * 'description' field.
 * </P>
 * @version $Id$
 */
public class AdditionalFieldCustomTaxonomyEditorPlugin extends TaxonomyEditorPlugin {

    private static final long serialVersionUID = 1L;

    private final class FullDescriptionModel implements IModel<String> {
        private static final long serialVersionUID = 1L;

        public String getObject() {
            EditableCategory category = getCategory();

            if (category != null) {
                return category.getInfo(getCurrentLocaleSelection()).getString(CustomTaxonomyConstants.FULL_DESCRIPTION, "");
            }

            return null;
        }

        public void setObject(String object) {
            EditableCategoryInfo info = getCategory().getInfo(getCurrentLocaleSelection());

            try {
                info.setString(CustomTaxonomyConstants.FULL_DESCRIPTION, object);
            } catch (TaxonomyException e) {
                error(e.getMessage());
                redraw();
            }
        }

        public void detach() {
        }
    }

    public AdditionalFieldCustomTaxonomyEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        final boolean editing = "edit".equals(config.getString("mode"));
        final Form<?> container = getContainerForm();
        
        if (editing) {
            container.add(new TextAreaWidget("fulldescription", new FullDescriptionModel()));
        } else {
            container.add(new MultiLineLabel("fulldescription", new FullDescriptionModel()));
        }
    }

}
