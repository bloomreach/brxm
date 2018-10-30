/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.RESOLVED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.UNRESOLVED;

public abstract class ExportImportTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File resources;
    private Registrar registrar;

    @Before
    public void setupTestModule() throws IOException {
        final File module = temporaryFolder.newFolder("module");
        resources = temporaryFolder.newFolder("module", "resources");
        final ClassLoader classLoader = getClass().getClassLoader();
        new Extractor(resources, "module", Arrays.asList("en", "nl", "fr"), classLoader, new String[] {}).extract();
        registrar = new Registrar(module, "module", Arrays.asList("nl", "fr"), classLoader, new String[] {});
        registrar.initialize();
        final File pom = temporaryFolder.newFile("module/pom.xml");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("pom.xml"), pom);
        changeBundle();
        registrar.update();
    }

    private void changeBundle() throws IOException {
        ResourceBundle resourceBundle = registrar.getRegistry().getResourceBundle(null, "angular/dummy/i18n/en.json", BundleType.ANGULAR);
        resourceBundle.getEntries().put("key", String.valueOf(System.currentTimeMillis()));
        resourceBundle.save();

        resourceBundle = registrar.getRegistry().getResourceBundle("bundle", "dummy-repository-translations_en.yaml", BundleType.REPOSITORY);
        resourceBundle.getEntries().put("key", String.valueOf(System.currentTimeMillis()));
        resourceBundle.save();
    }

    private Exporter getExporter(File baseDir) {
        return new Exporter(baseDir, getExportFileWriter());
    }

    private Importer getImporter(File baseDir) {
        return new Importer(baseDir, getImportFileReader());
    }

    abstract ExportFileWriter getExportFileWriter();

    abstract ImportFileReader getImportFileReader();

    @Test
    public void testExporter() throws Exception {
        final File export = getExporter(temporaryFolder.getRoot()).export("nl");
        final List<String[]> records = getImportFileReader().read(export);

        assertEquals(4, records.size());

        // first record in the export set is the header

        // this record is due to the change in the resource bundle angular/dummy/i18n/en.json
        String[] record = getRecord(records, "module#angular/dummy/i18n/registry.json#key");
        assertNotNull(record);
        assertEquals(3, record.length);
        assertEquals("value", record[1]);
        assertEquals("waarde", record[2]);

        // this record is due to the change in the resource bundle dummy-repository-translations_en.json
        record = getRecord(records, "module#dummy-repository-translations.registry.json#bundle/key");
        assertNotNull(record);
        assertEquals(3, record.length);
        assertEquals("value", record[1]);
        assertEquals("waarde", record[2]);

        // this record is due to the fact that the translation is missing in the source files
        record = getRecord(records, "module#org/onehippo/cms/l10n/test/DummyWicketPlugin.registry.json#key_en_only");
        assertNotNull(record);
        assertEquals(3, record.length);
        assertEquals("missing in fr and nl", record[1]);
        assertEquals("", record[2]);
    }

    private String[] getRecord(final List<String[]> records, final String key) {
        for (String[] record : records) {
            if (record[0].equals(key)) {
                return record;
            }
        }
        return null;
    }

    @Test
    public void testImporter() throws Exception {
        final RegistryInfo angularRegistryInfo = registrar.getRegistry().getRegistryInfo("angular/dummy/i18n/registry.json");
        final RegistryInfo repositoryRegistryInfo = registrar.getRegistry().getRegistryInfo("dummy-repository-translations.registry.json");
        
        verifyInitialStatus : {
            KeyData keyData = angularRegistryInfo.getKeyData("key");
            assertNotNull(keyData);
            assertEquals(UPDATED, keyData.getStatus());
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("nl"));
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("fr"));
            
            keyData = repositoryRegistryInfo.getKeyData("bundle/key");
            assertNotNull(keyData);
            assertEquals(UPDATED, keyData.getStatus());
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("nl"));
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("fr"));
        }

        importDutch: {
            File export = getExporter(temporaryFolder.getRoot()).export("nl");
            File _import = temporaryFolder.newFile("importNL");
            FileUtils.copyFile(export, _import);
            getImporter(temporaryFolder.getRoot())._import("importNL", "nl");
        }

        assertDutchIsResolved: {
            assertDutchIsResolvedAndFrenchIsUnresolved(angularRegistryInfo, repositoryRegistryInfo);
        }
        
        importFrench: {
            File export = getExporter(temporaryFolder.getRoot()).export("fr");
            File _import = temporaryFolder.newFile("importFR");
            FileUtils.copyFile(export, _import);
            // change the reference bundles in the mean time
            changeBundle();
            getImporter(temporaryFolder.getRoot())._import("importFR", "fr");
        }

        assertFrenchIsUnresolved: {
            assertDutchIsResolvedAndFrenchIsUnresolved(angularRegistryInfo, repositoryRegistryInfo);
        }
        
    }

    private void assertDutchIsResolvedAndFrenchIsUnresolved(final RegistryInfo angularRegistryInfo, final RegistryInfo repositoryRegistryInfo) throws IOException {
        angularRegistryInfo.load();
        KeyData keyData = angularRegistryInfo.getKeyData("key");
        assertNotNull(keyData);
        assertEquals(UPDATED, keyData.getStatus());
        assertEquals(RESOLVED, keyData.getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, keyData.getLocaleStatus("fr"));

        repositoryRegistryInfo.load();
        keyData = repositoryRegistryInfo.getKeyData("bundle/key");
        assertNotNull(keyData);
        assertEquals(UPDATED, keyData.getStatus());
        assertEquals(RESOLVED, keyData.getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, keyData.getLocaleStatus("fr"));
    }

}
