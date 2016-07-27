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

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cms.l10n.KeyData.LocaleStatus.UNRESOLVED;

public class InitializeRegistryTest extends RegistrarTest {
    
    @Test
    public void testInitializeRegistry() throws IOException {
        assertEquals(5, getPendingTranslationCount(registry));

        RegistryInfo registryInfo = registry.getRegistryInfo("angular/dummy/i18n/registry.json");
        assertEquals(UNRESOLVED, registryInfo.getKeyData("key").getLocaleStatus("fr"));

        registryInfo = registry.getRegistryInfo("org/onehippo/cms/l10n/test/DummyWicketPlugin.registry.json");
        assertEquals(UNRESOLVED, registryInfo.getKeyData("key_en_nl").getLocaleStatus("fr"));
        assertEquals(UNRESOLVED, registryInfo.getKeyData("key_en_only").getLocaleStatus("nl"));
        assertEquals(UNRESOLVED, registryInfo.getKeyData("key_en_only").getLocaleStatus("fr"));

        registryInfo = registry.getRegistryInfo("dummy-repository-translations.registry.json");
        assertEquals(UNRESOLVED, registryInfo.getKeyData("anotherBundle/key").getLocaleStatus("fr"));
    }

    @Test
    public void running_initialize_again_should_result_in_unchanged_registry() throws IOException {
        initializeRegistry();

        testInitializeRegistry();
    }

}
