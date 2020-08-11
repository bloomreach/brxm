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
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.l10n.KeyData.LocaleStatus.RESOLVED;

public class RegistrarTest {
    
    private static final Logger log = LoggerFactory.getLogger(RegistrarTest.class);
    
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    protected Collection<String> extractorLocales = Arrays.asList("en", "nl", "fr");
    protected Collection<String> registrarLocales = Arrays.asList("nl", "fr");

    protected File resources;
    protected Registrar registrar;
    protected Registry registry;

    @Before
    public void setUp() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        resources = temporaryFolder.newFolder("resources");
        new Extractor(resources, "module", extractorLocales, classLoader, new String[] {}).extract();

        initializeRegistry();
    }

    protected int getPendingTranslationCount(Registry registry) throws IOException {
        int count = 0;
        for (RegistryInfo file : registry.getRegistryInfos()) {
            file.load();
            for (String key : file.getKeys()) {
                KeyData keyData = file.getKeyData(key);
                for (String locale : registrarLocales) {
                    if (keyData.getLocaleStatus(locale) != RESOLVED) {
                        count++;
                        log.info("Unresolved locale status: " +
                                "fileId = " + file.getFileName()
                                + "; key = " + key
                                + "; locale = " + locale);
                    }
                }
            }
        }

        return count;
    }

    protected void initializeRegistry() throws IOException {
        final ClassLoader classLoader = getClass().getClassLoader();
        registrar = new Registrar(temporaryFolder.getRoot(), "module", registrarLocales, classLoader, new String[]{});
        registrar.initialize();

        registry = registrar.getRegistry();
    }

}
