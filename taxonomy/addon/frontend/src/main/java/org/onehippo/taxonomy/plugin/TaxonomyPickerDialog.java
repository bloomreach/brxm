/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.TaxonomyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaxonomyPickerDialog used by {@link TaxonomyPickerPlugin} to show picker dialog to users.
 * @version $Id$
 */
public class TaxonomyPickerDialog extends AbstractDialog<Classification> {

    static final Logger log = LoggerFactory.getLogger(TaxonomyPickerDialog.class);

    public static final String CONFIG_TYPE = "taxonomy.type";
    public static final String TREE = "tree";
    public static final String PALETTE = "palette";

    protected String viewType;

    /**
     * The panel where {@link TaxonomyBrowser} instance is located.
     */
    protected Panel browser;

    /**
     * Constructor which adds UI components in the dialog.
     * The main UI component in this dialog is the taxonomy browser.
     * @param context
     * @param config
     * @param model
     * @param preferredLocale
     */
    public TaxonomyPickerDialog(final IPluginContext context, final IPluginConfig config, IModel<Classification> model,
                                String preferredLocale) {
        super(model);

        setOkEnabled(true);
        setOutputMarkupId(true);

        viewType = config.getString(CONFIG_TYPE, TREE);
        if (PALETTE.equals(viewType)) {
            add(browser = new TaxonomyPalette("content", new Model<>(model.getObject()),
                    new TaxonomyModel(context, config), preferredLocale));
        } else {
            if (!TREE.equals(viewType)) {
                log.warn("Invalid taxonomy picker type " + viewType + ", falling back to 'tree'");
            }

            add(browser = new TaxonomyBrowser("content", new Model<>(model.getObject()),
                    new TaxonomyModel(context, config), preferredLocale));
        }
    }

    @Override
    public IValueMap getProperties() {
        if (TREE.equals(viewType)) {
            return DialogConstants.LARGE;
        } else {
            return new ValueMap("width=500,height=368");
        }
    }

    public IModel<String> getTitle() {
        return Model.of(getString("taxonomy-picker"));
    }

    @Override
    protected void onOk() {
        setModelObject((Classification) browser.getDefaultModelObject());
        super.onOk();
    }
}
