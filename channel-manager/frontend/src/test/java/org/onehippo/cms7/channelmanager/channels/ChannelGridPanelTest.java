package org.onehippo.cms7.channelmanager.channels;

import java.util.Arrays;
import java.util.List;

import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ChannelGridPanelTest {

    @Test
    public void columnsForExistingChannelFields() {
        String[] existingChannelFields = new String[]{
                "blueprintId",
                "composerModeEnabled",
                "contentRoot",
                "hostname",
                "hstMountPoint",
                "hstConfigPath",
                "id",
                "name",
                "subMountPath",
                "type",
                "url"
        };
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("columns", existingChannelFields);

        List<String> parsedFields = ChannelGridPanel.parseChannelFields(config);
        assertEquals(existingChannelFields.length, parsedFields.size());
        for (int i = 0; i < existingChannelFields.length; i++) {
            assertEquals(existingChannelFields[i], parsedFields.get(i));
        }
    }

    @Test
    public void columnsForNonGetterMethodsAreIgnored() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("columns", new String[]{"string", "hashCode"});

        List<String> parsedFields = ChannelGridPanel.parseChannelFields(config);

        assertFalse(parsedFields.contains("string"));
        assertFalse(parsedFields.contains("hashCode"));
    }

    @Test
    public void columnsForNonExistingChannelFieldsAreIgnored() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("columns", new String[]{"id", "nosuchfield", "name"});

        List<String> parsedFields = ChannelGridPanel.parseChannelFields(config);
        assertEquals(2, parsedFields.size());
        assertEquals("id", parsedFields.get(0));
        assertEquals("name", parsedFields.get(1));
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

    private void testIsDefaultColumns(List<String> fields) {
        assertEquals(1, fields.size());
        assertTrue(fields.contains("name"));
    }

    @Test
    public void sortColumnForExistingColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.column", "url");

        List<String> columns = Arrays.asList("url");

        assertEquals("url", ChannelGridPanel.parseSortColumn(config, columns));
    }

    @Test
    public void sortColumnForNonExistingColumnIsFirstColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        config.put("sort.column", "url");

        List<String> columns = Arrays.asList("hostname", "blueprintId");

        assertEquals("hostname", ChannelGridPanel.parseSortColumn(config, columns));
    }

    @Test
    public void emptySortColumnIsFirstColumn() {
        JavaPluginConfig config = new JavaPluginConfig();
        List<String> columns = Arrays.asList("url", "hostname");
        assertEquals("url", ChannelGridPanel.parseSortColumn(config, columns));
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
