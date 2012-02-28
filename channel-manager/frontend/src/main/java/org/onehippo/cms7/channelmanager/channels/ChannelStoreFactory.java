/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.channelmanager.channels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.hst.rest.BlueprintService;
import org.hippoecm.hst.rest.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;

public final class ChannelStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(ChannelStore.class);

    public static final String CONFIG_COLUMNS = "columns";
    public static final String CONFIG_SORT_COLUMN = "sort.column";
    public static final String CONFIG_SORT_ORDER = "sort.order";

    private ChannelStoreFactory() {
        // prevent instantiation
    }

    public static ChannelStore createStore(IPluginContext context, IPluginConfig config, ChannelService channelService, BlueprintService blueprintService) {
        Set<String> storeFieldNames = parseChannelFields(config);

        // then create a list of all the Ext fields in the store
        List<ExtField> fieldList = new ArrayList<ExtField>();
        for (String storeFieldName : storeFieldNames) {
            fieldList.add(new ExtField(storeFieldName));
        }

        // Retrieve the Hippo locale provider to resolve locales of new channels
        String localeProviderServiceId = config.getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName());
        ILocaleProvider localeProvider = context.getService(localeProviderServiceId, ILocaleProvider.class);
        if (localeProvider == null) {
            throw new IllegalStateException(String.format("Cannot find locale provider service with ID '%s'", localeProviderServiceId));
        }

		return new ChannelStore("channel-store",
                                fieldList,
                                parseSortColumn(config, storeFieldNames),
                                parseSortOrder(config),
                                new LocaleResolver(localeProvider),
                                channelService,
                                blueprintService);

    }

    public static ChannelStore createStore(IPluginContext context, IPluginConfig config) {
    	return createStore(context, config, null, null);
    }

    static Set<String> parseChannelFields(IPluginConfig config) {
        Set<String> storeFieldNames = new HashSet<String>();

        // create an intermediate set of all unique field names to put in the ExtJS store: a union of all default
        // channel fields and the visible fields (since the latter may also include custom channel properties)
        for (ChannelStore.ChannelField channelField : ChannelStore.ChannelField.values()) {
            storeFieldNames.add(channelField.name());
        }

        String[] columnNames = config.getStringArray(CONFIG_COLUMNS);
        if (columnNames != null && columnNames.length > 0) {
            storeFieldNames.addAll(Arrays.asList(columnNames));
        }
        if (!storeFieldNames.contains(ChannelStore.ChannelField.name.name())) {
            storeFieldNames.add(ChannelStore.ChannelField.name.name());
        }
        return storeFieldNames;
    }


    static String parseSortColumn(IPluginConfig config, Set<String> columnNames) {
        if (config == null || columnNames.size() == 0) {
            return ChannelStore.ChannelField.name.name();
        }

        String configSortColumn = config.getString(CONFIG_SORT_COLUMN);
        if (columnNames.contains(configSortColumn)) {
            return configSortColumn;
        }

        return ChannelStore.ChannelField.name.name();
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


}
