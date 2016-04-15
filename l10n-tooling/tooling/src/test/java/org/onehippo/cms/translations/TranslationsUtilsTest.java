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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms.translations.BundleType.ANGULAR;
import static org.onehippo.cms.translations.BundleType.REPOSITORY;
import static org.onehippo.cms.translations.BundleType.WICKET;
import static org.onehippo.cms.translations.TranslationsUtils.mapSourceBundleFileToTargetBundleFile;
import static org.onehippo.cms.translations.TranslationsUtils.mapRegistryFileToResourceBundleFile;
import static org.onehippo.cms.translations.TranslationsUtils.mapResourceBundleToRegistryFile;

public class TranslationsUtilsTest {

    @Test
    public void testSourceBundleFileToTargetBundleFileMapping() {
        String bundleFileName = mapSourceBundleFileToTargetBundleFile("dummy-repository-translations.json", REPOSITORY, "nl");
        assertEquals("dummy-repository-translations_nl.json", bundleFileName);
    }
    
    @Test
    public void testResourceBundleToRegistryFileMapping() {
        String registryFile = mapResourceBundleToRegistryFile(ANGULAR, "angular/project/app/i18n/en.json");
        assertEquals("angular/project/app/i18n/registry.json", registryFile);
        registryFile = mapResourceBundleToRegistryFile(WICKET, "org/example/TestPlugin.properties");
        assertEquals("org/example/TestPlugin.registry.json", registryFile);
        registryFile = mapResourceBundleToRegistryFile(REPOSITORY, "example-translations_en.json");
        assertEquals("example-translations.registry.json", registryFile);
    }

    @Test
    public void testRegistryFileToResourceBundleFilesMapping() throws IOException {
        String bundleFileName = mapRegistryFileToResourceBundleFile("angular/dummy/i18n/registry.json", ANGULAR, "nl");
        assertEquals("angular/dummy/i18n/nl.json", bundleFileName);
        bundleFileName = mapRegistryFileToResourceBundleFile("com/onehippo/cms7/localizer/test/DummyWicketPlugin.registry.json", WICKET, "nl");
        assertEquals("com/onehippo/cms7/localizer/test/DummyWicketPlugin_nl.properties", bundleFileName);
        bundleFileName = mapRegistryFileToResourceBundleFile("dummy-repository-translations.registry.json", REPOSITORY, "nl");
        assertEquals("dummy-repository-translations_nl.json", bundleFileName);
    }
    
}
