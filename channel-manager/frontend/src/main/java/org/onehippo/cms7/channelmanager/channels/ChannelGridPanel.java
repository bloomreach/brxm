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
import java.util.EnumSet;
import java.util.List;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
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
    private List<ChannelStore.Column> visibleFields;

    private static final Logger log = LoggerFactory.getLogger(ChannelGridPanel.class);
    private ChannelStore store;

    public ChannelGridPanel(IPluginConfig config) {
        super();

        List<ExtField> fieldList = new ArrayList<ExtField>();
        for (ChannelStore.Column column : ChannelStore.Column.values()) {
            fieldList.add(new ExtField(column.name()));
        }

        visibleFields = parseChannelFields(config);

        this.store = new ChannelStore(fieldList, parseSortColumn(config, visibleFields), parseSortOrder(config));
        add(this.store);
    }

    static List<ChannelStore.Column> parseChannelFields(IPluginConfig config) {
        if (config == null) {
            return Arrays.asList(ChannelStore.Column.values());
        }
        String[] columnNames = config.getStringArray(CONFIG_COLUMNS);
        if (columnNames == null || columnNames.length == 0) {
            return Arrays.asList(ChannelStore.Column.values());
        }

        List<ChannelStore.Column> columns = new ArrayList<ChannelStore.Column>();
        for (String columnName : columnNames) {
            try {
                columns.add(ChannelStore.Column.valueOf(columnName));
            } catch (Exception exception) {
                // ignore
            }
        }
        if (!columns.contains(ChannelStore.Column.name)) {
            columns.add(0, ChannelStore.Column.name);
        }
        return columns;
    }

    static String parseSortColumn(IPluginConfig config, List<ChannelStore.Column> columnNames) {
        if (config == null || columnNames.size() == 0) {
            return ChannelStore.Column.name.name();
        }
        String configSortColumn = config.getString(CONFIG_SORT_COLUMN);
        try {
            ChannelStore.Column sortColumn = ChannelStore.Column.valueOf(configSortColumn);
            for (ChannelStore.Column column : columnNames) {
                if (sortColumn == column) {
                    return sortColumn.name();
                }
            }
        } catch (Exception exception) {
            // ignore
        }
        ChannelStore.Column fallback = columnNames.get(0);
        log.warn("Sort column '{}' is not one of the shown columns {}. Using column '{}' instead.",
                    new Object[]{configSortColumn, columnNames, fallback});
        return fallback.name();
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

        EnumSet<ChannelStore.Column> hiddenColumns = EnumSet.allOf(ChannelStore.Column.class);
        for (ChannelStore.Column column : visibleFields) {
            final JSONObject fieldConfig = new JSONObject();
            fieldConfig.put("dataIndex", column.name());
            fieldConfig.put("id", column.name());
            fieldConfig.put("header", getResourceValue("column." + column.name()));
            fieldConfig.put("hidden", false);
            columnsConfig.put(fieldConfig);
            hiddenColumns.remove(column);
        }

        for (ChannelStore.Column column : hiddenColumns) {
            final JSONObject fieldConfig = new JSONObject();
            fieldConfig.put("dataIndex", column.name());
            fieldConfig.put("id", column.name());
            fieldConfig.put("header", getResourceValue("column." + column.name()));
            fieldConfig.put("hidden", true);
            columnsConfig.put(fieldConfig);
        }

        return columnsConfig;
    }

    private static String getResourceValue(String key) {
        return new ClassResourceModel(key, ChannelGridPanel.class).getObject();
    }

}
