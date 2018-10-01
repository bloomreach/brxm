package org.onehippo.cm.engine.autoexport;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import com.google.common.collect.ImmutableList;

import static org.junit.Assert.*;

public class AutoExportConfigTest {

    @Test
    public void processModuleStrings_no_root_mapping() {
        Map<String, Pair<String, Collection<String>>> modules;
        List<String> moduleStrings = ImmutableList.of(
                "repository-data/development",
                "repository-data/site:/hst:mysite");
        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(AutoExportConstants.LOGGER_NAME).build()) {
            modules = AutoExportConfig.processModuleStrings(moduleStrings,true);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Misconfiguration of autoexport:modules property: " +
                    "there must be a module that maps to /")));
        }
        assertEquals(0, modules.size());
    }

    @Test
    public void processModuleStrings_multiple_same_mappings() {
        Map<String, Pair<String, Collection<String>>> modules;
        final List<String> moduleStrings = ImmutableList.of(
                "repository-data/application:/",
                "repository-data/development:/hst:mysite",
                "repository-data/site:mysite:/hst:mysite");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onError().trap(AutoExportConstants.LOGGER_NAME).build()) {
            modules = AutoExportConfig.processModuleStrings(moduleStrings,true);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Misconfiguration of autoexport:modules property: " +
                    "the same repository path /hst:mysite may not be mapped to multiple modules")));
        }
        assertEquals(0, modules.size());
    }

    @Test
    public void processModuleStrings_site_without_mapping() {
        Map<String, Pair<String, Collection<String>>> modules;
        final List<String> moduleStrings = ImmutableList.of(
                "repository-data/application:/",
                "repository-data/site:mysite");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(AutoExportConstants.LOGGER_NAME).build()) {
            modules = AutoExportConfig.processModuleStrings(moduleStrings,true);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Changes to repository path '/' will be exported " +
                    "to directory 'repository-data/application'")));
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Module at 'repository-data/site' registered to " +
                    "update existing definitions via auto-export without mapping to a repository path")));
        }

        assertEquals(2, modules.size());

        assertTrue(modules.containsKey("repository-data/site"));
        assertEquals("mysite", modules.get("repository-data/site").getKey());
        assertEquals(0, modules.get("repository-data/site").getValue().size());
    }


    @Test
    public void processModuleStrings() {
        Map<String, Pair<String, Collection<String>>> modules;
        final List<String> moduleStrings = ImmutableList.of(
                "repository-data/application:/",
                "repository-data/development",
                "repository-data/site:mysite:/hst:mysite");

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(AutoExportConstants.LOGGER_NAME).build()) {
            modules = AutoExportConfig.processModuleStrings(moduleStrings, true);
            assertTrue(interceptor.messages().anyMatch(m -> m.equals("Changes to repository path '/hst:mysite' will " +
                    "be exported to directory 'repository-data/site'")));
        }

        assertEquals(3, modules.size());

        assertNull(modules.get("repository-data/application").getKey());
        assertEquals("/", modules.get("repository-data/application").getValue().iterator().next());

        assertNull(modules.get("repository-data/development").getKey());
        assertEquals(0, modules.get("repository-data/development").getValue().size());

        assertEquals("mysite", modules.get("repository-data/site").getKey());
        assertEquals("/hst:mysite", modules.get("repository-data/site").getValue().iterator().next());
    }

}