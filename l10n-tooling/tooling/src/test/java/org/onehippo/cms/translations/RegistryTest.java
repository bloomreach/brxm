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
import java.util.Properties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class RegistryTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private int getChangeCount(Registry registry) throws IOException {
        int count = 0;
        for (RegistryFile file : registry.listRegistryFiles()) {
            file.load();
            for (String key : file.getKeys()) {
                if (file.getKeyData(key).getStatus() != KeyData.KeyStatus.CLEAN) {
                    count++;
                }
            }
        }

        return count;
    }

    @Test
    public void running_registrar_before_extract_results_in_3_changes() throws IOException {
        Registrar registrar = new Registrar(temporaryFolder.getRoot(), Arrays.asList("de", "fr"));
        registrar.initializeRegistry();
        registrar.updateRegistry();

        assertEquals(3, getChangeCount(registrar.getRegistry()));
    }

    @Test
    public void running_registrar_after_extract_results_in_0_changes() throws IOException {
        Extractor extractor = new Extractor(temporaryFolder.getRoot(), Arrays.asList("en", "de", "fr"));
        extractor.extract();

        Registrar registrar = new Registrar(temporaryFolder.getRoot(), Arrays.asList("de", "fr"));
        registrar.initializeRegistry();
        registrar.updateRegistry();

        assertEquals(0, getChangeCount(registrar.getRegistry()));
    }

    @Test
    public void test_update_is_registered() throws IOException {
        // extract translations, reference sets and initialize register
        Extractor extractor = new Extractor(temporaryFolder.getRoot(), Arrays.asList("en", "de", "fr"));
        extractor.extract();

        Registrar registrar = new Registrar(temporaryFolder.getRoot(), Arrays.asList("de", "fr"));
        registrar.initializeRegistry();

        // modify a saved reference set
        Properties properties = new Properties();
        properties.setProperty("key", "foo");

        ResourceBundle repositoryBundle = new RepositoryResourceBundle("dummybundles", "dummy-repository-translations.json", new ArtifactInfo(""), "en", properties);
        RepositoryResourceBundleSerializer repositoryResourceBundleSerializer = new RepositoryResourceBundleSerializer(temporaryFolder.getRoot());
        repositoryResourceBundleSerializer.serializeBundle(repositoryBundle);

        // update the register
        registrar.updateRegistry();

        // validate overall changes
        assertEquals(1, getChangeCount(registrar.getRegistry()));

        // validate the reference was correctly updated
        final String bundleFileName = TranslationsUtils.getLocalizedBundleFileName(repositoryBundle.getFileName(), BundleType.REPOSITORY, repositoryBundle.getLocale());
        repositoryBundle = repositoryResourceBundleSerializer.deserializeBundle(bundleFileName, repositoryBundle.getName(), repositoryBundle.getLocale());
        RegistryFile registryFile = registrar.getRegistry().loadRegistryFile(repositoryBundle);
        Assert.assertEquals(KeyData.KeyStatus.UPDATED, registryFile.getKeyData("dummybundles/key").getStatus());
        assertEquals("value", repositoryBundle.getEntries().get("key"));
    }

    @Test
    public void test_deleted_keys_are_registered() throws IOException {
        // extract translations, reference sets and initialize register
        Extractor extractor = new Extractor(temporaryFolder.getRoot(), Arrays.asList("en", "de", "fr"));
        extractor.extract();

        Registrar registrar = new Registrar(temporaryFolder.getRoot(), Arrays.asList("de", "fr"));
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
        assertEquals(3, getChangeCount(registrar.getRegistry()));

        // validate the register was correctly updated
        registryFile = registrar.getRegistry().loadRegistryFile(fileName1);
        Assert.assertEquals(KeyData.KeyStatus.CLEAN, registryFile.getKeyData("key").getStatus());
        Assert.assertEquals(KeyData.KeyStatus.DELETED, registryFile.getKeyData("key2").getStatus());

        registryFile = registrar.getRegistry().loadRegistryFile(fileName2);
        Assert.assertEquals(KeyData.KeyStatus.DELETED, registryFile.getKeyData("key").getStatus());
        Assert.assertEquals(KeyData.KeyStatus.DELETED, registryFile.getKeyData("key2").getStatus());
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
