/*
 *  Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtDataField;

public final class ChannelStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(ChannelStore.class);

    public static final String CONFIG_COLUMNS = "columns";
    public static final String CONFIG_SORT_COLUMN = "sort.column";
    public static final String CONFIG_SORT_ORDER = "sort.order";
    public static final String CONFIG_CHANNEL_STORE = "channelStore";

    private ChannelStoreFactory() {
        // prevent instantiation
    }

    public static ChannelStore createStore(final IPluginContext context,
                                           final IPluginConfig config,
                                           final BlueprintStore blueprintStore) {
        Set<String> storeFieldNames = parseChannelFields(config);

        // then create a list of all the Ext fields in the store
        List<ExtDataField> fieldList = new ArrayList<>();
        for (String storeFieldName : storeFieldNames) {
            fieldList.add(new ExtDataField(storeFieldName));
        }

        // Retrieve the Hippo locale provider to resolve locales of new channels
        String localeProviderServiceId = config.getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName());
        ILocaleProvider localeProvider = context.getService(localeProviderServiceId, ILocaleProvider.class);
        if (localeProvider == null) {
            throw new IllegalStateException(
                    String.format("Cannot find locale provider service with ID '%s'", localeProviderServiceId));
        }

        final ChannelStore channelStore = build(config, "channel-store", fieldList,
                parseSortColumn(config, storeFieldNames), parseSortOrder(config),
                new LocaleResolver(localeProvider), blueprintStore);

        if (config.containsKey("channelRegionIconPath")) {
            channelStore.setChannelRegionIconPath(config.getString("channelRegionIconPath"));
        }
        if (config.containsKey("channelTypeIconPath")) {
            channelStore.setChannelTypeIconPath(config.getString("channelTypeIconPath"));
        }

        return channelStore;
    }

    static ChannelStore build(IPluginConfig config, String storeId, List<ExtDataField> fieldList, String sortColumn, ChannelStore.SortOrder sortOrder, LocaleResolver localeResolver, BlueprintStore blueprintStore){
        String channelStoreClassName = config.getString(CONFIG_CHANNEL_STORE);
        if(StringUtils.isNotBlank(channelStoreClassName)){
            try {
                // TODO CHANNELMGR-1949 get rid of this extensibility?
                Class<?> customChannelStore = Class.forName(channelStoreClassName);
                Constructor<?> channelStoreConstructor = customChannelStore.getConstructor(String.class, List.class, String.class, ChannelStore.SortOrder.class, LocaleResolver.class, BlueprintStore.class);
                return (ChannelStore) channelStoreConstructor.newInstance(storeId, fieldList, sortColumn, sortOrder, localeResolver, blueprintStore);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | ClassCastException e) {
                log.error("Could not instantiate custom channel store: {}", channelStoreClassName, e);
            }
        }
        return new ChannelStore("channel-store", fieldList, sortColumn, sortOrder, localeResolver, blueprintStore);
    }

    static Set<String> parseChannelFields(IPluginConfig config) {
        Set<String> storeFieldNames = new HashSet<>();

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
        if (config == null || columnNames.isEmpty()) {
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
