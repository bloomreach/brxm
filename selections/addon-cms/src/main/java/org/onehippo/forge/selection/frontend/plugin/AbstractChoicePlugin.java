/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.onehippo.forge.selection.frontend.plugin;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.plugin.sorting.SortHelper;
import org.onehippo.forge.selection.frontend.provider.IValueListProvider;
import org.onehippo.forge.selection.frontend.utils.SelectionUtils;

/**
 * Base class for CMS widgets that enable the user to select an item from a list.
 * Contains logic for fetching the list and generic plugin functionalities.
 * The list is backed by a ValueListProvider service that provides a ValueList
 * object.
 *
 * The default DocumentValueListProvider reads a document of the type
 * 'selection:valuelist', which contains key label pairs used to display values
 * and labels in the dropdown.
 *
 * The plugin configuration must then be provided with a <code>source</code>
 * property, which can either be a valid UUID of a handle or the path to the
 * document based on the JCR root.
 */
public abstract class AbstractChoicePlugin extends RenderPlugin<String> {
    private static final long serialVersionUID = 1L;

    private static final ValueList EMPTY_VALUE_LIST = new ValueList();
    private final SortHelper sortHelper = new SortHelper();

    /**
     * Constructor for this class.
     *
     * @param context plugin context
     * @param config  plugin config
     */
    public AbstractChoicePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    /**
     * Returns whether plugin is in edit mode
     *
     * @return true if plugin is in edit mode
     */
    protected boolean isInEditMode() {
        final String mode = getPluginConfig().getString(ITemplateEngine.MODE);
        return "edit".equals(mode);
    }

    /**
     * Returns the list of value items that is used to populate
     * the choices for this selectable widget
     *
     * @return list of values
     */
    protected ValueList getValueList() {
        // configured provider
        ValueList valueList = getValueListFromProvider();
        if (valueList != null) {
            return valueList;
        }

        return EMPTY_VALUE_LIST;
    }


    /**
     * Returns the value list provided by the provider configured for this plugin.
     *
     * @return value list from configured value list provider
     */
    private ValueList getValueListFromProvider() {
        IValueListProvider selectedProvider = getValueListProvider();
        if (selectedProvider == null) {
            return null;
        }
        final Locale locale = SelectionUtils.getLocale(SelectionUtils.getNode(getModel()));
        final ValueList valueList = selectedProvider.getValueList(getPluginConfig().getString(Config.SOURCE), locale);

        sortHelper.sort(valueList, getPluginConfig());

        return valueList;
    }

    /**
     * Returns the value list provider configured for this plugin.
     */
    private IValueListProvider getValueListProvider() {
        String providerName = getPluginConfig().getString(Config.VALUELIST_PROVIDER, IValueListProvider.SERVICE_VALUELIST_DEFAULT);
        if (StringUtils.isNotBlank(providerName)) {
            return getPluginContext().getService(providerName, IValueListProvider.class);
        }

        return null;
    }

    /**
     * Returns the property value model associated with this plugin.
     *
     * @return property value model
     */
    protected JcrPropertyValueModel<String> getValueModel() {
        return (JcrPropertyValueModel<String>) getModel();
    }
}
