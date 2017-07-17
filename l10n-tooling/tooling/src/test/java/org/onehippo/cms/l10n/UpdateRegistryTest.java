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
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms.l10n.BundleType.REPOSITORY;
import static org.onehippo.cms.l10n.BundleType.WICKET;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.CLEAN;
import static org.onehippo.cms.l10n.KeyData.KeyStatus.UPDATED;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.UNRESOLVED;

public class UpdateRegistryTest extends RegistrarTest {
    
    @Test
    public void changedKeysAreUpdatedInRegistryAndReferenceBundle() throws IOException {
        final String angularBundleFileName = "angular/dummy/i18n/en.json";
        final AngularResourceBundle angularResourceBundle;
        final int initialPendingTranslationCount;

        // change the reference bundle to fake an incoming change
        setup: {
            initialPendingTranslationCount = getPendingTranslationCount(registry);
            angularResourceBundle = new AngularResourceBundle(null, angularBundleFileName, 
                    new File(resources, angularBundleFileName));
            angularResourceBundle.load();
            angularResourceBundle.getEntries().put("key", "changedValue");
            angularResourceBundle.save();
        }

        registrar.update();

        assertions: {
            assertEquals(1, getPendingTranslationCount(registry) - initialPendingTranslationCount);

            angularResourceBundle.load();
            assertEquals("value", angularResourceBundle.getEntries().get("key"));

            final RegistryInfo registryInfo = registry.getRegistryInfoForBundle(angularResourceBundle);
            final KeyData keyData = registryInfo.getKeyData("key");
            assertEquals(UPDATED, keyData.getStatus());
            assertEquals(2, keyData.getLocales().size());
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("nl"));
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("fr"));
        }
    }

    @Test
    public void addedKeysAreAddedToRegistryAndReferenceBundle() throws IOException {
        final String wicketBundleFileName = "org/onehippo/cms/l10n/test/DummyWicketPlugin.properties";
        final WicketResourceBundle wicketResourceBundle;
        final int initialPendingTranslationCount;
        final RegistryInfo registryInfo;
        
        // change the reference bundle to fake and incoming addition
        setup: {
            initialPendingTranslationCount = getPendingTranslationCount(registry);
            wicketResourceBundle = new WicketResourceBundle(null, wicketBundleFileName, new File(resources, wicketBundleFileName));
            wicketResourceBundle.load();
            wicketResourceBundle.getEntries().remove("key");
            wicketResourceBundle.save();
            registryInfo = registry.getRegistryInfoForBundle(wicketResourceBundle);
            registryInfo.removeKeyData("key");
            registryInfo.save();
        }
        
        registrar.update();

        assertions: {
            assertEquals(2, getPendingTranslationCount(registry) - initialPendingTranslationCount);
            wicketResourceBundle.load();
            assertEquals("value", wicketResourceBundle.getEntries().get("key"));
            registryInfo.load();
            final KeyData keyData = registryInfo.getKeyData("key");
            assertEquals(ADDED, keyData.getStatus());
            assertEquals(2, keyData.getLocales().size());
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("nl"));
            assertEquals(UNRESOLVED, keyData.getLocaleStatus("fr"));
        }
    }

    @Test
    public void addedBundlesAreRegisteredAndAdded() throws IOException {
        final String registryFileName = "angular/dummy/i18n/registry.json";
        
        setup: {
            // remove the reference bundle to fake incoming addition
            registry.getRegistryFile("angular/dummy/i18n/en.json").delete();
            registry.getRegistryFile("angular/dummy/i18n/nl.json").delete();
            registry.getRegistryFile(registryFileName).delete();
        }
        
        registrar.update();

        assertions : {
            final RegistryInfo registryInfo = registry.getRegistryInfo(registryFileName);
            assertTrue(registryInfo.exists());
            assertEquals(1, registryInfo.getKeys().size());
            final KeyData keyData = registryInfo.getKeyData("key");
            assertNotNull(keyData);
            assertEquals(ADDED, keyData.getStatus());
            assertEquals(2, keyData.getLocales().size());
        }
    }

    @Test
    public void deletedKeysAreDeletedFromRegistryAndReferenceBundle() throws IOException {
        final String registryFileName = "angular/dummy/i18n/registry.json";
        final RegistryInfo registryInfo;
        final ResourceBundle resourceBundle;
        
        setup: {
            // change the registry and reference bundle to fake incoming deletion
            registryInfo = registry.getRegistryInfo(registryFileName);
            registryInfo.putKeyData("key2", new KeyData(CLEAN));
            registryInfo.save();
            resourceBundle = registry.getResourceBundle("", "en", registryInfo);
            resourceBundle.getEntries().put("key2", "value2");
            resourceBundle.save();
        }

        registrar.update();
        
        assertions: {
            registryInfo.load();
            assertNull(registryInfo.getKeyData("key2"));

            resourceBundle.load();
            assertNull(resourceBundle.getEntries().get("key2"));
        }
    }

    @Test
    public void deletedBundlesAreDeleted() throws IOException {
        final String registryFileName = "org/example/test/TestPlugin.registry.json";
        final String bundleFileName = "org/example/test/TestPlugin.properties";
        final String bundleFileNameNL = "org/example/test/TestPlugin_nl.properties";
        
        setup: {
            // add a local bundles and registry file to fake incoming bundle delete
            final RegistryInfo registryInfo = registry.getRegistryInfo(registryFileName);
            registryInfo.setBundleType(WICKET);
            registryInfo.putKeyData("key", new KeyData(CLEAN));
            registryInfo.save();
            WicketResourceBundle resourceBundle = new WicketResourceBundle(null, bundleFileName, 
                    new File(resources, bundleFileName));
            resourceBundle.getEntries().put("key", "value");
            resourceBundle.save();
            resourceBundle = new WicketResourceBundle(null, bundleFileNameNL, new File(resources, bundleFileNameNL));
            resourceBundle.getEntries().put("key", "waarde");
            resourceBundle.save();
        }

        registrar.update();

        assertions: {
            assertFalse("Reference bundle file still exists", registry.getRegistryFile(bundleFileName).exists());
            assertFalse("Dutch bundle file still exists", registry.getRegistryFile(bundleFileNameNL).exists());
            assertFalse("Registry file still exists", registry.getRegistryFile(registryFileName).exists());
        }
    }

    @Test
    public void testDeletedRepositoryBundle() throws IOException {
        final String registryInfoFileName = "dummy-repository-translations.registry.json";
        final String bundleFileNameEN = "dummy-repository-translations_en.yaml";
        final String bundleFileNameNL = "dummy-repository-translations_nl.yaml";
        final String bundleFileNameFR = "dummy-repository-translations_fr.yaml";
        final ResourceBundle bundle2EN;
        final ResourceBundle bundle2NL;
        final ResourceBundle bundle2FR;
        final RegistryInfo registryInfo;
        
        setup: {
            // add a bundle to an existing repository resource bundles file to fake incoming delete of repository resource bundle
            bundle2EN = ResourceBundle.createInstance("bundle2", bundleFileNameEN, registry.getRegistryFile(bundleFileNameEN), REPOSITORY);
            bundle2EN.getEntries().put("key", "value");
            bundle2EN.save();
            bundle2NL = ResourceBundle.createInstance("bundle2", bundleFileNameNL, registry.getRegistryFile(bundleFileNameNL), REPOSITORY);
            bundle2NL.getEntries().put("key", "waarde");
            bundle2NL.save();
            bundle2FR = ResourceBundle.createInstance("bundle2", bundleFileNameFR, registry.getRegistryFile(bundleFileNameFR), REPOSITORY);
            bundle2FR.getEntries().put("key", "waarde");
            bundle2FR.save();
            registryInfo = registry.getRegistryInfo(registryInfoFileName);
            registryInfo.putKeyData("bundle2/key", new KeyData(CLEAN));
        }
        
        registrar.update();
        
        assertions: {
            bundle2EN.load();
            assertFalse(bundle2EN.exists());
            bundle2NL.load();
            assertFalse(bundle2NL.exists());
            bundle2FR.load();
            assertFalse(bundle2FR.exists());
            // multiple bundles in one file
            assertTrue(registryInfo.exists());
            registryInfo.load();
            assertNull(registryInfo.getKeyData("bundle2/key"));
            assertNotNull(registryInfo.getKeyData("bundle/key"));
            assertTrue(registry.getRegistryFile(bundleFileNameEN).exists());
            assertTrue(registry.getRegistryFile(bundleFileNameNL).exists());
            assertTrue(registry.getRegistryFile(bundleFileNameFR).exists());
        }
    }
}
