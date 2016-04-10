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
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class RepositoryResourceBundleSerializerTest {
    
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder(); 
    
    @Test
    public void testSerializeResourceBundle() throws Exception {
        final File test = temporaryFolder.newFolder("test");
        new RepositoryResourceBundleSerializer(test).serializeBundle(createBundle());
//        System.out.println(FileUtils.readFileToString(new File(test, "foobar_en.json")));
//        System.out.println(FileUtils.readFileToString(new File(test, "extensions/en/hippoecm-extension.xml")));
    }
    
    private ResourceBundle createBundle() {
        return new AngularResourceBundle("foo.bar", "foobar.json", new ArtifactInfo("test-artifact"), "en", createProperties());
    }
    
    private Properties createProperties() {
        Properties properties = new Properties();
        properties.put("foo", "bar");
        return properties;
    }

    @Test
    public void test_multiple_bundles_can_be_stored_in_single_file() throws Exception {
        Properties set1 = new Properties();
        set1.put("key1", "key 1 set 1");
        set1.put("key2", "key 2 set 1");

        Properties set2 = new Properties();
        set2.put("key1", "key 1 set 2");
        set2.put("key2", "key 2 set 2");

        final File test = new File(temporaryFolder.getRoot(), "test");
        ResourceBundleSerializer serializer = new RepositoryResourceBundleSerializer(temporaryFolder.getRoot());

        serializer.serializeBundle(new RepositoryResourceBundle("one", test.getName(), new ArtifactInfo("test-artifact"), "en", set1));
        serializer.serializeBundle(new RepositoryResourceBundle("two", test.getName(), new ArtifactInfo("test-artifact"), "en", set2));

        ResourceBundle bundle = serializer.deserializeBundle(test.getName(), "one", "en");
        assertEquals("key 1 set 1", bundle.getEntries().get("key1"));
        assertEquals("key 2 set 1", bundle.getEntries().get("key2"));

        bundle = serializer.deserializeBundle(test.getName(), "two", "en");
        assertEquals("key 1 set 2", bundle.getEntries().get("key1"));
        assertEquals("key 2 set 2", bundle.getEntries().get("key2"));
    }

}
