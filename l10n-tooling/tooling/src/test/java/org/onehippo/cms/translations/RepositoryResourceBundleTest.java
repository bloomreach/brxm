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

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepositoryResourceBundleTest extends ResourceBundleTest {
    
    @Override
    protected RepositoryResourceBundle createResourceBundle() {
        final String fileName = "dummy-repository-translations_en.json";
        return new RepositoryResourceBundle("foo", fileName, new File(temporaryFolder.getRoot(), fileName));
    }
    
    @Test
    @Override
    public void testResourceBundleSerialization() throws Exception {
        final RepositoryResourceBundle resourceBundle = createResourceBundle();
        resourceBundle.getEntries().put("key", "value");
        resourceBundle.save();
        resourceBundle.load();
        assertEquals(resourceBundle.getEntries().get("key"), "value");
        final File extensionFile = resourceBundle.getExtensionFile();
        assertTrue(extensionFile.exists());
        final RepositoryExtension extension = RepositoryExtension.load(extensionFile);
        assertTrue(extension.containsResourceBundle(resourceBundle));
    }
    
    @Test
    public void testResourceBundleDeletion() throws Exception {
        final RepositoryResourceBundle bundle1;
        final RepositoryResourceBundle bundle2;
        
        createTwoBundles: {
            final String fileName = "dummy-repository-translations_en.json";
            bundle1 = new RepositoryResourceBundle("path.to.bundle1", fileName, new File(temporaryFolder.getRoot(), fileName));
            bundle2 = new RepositoryResourceBundle("path.to.bundle2", fileName, new File(temporaryFolder.getRoot(), fileName));
            bundle1.getEntries().put("key", "value");
            bundle2.getEntries().put("key", "value");
            bundle1.save();
            bundle2.save();
        }

        final File extensionFile;
        
        assertBundlesAndExtensionExist: {
            assertTrue(bundle1.exists());
            assertTrue(bundle2.exists());
            extensionFile = bundle1.getExtensionFile();
            assertTrue(extensionFile.exists());
            assertEquals(extensionFile, bundle2.getExtensionFile());
            final RepositoryExtension extension = RepositoryExtension.load(extensionFile);
            assertTrue(extension.containsResourceBundle(bundle1));
            assertTrue(extension.containsResourceBundle(bundle2));
//        System.out.println(FileUtils.readFileToString(extensionFile));
//        System.out.println(FileUtils.readFileToString(new File(temporaryFolder.getRoot(), bundle1.getFileName())));
        }

        bundle2.delete();
        
        assertOneBundleAndExtensionExist: {
            assertTrue(bundle1.exists());
            assertFalse(bundle2.exists());
            assertTrue(extensionFile.exists());
            final RepositoryExtension extension = RepositoryExtension.load(extensionFile);
            assertTrue(extension.containsResourceBundle(bundle1));
//        System.out.println(FileUtils.readFileToString(extensionFile));
//        System.out.println(FileUtils.readFileToString(new File(temporaryFolder.getRoot(), bundle1.getFileName())));
        }
        
        bundle1.delete();
        
        assertNoBundlesAndNoExtensionExist: {
            assertFalse(bundle1.exists());
            assertFalse(new File(temporaryFolder.getRoot(), bundle1.getFileName()).exists());
            assertFalse(extensionFile.exists());
        }
    }
    
}
