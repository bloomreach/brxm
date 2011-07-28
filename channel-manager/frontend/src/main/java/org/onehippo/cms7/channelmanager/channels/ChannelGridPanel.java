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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.hst.configuration.channel.Channel;
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

    private static final String COLUMN_NAME = "name";
    private static final List<String> DEFAULT_COLUMNS = Collections.singletonList(COLUMN_NAME);

    private static final Logger log = LoggerFactory.getLogger(ChannelGridPanel.class);
    private ChannelStore store;

    public ChannelGridPanel(IPluginConfig config) {
        super();

        List<String> channelFields = parseChannelFields(config);
        List<ExtField> fieldList = new ArrayList<ExtField>();
        for (String fieldName : channelFields) {
            fieldList.add(new ExtField(fieldName));
        }

        // always include the hidden field 'channelId' that is used in code to identify each channel
        fieldList.add(new ExtField(ChannelStore.CHANNEL_ID));

        this.store = new ChannelStore(fieldList, parseSortColumn(config, channelFields), parseSortOrder(config));
        add(this.store);
    }

    static List<String> parseChannelFields(IPluginConfig config) {
        if (config == null) {
            return DEFAULT_COLUMNS;
        }
        String[] columnNames = config.getStringArray(CONFIG_COLUMNS);
        if (columnNames == null || columnNames.length == 0) {
            return DEFAULT_COLUMNS;
        }

        List<String> channelFields = new LinkedList<String>();
        for (String columnName : columnNames) {
            if (ReflectionUtil.getGetterMethodForField(Channel.class, columnName) != null) {
                channelFields.add(columnName);
            } else {
                log.warn("Ignoring channel list column '{}': class {} does not have a getter method for this field",
                        columnName, Channel.class.getName());
            }
        }

        if (channelFields.isEmpty()) {
            return DEFAULT_COLUMNS;
        }

        return channelFields;
    }

    static String parseSortColumn(IPluginConfig config, List<String> columnNames) {
        if (config == null) {
            return COLUMN_NAME;
        }

        String sortColumn = config.getString(CONFIG_SORT_COLUMN);

        if (!columnNames.contains(sortColumn)) {
            log.warn("Sort column '{}' is not one of the shown columns {}. Using column '{}' instead.",
                    new Object[]{sortColumn, columnNames, columnNames.get(0)});
            return columnNames.get(0);
        }

        return sortColumn;
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

        for (ExtField field : store.getFields()) {
            final String fieldName = field.getName();
            if (!fieldName.equals(ChannelStore.CHANNEL_ID)) {
                final JSONObject fieldConfig = new JSONObject();
                fieldConfig.put("dataIndex", fieldName);
                fieldConfig.put("id", fieldName);
                fieldConfig.put("header", getResourceValue("column." + fieldName));
                columnsConfig.put(fieldConfig);
            }
        }

        return columnsConfig;
    }

    private static String getResourceValue(String key) {
        return new ClassResourceModel(key, ChannelGridPanel.class).getObject();
    }

}
