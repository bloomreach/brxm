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
import java.io.FileInputStream;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cms.translations.RepositoryResourceBundleLoader.ExtensionParser;

import static org.junit.Assert.assertEquals;

public class RepositoryExtensionTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    public ResourceBundle createResourceBundle() {
        return new AngularResourceBundle("", "foobar_nl.json", "nl", null);
    }
    
    @Test
    public void testCreateEmptyExtension() throws Exception {
        final RepositoryExtension extension = RepositoryExtension.create();
        final File file = temporaryFolder.newFile("extension.xml");
        extension.save(file);
        final Collection<String> bundles = new ExtensionParser().parse(new FileInputStream(file));
        assertEquals(0, bundles.size());
    }
    
    @Test
    public void testLoadExtension() throws Exception {
        RepositoryExtension extension = RepositoryExtension.create();
        final File file = temporaryFolder.newFile("extension.xml");
        extension.save(file);
        RepositoryExtension.load(file);        
    }
    
    @Test
    public void testAddItem() throws Exception {
        final RepositoryExtension extension = RepositoryExtension.create();
        extension.addResourceBundlesItem(createResourceBundle());
        final File file = temporaryFolder.newFile("extension.xml");
        extension.save(file);
        final Collection<String> bundles = new ExtensionParser().parse(new FileInputStream(file));
        assertEquals(1, bundles.size());
    }
    
    @Test
    public void testRemoveItem() throws Exception {
        RepositoryExtension extension = RepositoryExtension.create();
        final ResourceBundle resourceBundle = createResourceBundle();
        extension.addResourceBundlesItem(resourceBundle);
        final File file = temporaryFolder.newFile("extension.xml");
        extension.save(file);
        Collection<String> bundles = new ExtensionParser().parse(new FileInputStream(file));
        assertEquals(1, bundles.size());
        extension = RepositoryExtension.load(file);
        extension.removeResourceBundlesItem(resourceBundle);
        extension.save(file);
        bundles = new ExtensionParser().parse(new FileInputStream(file));
        assertEquals(0, bundles.size());
    }

}
