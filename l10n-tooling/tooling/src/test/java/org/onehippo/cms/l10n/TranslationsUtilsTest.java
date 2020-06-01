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

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms.l10n.TranslationsUtils.mapRegistryFileToResourceBundleFile;
import static org.onehippo.cms.l10n.TranslationsUtils.mapResourceBundleToRegistryInfoFile;
import static org.onehippo.cms.l10n.TranslationsUtils.mapSourceBundleFileToTargetBundleFile;

public class TranslationsUtilsTest {

    @Test
    public void testSourceBundleFileToTargetBundleFileMapping() {
        String bundleFileName = mapSourceBundleFileToTargetBundleFile("dummy-repository-translations.yaml", BundleType.REPOSITORY, "nl");
        assertEquals("dummy-repository-translations_nl.yaml", bundleFileName);
    }
    
    @Test
    public void testResourceBundleToRegistryFileMapping() {
        String registryFile = mapResourceBundleToRegistryInfoFile(BundleType.ANGULAR, "angular/project/app/i18n/en.json");
        assertEquals("angular/project/app/i18n/registry.json", registryFile);
        registryFile = mapResourceBundleToRegistryInfoFile(BundleType.WICKET, "org/example/TestPlugin.properties");
        assertEquals("org/example/TestPlugin.registry.json", registryFile);
        registryFile = mapResourceBundleToRegistryInfoFile(BundleType.REPOSITORY, "example-translations_en.yaml");
        assertEquals("example-translations.registry.json", registryFile);
    }

    @Test
    public void testRegistryFileToResourceBundleFilesMapping() throws IOException {
        String bundleFileName = mapRegistryFileToResourceBundleFile("angular/dummy/i18n/registry.json", BundleType.ANGULAR, "nl");
        assertEquals("angular/dummy/i18n/nl.json", bundleFileName);
        bundleFileName = mapRegistryFileToResourceBundleFile("com/onehippo/cms7/localizer/test/DummyWicketPlugin.registry.json", BundleType.WICKET, "nl");
        assertEquals("com/onehippo/cms7/localizer/test/DummyWicketPlugin_nl.properties", bundleFileName);
        bundleFileName = mapRegistryFileToResourceBundleFile("dummy-repository-translations.registry.json", BundleType.REPOSITORY, "nl");
        assertEquals("dummy-repository-translations_nl.yaml", bundleFileName);
    }
    
}
