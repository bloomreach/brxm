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
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoverTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    protected Collection<String> extractorLocales = Arrays.asList("en", "nl", "fr");
    protected Collection<String> registrarLocales = Arrays.asList("nl", "fr");
    
    protected Registrar registrar;
    protected Registry registry;

    @Before
    public void setUp() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        final File resources = temporaryFolder.newFolder("resources");
        new Extractor(resources, "module", extractorLocales, classLoader, new String[] {}).extract();
        registrar = new Registrar(temporaryFolder.getRoot(), "module", registrarLocales, classLoader, new String[] {});
        registrar.initialize();
        registry = registrar.getRegistry();
    }

    @Test
    public void testMoveKey() throws IOException {
        final Mover mover = new Mover(temporaryFolder.getRoot(), "module", registrarLocales);
        mover.moveKey("dummy-repository-translations.yaml", "bundle/key", "bundle/movedKey");
        final RegistryInfo registryInfo = registry.getRegistryInfo("dummy-repository-translations.registry.json");
        assertNull(registryInfo.getKeyData("bundle/key"));
        assertNotNull(registryInfo.getKeyData("bundle/movedKey"));
        for (final String locale : extractorLocales) {
            final RepositoryResourceBundle resourceBundle = (RepositoryResourceBundle) registry.getResourceBundle("bundle", locale, registryInfo);
            resourceBundle.setModuleName("module");
            assertEquals(1, resourceBundle.getEntries().size());
            assertTrue(resourceBundle.getEntries().containsKey("movedKey"));
        }
    }
    
    @Test
    public void testMoveBundle() throws IOException {
        final Mover mover = new Mover(temporaryFolder.getRoot(), "module", registrarLocales);
        mover.moveBundle("dummy-repository-translations.yaml", "dummy-repository-translations-moved.yaml");
        RegistryInfo registryInfo = registry.getRegistryInfo("dummy-repository-translations.registry.json");
        assertFalse(registryInfo.exists());
        registryInfo = registry.getRegistryInfo("dummy-repository-translations-moved.registry.json");
        assertTrue(registryInfo.exists());
        for (String locale : extractorLocales) {
            final RepositoryResourceBundle resourceBundle = (RepositoryResourceBundle) registry.getResourceBundle("bundle", locale, registryInfo);
            resourceBundle.setModuleName("module");
            assertTrue(resourceBundle.exists());
        }
    }
    
}
