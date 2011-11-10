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
import java.util.List;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

/**
 * Ext Grid Panel for Channels Listing.
 */
@ExtClass("Hippo.ChannelManager.ChannelGridPanel")
public class ChannelGridPanel extends ExtPanel {

    public static final String CHANNEL_GRID_PANEL_JS = "ChannelGridPanel.js";

    private static final Logger log = LoggerFactory.getLogger(ChannelGridPanel.class);
    private ChannelStore store;
    private List<String> visibleFields;
    private ExtStoreFuture storeFuture;

    @ExtProperty
    private boolean canModifyChannels;

    public ChannelGridPanel(IPluginContext context, IPluginConfig config, ExtStoreFuture storeFuture) {
        super();
        this.store = (ChannelStore) storeFuture.getStore();
        canModifyChannels = store.canModifyChannels();
        this.storeFuture = storeFuture;
        visibleFields = parseChannelFields(config);
    }

    static List<String> parseChannelFields(IPluginConfig config) {
        List<String> columns = new ArrayList<String>();

        if (config == null) {
            return ChannelStore.ALL_COLUMN_NAMES;
        }

        String[] columnNames = config.getStringArray(ChannelStoreFactory.CONFIG_COLUMNS);
        if (columnNames == null || columnNames.length == 0) {
            return ChannelStore.ALL_COLUMN_NAMES;
        }
        columns.addAll(Arrays.asList(columnNames));
        if (!columns.contains(ChannelStore.Column.name.name())) {
            columns.add(0, ChannelStore.Column.name.name());
        }
        return columns;
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("store", new JSONIdentifier(this.store.getJsObjectId()));
        properties.put("columns", getColumnsConfig());
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
