package org.onehippo.cms7.channelmanager.channels;

import java.util.Arrays;
import java.util.List;

import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ChannelGridPanelTest {

    @Test
    public void columnsForNonExistingChannelFieldsAreIgnored() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("columns", new String[]{"id", "nosuchfield", "name"});

        List<ChannelStore.Column> parsedFields = ChannelGridPanel.parseChannelFields(config);
        assertEquals(2, parsedFields.size());
        assertTrue(parsedFields.contains(ChannelStore.Column.id));
        assertTrue(parsedFields.contains(ChannelStore.Column.name));
    }

    @Test
    public void noColumnsAfterIgnoringUsesDefault() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("columns", new String[]{"", "nosuchfield"});
        testIsDefaultColumns(ChannelGridPanel.parseChannelFields(config));
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

    private void testIsDefaultColumns(List<ChannelStore.Column> fields) {
        assertTrue(fields.contains(ChannelStore.Column.name));
    }

    @Test
    public void sortColumnForExistingColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.column", "url");
        assertEquals("url", ChannelGridPanel.parseSortColumn(config, Arrays.asList(ChannelStore.Column.url)));
    }

    @Test
    public void sortColumnForNonExistingColumnIsFirstColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.column", "url");
        assertEquals("hostname", ChannelGridPanel.parseSortColumn(config, Arrays.asList(ChannelStore.Column.hostname, ChannelStore.Column.id)));
    }

    @Test
    public void emptySortColumnIsFirstColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        assertEquals("url", ChannelGridPanel.parseSortColumn(config, Arrays.asList(ChannelStore.Column.url, ChannelStore.Column.hostname)));
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
