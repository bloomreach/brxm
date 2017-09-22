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

import java.util.Locale;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.taxonomy.plugin.TaxonomyPickerDialog;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.TaxonomyModel;

public class AdditionalFieldCustomTaxonomyPickerDialog extends TaxonomyPickerDialog {

        private static final long serialVersionUID = 1L;

        public AdditionalFieldCustomTaxonomyPickerDialog(IPluginContext context, IPluginConfig config,
                IModel<Classification> model, Locale preferredLocale) {
            super(context, config, model, preferredLocale);

            addOrReplace(browser = new AdditionalFieldCustomTaxonomyBrowser("content", new Model<>(model.getObject()),
                    new TaxonomyModel(context, config), preferredLocale));
        }
    }
