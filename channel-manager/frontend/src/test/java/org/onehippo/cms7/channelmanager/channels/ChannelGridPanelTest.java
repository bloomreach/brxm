package org.onehippo.cms7.channelmanager.channels;

import java.util.Arrays;
import java.util.Collections;
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
        assertEquals(ChannelStore.Column.id.name(), parsedFields.get(0));
        assertEquals("customChannelProperty", parsedFields.get(1));
        assertEquals(ChannelStore.Column.name.name(), parsedFields.get(2));
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
        for (ChannelStore.Column column : ChannelStore.Column.values()) {
            assertTrue(fields.contains(column.name()));
        }
    }

    @Test
    public void sortColumnForExistingColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.column", "url");
        assertEquals("url", ChannelGridPanel.parseSortColumn(config, Collections.singletonList(ChannelStore.Column.url.name())));
    }

    @Test
    public void sortColumnForNonExistingColumnIsFirstColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.column", "url");
        assertEquals("hostname", ChannelGridPanel.parseSortColumn(config, Arrays.asList(ChannelStore.Column.hostname.name(), ChannelStore.Column.id.name())));
    }

    @Test
    public void emptySortColumnIsFirstColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        assertEquals("url", ChannelGridPanel.parseSortColumn(config, Arrays.asList(ChannelStore.Column.url.name(), ChannelStore.Column.hostname.name())));
    }

    @Test
    public void nullConfigurationUsesSortColumnName() {
        assertEquals("name", ChannelGridPanel.parseSortColumn(null, null));
    }

    @Test
    public void sortOrderAscending() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.order", "ascending");
        assertEquals(ChannelStore.SortOrder.ascending, ChannelGridPanel.parseSortOrder(config));
    }

    @Test
    public void sortOrderDescending() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.order", "ascending");
        assertEquals(ChannelStore.SortOrder.ascending, ChannelGridPanel.parseSortOrder(config));
    }

    @Test
    public void unknownSortOrderIsAscending() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.order", "nosuchsortorder");
        assertEquals(ChannelStore.SortOrder.ascending, ChannelGridPanel.parseSortOrder(config));
    }

    @Test
    public void emptySortOrderIsAscending() {
        JavaPluginConfig emptyConfig = new JavaPluginConfig();
        assertEquals(ChannelStore.SortOrder.ascending, ChannelGridPanel.parseSortOrder(emptyConfig));
    }

    @Test
    public void nullConfigurationUsesAscending() {
        assertEquals(ChannelStore.SortOrder.ascending, ChannelGridPanel.parseSortOrder(null));
    }

}
