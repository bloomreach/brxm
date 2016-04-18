/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.CLEAN;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.RESOLVED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.UNRESOLVED;

public class ExportImportTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File resources;
    private Registrar registrar;

    @Before
    public void setupTestModule() throws IOException {
        temporaryFolder.newFolder("module");
        resources = temporaryFolder.newFolder("module/resources");
        new Extractor(resources, "module", Arrays.asList("en", "nl")).extract();
        registrar = new Registrar(resources, Collections.singletonList("nl"));
        registrar.initialize();
        final File pom = temporaryFolder.newFile("module/pom.xml");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("pom.xml"), pom);
        changeBundle();
        registrar.update();
    }

    private void changeBundle() throws IOException {
        ResourceBundle resourceBundle = registrar.getRegistry().getResourceBundle(null, "angular/dummy/i18n/en.json", BundleType.ANGULAR);
        resourceBundle.getEntries().put("key", "value2");
        resourceBundle.save();

        resourceBundle = registrar.getRegistry().getResourceBundle("bundle", "dummy-repository-translations_en.json", BundleType.REPOSITORY);
        resourceBundle.getEntries().put("key", "value2");
        resourceBundle.save();
    }

    @Test
    public void testExporter() throws Exception {
        final File export = new Exporter(temporaryFolder.getRoot(), "Default").export("nl");
        final CSVParser parser = new CSVParser(new FileReader(export), CSVFormat.DEFAULT);
        final List<CSVRecord> records = parser.getRecords();
        assertEquals(4, records.size());

        // first record in the export set is the header

        // this record is due to the change in the resource bundle angular/dummy/i18n/en.json
        CSVRecord record = records.get(1);
        assertEquals(3, record.size());
        assertEquals("value", record.get(1));
        assertEquals("waarde", record.get(2));

        // this record is due to the change in the resource bundle dummy-repository-translations_en.json
        record = records.get(2);
        assertEquals(3, record.size());
        assertEquals("value", record.get(1));
        assertEquals("waarde", record.get(2));

        // this record is due to the fact that the translation is missing in the source files
        record = records.get(3);
        assertEquals(3, record.size());
        assertEquals("missing in fr and nl", record.get(1));
        assertEquals("", record.get(2));
    }

    @Test
    public void testImporter() throws Exception {
        final RegistryInfo angularRegistryInfo = registrar.getRegistry().getRegistryInfo("angular/dummy/i18n/registry.json");
        KeyData keyData = angularRegistryInfo.getKeyData("key");
        assertNotNull(keyData);
        assertEquals(UPDATED, keyData.getStatus());
        assertEquals(UNRESOLVED, keyData.getLocaleStatus("nl"));

        final RegistryInfo repositoryRegistryInfo = registrar.getRegistry().getRegistryInfo("dummy-repository-translations.registry.json");
        keyData = repositoryRegistryInfo.getKeyData("bundle/key");
        assertNotNull(keyData);
        assertEquals(UPDATED, keyData.getStatus());
        assertEquals(UNRESOLVED, keyData.getLocaleStatus("nl"));

        final File export = new Exporter(temporaryFolder.getRoot(), "Default").export("nl");
        final File _import = temporaryFolder.newFile("import.csv");
        FileUtils.copyFile(export, _import);
        new Importer(temporaryFolder.getRoot(), "Default")._import("import.csv", "nl");

        angularRegistryInfo.load();
        keyData = angularRegistryInfo.getKeyData("key");
        assertNotNull(keyData);
        assertEquals(CLEAN, keyData.getStatus());
        assertEquals(RESOLVED, keyData.getLocaleStatus("nl"));

        repositoryRegistryInfo.load();
        keyData = repositoryRegistryInfo.getKeyData("bundle/key");
        assertNotNull(keyData);
        assertEquals(CLEAN, keyData.getStatus());
        assertEquals(RESOLVED, keyData.getLocaleStatus("nl"));
    }

}
