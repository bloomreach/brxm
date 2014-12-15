/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.taxonomy.demo.plugin;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.taxonomy.plugin.TaxonomyPickerPlugin;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.ClassificationModel;

public class AdditionalFieldCustomTaxonomyPickerPlugin extends TaxonomyPickerPlugin {

    private static final long serialVersionUID = 1L;

    public AdditionalFieldCustomTaxonomyPickerPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected AbstractDialog<Classification> createPickerDialog(ClassificationModel model, String preferredLocale) {
        return new AdditionalFieldCustomTaxonomyPickerDialog(getPluginContext(), getPluginConfig(), model, preferredLocale);
    }


}
