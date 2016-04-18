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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class RegistryInfoTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testSaveAndLoad() throws IOException {
        final File file = temporaryFolder.newFile("registry.json");
        RegistryInfo registryInfo = new RegistryInfo("registry.json", file);
        registryInfo.setBundleType(BundleType.ANGULAR);
        registryInfo.getOrCreateKeyData("key").setStatus(KeyData.KeyStatus.ADDED);
        registryInfo.save();
        registryInfo.load();
        assertEquals(BundleType.ANGULAR, registryInfo.getBundleType());
    }

}
