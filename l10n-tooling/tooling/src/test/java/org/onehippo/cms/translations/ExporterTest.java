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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExporterTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File resources;
    private Registrar registrar;
    
    @Before
    public void createTestModule() throws IOException {
        resources = new File(temporaryFolder.getRoot(), "resources");
        resources.mkdir();
        new Extractor(resources, Arrays.asList("en", "nl")).extract();
        final File pom = new File(temporaryFolder.getRoot(), "pom.xml");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("pom.xml"), pom);
        changeBundle();
        registrar = new Registrar(resources, Arrays.asList("nl"));
        registrar.updateRegistry();
    }
    
    private void changeBundle() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("key", "foo");

        // updateRegistry reference bundle
        ResourceBundle angularBundle = new AngularResourceBundle("", "angular/dummy/i18n/en.json", null, "en", properties);
        AngularResourceBundleSerializer angularResourceBundleSerializer = new AngularResourceBundleSerializer(resources);
        angularResourceBundleSerializer.serializeBundle(angularBundle);
    }

    @Test
    public void testExporter() throws Exception {
        new Exporter(temporaryFolder.getRoot()).export("nl");
    }
}
