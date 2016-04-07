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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RepositoryExtensionTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    public ResourceBundle createResourceBundle() {
        return new AngularResourceBundle("foo.bar", "foobar.json", new ArtifactInfo("test-artifact"), "nl", null);
    }
    
    @Test
    public void testCreateEmptyExtension() throws Exception {
        final RepositoryExtension extension = RepositoryExtension.create();
        final File file = temporaryFolder.newFile("extension.xml");
        extension.save(file);
//        System.out.println(FileUtils.readFileToString(file));
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
//        System.out.println(FileUtils.readFileToString(file));
    }
    
    @Test
    public void testRemoveItem() throws Exception {
        RepositoryExtension extension = RepositoryExtension.create();
        final ResourceBundle resourceBundle = createResourceBundle();
        extension.addResourceBundlesItem(resourceBundle);
        final File file = temporaryFolder.newFile("extension.xml");
        extension.save(file);
        extension = RepositoryExtension.load(file);
        extension.removeResourceBundlesItem(resourceBundle);
        extension.save(file);
//        System.out.println(FileUtils.readFileToString(file));
    }

}
