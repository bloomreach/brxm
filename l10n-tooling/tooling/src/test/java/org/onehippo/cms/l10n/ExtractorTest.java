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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExtractorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testExtractor() throws IOException {
        new Extractor(temporaryFolder.getRoot(), "module-name", Arrays.asList("en", "nl", "fr"), 
                getClass().getClassLoader(), new String[] {}).extract();
        assertFileExists("angular/dummy/i18n/en.json");
        assertFileExists("angular/dummy/i18n/nl.json");
        assertFileExists("org/onehippo/cms/l10n/test/DummyWicketPlugin.properties");
        assertFileExists("org/onehippo/cms/l10n/test/DummyWicketPlugin_nl.properties");
        assertFileExists("org/onehippo/cms/l10n/test/DummyWicketPlugin_fr.properties");
        assertFileExists("dummy-repository-translations_en.yaml");
        assertFileExists("dummy-repository-translations_nl.yaml");
        assertFileExists("dummy-repository-translations_fr.yaml");
    }

    private void assertFileExists(final String fileName) {
        assertTrue("File not extracted: " + fileName, new File(temporaryFolder.getRoot(), fileName).exists());
    }
    
    private void assertFileNotExists(final String fileName) {
        assertFalse("File extracted: " + fileName, new File(temporaryFolder.getRoot(), fileName).exists());
    }

}
