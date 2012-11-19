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

import org.apache.wicket.Localizer;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
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
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ChannelGridPanel.class);
    private ChannelStore store;
    private List<String> visibleFields;
    
    @ExtProperty
    @SuppressWarnings("unused")
    private String cmsUser;
    
    @ExtProperty
    @SuppressWarnings("unused")
    private String composerRestMountUrl;

    public ChannelGridPanel(IPluginConfig config, ExtStoreFuture storeFuture) {
        this.store = (ChannelStore) storeFuture.getStore();

        this.cmsUser = UserSession.get().getJcrSession().getUserID();
        this.composerRestMountUrl = config.getString("templateComposerContextPath", "/site") +
                config.getString("composerRestMountPath", "/_rp");

        visibleFields = parseChannelFields(config);
        visibleFields.removeAll(ChannelStore.INTERNAL_FIELDS);
    }

    static List<String> parseChannelFields(IPluginConfig config) {
        List<String> columns = new ArrayList<String>();

        if (config == null) {
            return ChannelStore.ALL_FIELD_NAMES;
        }

        String[] columnNames = config.getStringArray(ChannelStoreFactory.CONFIG_COLUMNS);
        if (columnNames == null || columnNames.length == 0) {
            return ChannelStore.ALL_FIELD_NAMES;
        }
        columns.addAll(Arrays.asList(columnNames));
        if (!columns.contains(ChannelStore.ChannelField.name.name())) {
            columns.add(0, ChannelStore.ChannelField.name.name());
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

        List<String> hiddenFields = new ArrayList<String>(ChannelStore.ALL_FIELD_NAMES);
        hiddenFields.removeAll(ChannelStore.INTERNAL_FIELDS);

        for (String columnfield : visibleFields) {
            columnsConfig.put(createColumnFieldConfig(columnfield, false));
            hiddenFields.remove(columnfield);
        }

        for (String fieldName : hiddenFields) {
            columnsConfig.put(createColumnFieldConfig(fieldName, true));
        }

        return columnsConfig;
    }

    private JSONObject createColumnFieldConfig(String columnName, boolean isHidden) throws JSONException {
        final JSONObject fieldConfig = new JSONObject();
        fieldConfig.put("dataIndex", columnName);
        fieldConfig.put("id", columnName);
        fieldConfig.put("header", store.getLocalizedFieldName(columnName));
        fieldConfig.put("hidden", isHidden);

        if (ChannelStore.ChannelField.name.name().equals(columnName)) {
            // render the 'name' column as a link to the template composer
            String tooltipNamePrefix = getLocalizer().getString("tooltip.name.prefix", this);
            fieldConfig.put("xtype", "templatecolumn");
            fieldConfig.put("tpl", "<a href=\"#\" name=\"show-channel\" title=\"" + tooltipNamePrefix + " {name}\">{name}</a>");
        } else if (ChannelStore.ChannelField.url.name().equals(columnName)) {
            // render the 'url' column as two links: one to the live site, and one to the preview site
            Localizer localizer = getLocalizer();
            CharSequence previewLabel = Strings.escapeMarkup(localizer.getString("action.preview", this));
            CharSequence liveTooltip = Strings.escapeMarkup(localizer.getString("tooltip.live", this));
            CharSequence previewTooltip = Strings.escapeMarkup(localizer.getString("tooltip.preview", this));
            fieldConfig.put("xtype", "templatecolumn");
            fieldConfig.put("tpl", "<a href=\"{url}\" name=\"show-live\" class=\"show-live\" target=\"_blank\" "
                    + "title=\"" + liveTooltip + "\">{url}</a>"
                    + "<a href=\"{contextPath}"
                    + "{[values.cmsPreviewPrefix !== '' ? '/' : '']}{cmsPreviewPrefix}"
                    + "{[values.mountPath === '' ? '/' : '']}{mountPath}\" "
                    + "name=\"show-preview\" class=\"show-preview\" target=\"hippochannelmanagerpreview\" title=\""
                    + previewTooltip + "\">" + previewLabel + "</a>");
        } else if (ChannelStore.ChannelField.lockedOn.name().equals(columnName)) {
            String dateFormat = getLocalizer().getString("locked-date-format", this);
            String renderer = "function(timestamp) { return Ext.isEmpty(timestamp) ? '' : new Date(parseInt(timestamp)).format(\"" + dateFormat + "\"); }";
            fieldConfig.put("renderer", new JSONIdentifier(renderer));
        }
        return fieldConfig;
    }

}
