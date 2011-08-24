/**
 * Copyright 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.channelmanager.channels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

/**
 * Ext Grid Panel for Channels Listing.
 */
@ExtClass("Hippo.ChannelManager.ChannelGridPanel")
public class ChannelGridPanel extends ExtPanel {

    public static final String CHANNEL_GRID_PANEL_JS = "Hippo.ChannelManager.ChannelGridPanel.js";

    public static final String CONFIG_COLUMNS = "columns";
    public static final String CONFIG_SORT_COLUMN = "sort.column";
    public static final String CONFIG_SORT_ORDER = "sort.order";
    private List<String> visibleFields;

    private static final Logger log = LoggerFactory.getLogger(ChannelGridPanel.class);
    private ChannelStore store;

    public ChannelGridPanel(IPluginConfig config) {
        super();

        visibleFields = parseChannelFields(config);

        // create an intermediate set of all unique field names to put in the ExtJS store: a union of all default
        // channel fields and the visible fields (since the latter may also include custom channel properties)
        Set<String> storeFieldNames = new HashSet<String>();
        storeFieldNames.addAll(visibleFields);
        for (ChannelStore.Column channelField : ChannelStore.Column.values()) {
            storeFieldNames.add(channelField.name());
        }

        // then create and add a store with all the Ext fields in the set
        List<ExtField> fieldList = new ArrayList<ExtField>();
        for (String storeFieldName : storeFieldNames) {
            fieldList.add(new ExtField(storeFieldName));
        }
        this.store = new ChannelStore(fieldList, parseSortColumn(config, visibleFields), parseSortOrder(config));
        add(this.store);
    }

    static List<String> parseChannelFields(IPluginConfig config) {
        List<String> columns = new ArrayList<String>();

        if (config == null) {
            return ChannelStore.ALL_COLUMN_NAMES;
        }

        String[] columnNames = config.getStringArray(CONFIG_COLUMNS);
        if (columnNames == null || columnNames.length == 0) {
            return ChannelStore.ALL_COLUMN_NAMES;
        }
        columns.addAll(Arrays.asList(columnNames));
        if (!columns.contains(ChannelStore.Column.name.name())) {
            columns.add(0, ChannelStore.Column.name.name());
        }
        return columns;
    }

    static String parseSortColumn(IPluginConfig config, List<String> columnNames) {
        if (config == null || columnNames.size() == 0) {
            return ChannelStore.Column.name.name();
        }

        String configSortColumn = config.getString(CONFIG_SORT_COLUMN);
        if (columnNames.contains(configSortColumn)) {
            return configSortColumn;
        }

        String fallback = columnNames.get(0);
        log.warn("Sort column '{}' is not one of the shown columns {}. Using column '{}' instead.",
                    new Object[]{configSortColumn, columnNames, fallback});
        return fallback;
    }

    static ChannelStore.SortOrder parseSortOrder(IPluginConfig config) {
        if (config == null) {
            return ChannelStore.SortOrder.ascending;
        }

        String order = config.getString(CONFIG_SORT_ORDER);
        if (order == null || order.equalsIgnoreCase("ascending")) {
            return ChannelStore.SortOrder.ascending;
        } else if (order.equalsIgnoreCase("descending")) {
            return ChannelStore.SortOrder.descending;
        } else {
            log.warn("Illegal sort order: '{}'. Using 'ascending' instead.", order);
            return ChannelStore.SortOrder.ascending;
        }
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("store", new JSONIdentifier(this.store.getJsObjectId()));
        properties.put("columns", getColumnsConfig());
    }

    public ChannelStore getStore() {
        return this.store;
    }

    private JSONArray getColumnsConfig() throws JSONException {
        JSONArray columnsConfig = new JSONArray();

        List<String> hiddenColumns = new ArrayList<String>(ChannelStore.ALL_COLUMN_NAMES);

        for (String columnName : visibleFields) {
            columnsConfig.put(createColumnFieldConfig(columnName, false));
            hiddenColumns.remove(columnName);
        }

        for (String columnName : hiddenColumns) {
            columnsConfig.put(createColumnFieldConfig(columnName, true));
        }

        return columnsConfig;
    }

    private JSONObject createColumnFieldConfig(String columnName, boolean isHidden) throws JSONException {
        final JSONObject fieldConfig = new JSONObject();
        fieldConfig.put("dataIndex", columnName);
        fieldConfig.put("id", columnName);
        fieldConfig.put("header", store.getColumnHeader(columnName));
        fieldConfig.put("hidden", isHidden);
        return fieldConfig;
    }

}
