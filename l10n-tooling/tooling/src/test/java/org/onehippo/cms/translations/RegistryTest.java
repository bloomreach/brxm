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
package org.onehippo.cms.translations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms.translations.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.RESOLVED;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.UNRESOLVED;

public class RegistryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Collection<String> extractorLocales = Arrays.asList("en", "nl", "fr");
    private Collection<String> registrarLocales = Arrays.asList("nl", "fr");

    private int getPendingTranslationCount(Registry registry) throws IOException {
        int count = 0;
        for (RegistryFile file : registry.listRegistryFiles()) {
            file.load();
            for (String key : file.getKeys()) {
                KeyData keyData = file.getKeyData(key);
                for (String locale : registrarLocales) {
                    if (keyData.getLocaleStatus(locale) != RESOLVED) {
                        count++;
                        System.out.println(
                                "fileId: " + file.getId()
                                + " key: " + key
                                + " locale: " + locale);
                    }
                }
            }
        }

        return count;
    }

    @Test
    public void running_initialize_before_extract_results_in_10_changes() throws IOException {
        Registrar registrar = new Registrar(temporaryFolder.getRoot(), registrarLocales);
        registrar.initializeRegistry();
        registrar.updateRegistry();

        assertEquals(10, getPendingTranslationCount(registrar.getRegistry()));

        RegistryFile registryFile = registrar.getRegistry().loadRegistryFile("angular/dummy/i18n/registry.json");
        assertEquals(UNRESOLVED, registryFile.getKeyData("key").getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key").getLocaleStatus("fr"));

        registryFile = registrar.getRegistry().loadRegistryFile("dummy-repository-translations.registry.json");
        assertEquals(UNRESOLVED, registryFile.getKeyData("dummybundles/key").getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("dummybundles/key").getLocaleStatus("fr"));

        registryFile = registrar.getRegistry().loadRegistryFile("org/onehippo/cms/translations/test/DummyWicketPlugin.registry.json");
        assertEquals(UNRESOLVED, registryFile.getKeyData("key").getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key").getLocaleStatus("fr"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key_en_nl").getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key_en_nl").getLocaleStatus("fr"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key_en_only").getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key_en_only").getLocaleStatus("fr"));
    }

    @Test
    public void running_initialize_after_extract_results_in_4_changes() throws IOException {
        Extractor extractor = new Extractor(temporaryFolder.getRoot(), extractorLocales);
        extractor.extract();

        Registrar registrar = new Registrar(temporaryFolder.getRoot(), registrarLocales);
        registrar.initializeRegistry();
        registrar.updateRegistry();

        assertEquals(4, getPendingTranslationCount(registrar.getRegistry()));

        RegistryFile registryFile = registrar.getRegistry().loadRegistryFile("angular/dummy/i18n/registry.json");
        assertEquals(UNRESOLVED, registryFile.getKeyData("key").getLocaleStatus("fr"));

        registryFile = registrar.getRegistry().loadRegistryFile("org/onehippo/cms/translations/test/DummyWicketPlugin.registry.json");
        assertEquals(UNRESOLVED, registryFile.getKeyData("key_en_nl").getLocaleStatus("fr"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key_en_only").getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, registryFile.getKeyData("key_en_only").getLocaleStatus("fr"));
    }

    @Test
    public void test_update_is_registered() throws IOException {
        // extract translations, reference sets and initialize register
        Extractor extractor = new Extractor(temporaryFolder.getRoot(), extractorLocales);
        extractor.extract();

        Registrar registrar = new Registrar(temporaryFolder.getRoot(), registrarLocales);
        registrar.initializeRegistry();

        int initialChanges = getPendingTranslationCount(registrar.getRegistry());

        // to fake an update in the reference language, modify a saved reference set
        Properties properties = new Properties();
        properties.setProperty("key", "foo");

        ResourceBundle repositoryBundle = new RepositoryResourceBundle("dummybundles", "dummy-repository-translations_en.json", new ArtifactInfo(""), "en", properties);
        RepositoryResourceBundleSerializer repositoryResourceBundleSerializer = new RepositoryResourceBundleSerializer(temporaryFolder.getRoot());
        repositoryResourceBundleSerializer.serializeBundle(repositoryBundle);

        // update the register
        registrar.updateRegistry();

        // validate the number of additional changes
        assertEquals(2, getPendingTranslationCount(registrar.getRegistry()) - initialChanges);

        // validate the reference was correctly updated
        repositoryBundle = repositoryResourceBundleSerializer.deserializeBundle(repositoryBundle.getFileName(), repositoryBundle.getName(), repositoryBundle.getLocale());
        RegistryFile registryFile = registrar.getRegistry().loadRegistryFile(repositoryBundle);
        final KeyData keyData = registryFile.getKeyData("dummybundles/key");
        assertEquals(UPDATED, keyData.getStatus());
        assertEquals(2, keyData.getLocales().size());
        assertEquals(UNRESOLVED, keyData.getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, keyData.getLocaleStatus("fr"));
        assertEquals("value", repositoryBundle.getEntries().get("key"));
    }

    @Test
    public void test_deleted_keys_are_registered() throws IOException {
        // extract translations, reference sets and initialize register
        Extractor extractor = new Extractor(temporaryFolder.getRoot(), extractorLocales);
        extractor.extract();

        Registrar registrar = new Registrar(temporaryFolder.getRoot(), registrarLocales);
        registrar.initializeRegistry();

        // add some dummy keys to the existing "dummy" resource bundle and introduce a new "new" resource bundle
        final String fileName1 = "angular/dummy/i18n/registry.json";
        RegistryFile registryFile = registrar.getRegistry().loadRegistryFile(fileName1);
        KeyData data = new KeyData();
        data.setStatus(KeyData.KeyStatus.CLEAN);
        registryFile.putKeyData("key", data);
        registryFile.putKeyData("key2", data);
        registryFile.save();

        final String fileName2 = "angular/new/i18n/registry.json";
        registryFile = registrar.getRegistry().loadRegistryFile(fileName2);
        data = new KeyData();
        data.setStatus(KeyData.KeyStatus.CLEAN);
        registryFile.putKeyData("key", data);
        registryFile.putKeyData("key2", data);
        registryFile.save();

        // update the registry
        registrar.updateRegistry();

        // validate overall changes
        assertEquals(3, getPendingTranslationCount(registrar.getRegistry()));

        // validate the register was correctly updated
        registryFile = registrar.getRegistry().loadRegistryFile(fileName1);
        assertEquals(KeyData.KeyStatus.CLEAN, registryFile.getKeyData("key").getStatus());
        assertEquals(KeyData.KeyStatus.DELETED, registryFile.getKeyData("key2").getStatus());

        registryFile = registrar.getRegistry().loadRegistryFile(fileName2);
        assertEquals(KeyData.KeyStatus.DELETED, registryFile.getKeyData("key").getStatus());
        assertEquals(KeyData.KeyStatus.DELETED, registryFile.getKeyData("key2").getStatus());
    }

    @Test
    public void testResourceBundleToRegistryFileMapping() {
        Registry registry = new Registry(temporaryFolder.getRoot());
        String registryFile = registry.mapResourceBundleToRegistryFile(new AngularResourceBundle(null, "/angular/project/app/i18n/en.json", null, "en", null));
        assertEquals("/angular/project/app/i18n/registry.json", registryFile);
        registryFile = registry.mapResourceBundleToRegistryFile(new WicketResourceBundle(null, "org/example/TestPlugin.properties", null, "en", null));
        assertEquals("org/example/TestPlugin.registry.json", registryFile);
        registryFile = registry.mapResourceBundleToRegistryFile(new RepositoryResourceBundle(null, "example-translations_en.json", null, "en", null));
        assertEquals("example-translations.registry.json", registryFile);
    }

    @Test
    public void testRegistryFileToResourceBundleFilesMapping() throws IOException {
        Extractor extractor = new Extractor(temporaryFolder.getRoot(), Arrays.asList("en", "nl"));
        extractor.extract();
        Registrar registrar = new Registrar(temporaryFolder.getRoot(), Arrays.asList("en", "nl"));
        registrar.updateRegistry();
        final Registry registry = registrar.getRegistry();
        String bundleFileName = registry.mapRegistryFileToResourceBundleFile("angular/dummy/i18n/registry.json", BundleType.ANGULAR, "nl");
        assertEquals("angular/dummy/i18n/nl.json", bundleFileName);
        bundleFileName = registry.mapRegistryFileToResourceBundleFile("com/onehippo/cms7/localizer/test/DummyWicketPlugin.registry.json", BundleType.WICKET, "nl");
        assertEquals("com/onehippo/cms7/localizer/test/DummyWicketPlugin_nl.properties", bundleFileName);
        bundleFileName = registry.mapRegistryFileToResourceBundleFile("dummy-repository-translations.registry.json", BundleType.REPOSITORY, "nl");
        assertEquals("dummy-repository-translations_nl.json", bundleFileName);
    }

}
