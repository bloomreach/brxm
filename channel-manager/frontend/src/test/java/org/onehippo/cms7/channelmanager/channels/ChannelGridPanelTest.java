/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashSet;
import java.util.List;

import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ChannelGridPanel}.
 */
public class ChannelGridPanelTest {

    @Test
    public void allColumnFieldsAreParsed() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("columns", new String[]{"id", "customChannelProperty", "name"});

        List<String> parsedFields = ChannelGridPanel.parseChannelFields(config);
        assertEquals(3, parsedFields.size());
        assertEquals(ChannelStore.ChannelField.id.name(), parsedFields.get(0));
        assertEquals("customChannelProperty", parsedFields.get(1));
        assertEquals(ChannelStore.ChannelField.name.name(), parsedFields.get(2));
    }

    @Test
    public void nullConfigurationUsesDefault() {
        testIsDefaultColumns(ChannelGridPanel.parseChannelFields(null));
    }

    @Test
    public void missingColumnPropertyUsesDefault() {
        testIsDefaultColumns(ChannelGridPanel.parseChannelFields(new JavaPluginConfig()));
    }

    @Test
    public void emptyColumnPropertyUsesDefault() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("columns", new String[]{});
        testIsDefaultColumns(ChannelGridPanel.parseChannelFields(config));
    }

    private void testIsDefaultColumns(List<String> fields) {
        for (ChannelStore.ChannelField column : ChannelStore.ChannelField.values()) {
            assertTrue(fields.contains(column.name()));
        }
    }

    @Test
    public void sortColumnForExistingColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.column", "url");
        assertEquals("url", ChannelStoreFactory.parseSortColumn(config, new HashSet<String>() {
            { add(ChannelStore.ChannelField.url.name()); }
        }));
    }

    @Test
    public void nullConfigurationUsesSortColumnName() {
        assertEquals("name", ChannelStoreFactory.parseSortColumn(null, null));
    }

    @Test
    public void sortOrderAscending() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.order", "ascending");
        assertEquals(ChannelStore.SortOrder.ascending, ChannelStoreFactory.parseSortOrder(config));
    }

    @Test
    public void sortOrderDescending() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.order", "ascending");
        assertEquals(ChannelStore.SortOrder.ascending, ChannelStoreFactory.parseSortOrder(config));
    }

    @Test
    public void unknownSortOrderIsAscending() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.order", "nosuchsortorder");
        assertEquals(ChannelStore.SortOrder.ascending, ChannelStoreFactory.parseSortOrder(config));
    }

    @Test
    public void emptySortOrderIsAscending() {
        JavaPluginConfig emptyConfig = new JavaPluginConfig();
        assertEquals(ChannelStore.SortOrder.ascending, ChannelStoreFactory.parseSortOrder(emptyConfig));
    }

    @Test
    public void nullConfigurationUsesAscending() {
        assertEquals(ChannelStore.SortOrder.ascending, ChannelStoreFactory.parseSortOrder(null));
    }

}
