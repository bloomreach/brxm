/**
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.repository.mock.MockBinary;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestResourceBundleUtils {

    @Test
    public void test_localeLookupList() {
        assertEquals(0, ResourceBundleUtils.localeLookupList(null).size());

        final List<Locale> localesEmpty = ResourceBundleUtils.localeLookupList(new Locale(""));
        assertEquals(1, localesEmpty.size());
        assertEquals("", localesEmpty.get(0).toString());

        final List<Locale> localesEN = ResourceBundleUtils.localeLookupList(Locale.ENGLISH);
        assertEquals(1, localesEN.size());
        assertEquals("en", localesEN.get(0).toString());
        final List<Locale> localesFR = ResourceBundleUtils.localeLookupList(Locale.FRANCE);
        assertEquals(2, localesFR.size());
        assertEquals("fr_FR", localesFR.get(0).toString());
        assertEquals("fr", localesFR.get(1).toString());
    }

    @Test
    public void test_getLocalePathForBase() {
        assertEquals("test.properties", ResourceBundleUtils.getLocalePathForBase("test.properties", new Locale("")));
        assertEquals("test_en.properties", ResourceBundleUtils.getLocalePathForBase("test.properties", Locale.ENGLISH));
        assertEquals("/foo/bar/test_en.properties", ResourceBundleUtils.getLocalePathForBase("/foo/bar/test.properties", Locale.ENGLISH));
        assertEquals("test_fr_FR.properties", ResourceBundleUtils.getLocalePathForBase("test.properties", Locale.FRANCE));
        assertEquals("/foo/bar/test_fr_FR.properties", ResourceBundleUtils.getLocalePathForBase("/foo/bar/test.properties", Locale.FRANCE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getLocalePathForBase_for_invalid_base_path1() {
        ResourceBundleUtils.getLocalePathForBase("test.propertiesx", Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getLocalePathForBase_for_invalid_base_path2() {
        ResourceBundleUtils.getLocalePathForBase("test.xproperties", Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getLocalePathForBase_for_invalid_base_path3() {
        ResourceBundleUtils.getLocalePathForBase(".properties", Locale.ENGLISH);
    }

    @Test
    public void default_only_bundle_with_and_without_locale() throws RepositoryException, IOException {
        final Node folder = MockNode.root().addNode("foo", "nt:folder")
                .addNode("bar", "nt:folder");

        final Node basePropertiesNode = folder.addNode("test.properties", JcrConstants.NT_FILE);

        final Node content = basePropertiesNode.addNode("jcr:content", JcrConstants.NT_RESOURCE);
        final InputStream defaultProperties = TestResourceBundleUtils.class.getClassLoader().getResourceAsStream("TestResourceBundleUtils.properties");
        MockBinary binary = new MockBinary(defaultProperties);
        content.setProperty(JcrConstants.JCR_DATA, binary);

        {
            final ResourceBundle bundle = ResourceBundleUtils.getBundle(basePropertiesNode.getSession(), basePropertiesNode.getPath(), null);
            assertEquals("testVal", bundle.getString("test"));
            try {
                bundle.getString("foo");
                fail("Key 'foo' is only present in fr bundle");
            } catch (MissingResourceException e) {
                // expected
            }
        }
        {
            final ResourceBundle bundle = ResourceBundleUtils.getBundle(basePropertiesNode.getSession(), basePropertiesNode.getPath(), Locale.FRANCE);
            assertEquals("testVal", bundle.getString("test"));
            try {
                bundle.getString("foo");
                fail("Key 'foo' is only present in fr bundle which is not added as jcr node");
            } catch (MissingResourceException e) {
                // expected
            }
        }
    }

    @Test
    public void default_fr_and_frFR_bundle_with_and_without_locale() throws RepositoryException, IOException {
        final Node folder = MockNode.root().addNode("foo", "nt:folder")
                .addNode("bar", "nt:folder");

        for (String locale : new String[]{"", "_fr", "_fr_FR"}) {
            final Node propertiesNode = folder.addNode(String.format("test%s.properties", locale), JcrConstants.NT_FILE);
            final Node content = propertiesNode.addNode("jcr:content", JcrConstants.NT_RESOURCE);
            final InputStream properties = TestResourceBundleUtils.class.getClassLoader()
                    .getResourceAsStream(String.format("TestResourceBundleUtils%s.properties", locale));
            MockBinary binary = new MockBinary(properties);
            content.setProperty(JcrConstants.JCR_DATA, binary);
        }

        {
            final ResourceBundle bundle = ResourceBundleUtils.getBundle(folder.getSession(), folder.getPath()+"/test.properties", null);
            assertEquals("testVal", bundle.getString("test"));
            try {
                bundle.getString("foo");
                fail("Key 'foo' is only present in fr bundle");
            } catch (MissingResourceException e) {
                // expected
            }
        }

        {
            final ResourceBundle bundle = ResourceBundleUtils.getBundle(folder.getSession(), folder.getPath()+"/test.properties", new Locale(""));
            assertEquals("testVal", bundle.getString("test"));
            try {
                bundle.getString("foo");
                fail("Key 'foo' is only present in fr bundle");
            } catch (MissingResourceException e) {
                // expected
            }
        }

        {
            final ResourceBundle bundle = ResourceBundleUtils.getBundle(folder.getSession(), folder.getPath()+"/test.properties",new Locale("fr"));
            assertEquals("testValFr", bundle.getString("test"));
            assertEquals("barFr", bundle.getString("foo"));
            try {
                bundle.getString("lux");
                fail("Key 'lux' is only present in fr_FR bundle");
            } catch (MissingResourceException e) {
                // expected
            }
        }

        {
            final ResourceBundle bundle = ResourceBundleUtils.getBundle(folder.getSession(), folder.getPath()+"/test.properties", Locale.FRANCE);
            assertEquals("testValfrFR", bundle.getString("test"));
            assertEquals("barFr_FR", bundle.getString("foo"));
            assertEquals("barFr_FR", bundle.getString("lux"));
        }
    }

    @Test
    public void default_bundle_for_locale_that_has_no_resource_bundle() throws RepositoryException, IOException {
        final Node folder = MockNode.root().addNode("foo", "nt:folder")
                .addNode("bar", "nt:folder");

        final Node basePropertiesNode = folder.addNode("test.properties", JcrConstants.NT_FILE);

        final Node content = basePropertiesNode.addNode("jcr:content", JcrConstants.NT_RESOURCE);
        final InputStream defaultProperties = TestResourceBundleUtils.class.getClassLoader().getResourceAsStream("TestResourceBundleUtils.properties");
        MockBinary binary = new MockBinary(defaultProperties);
        content.setProperty(JcrConstants.JCR_DATA, binary);
        final ResourceBundle bundle = ResourceBundleUtils.getBundle(basePropertiesNode.getSession(), basePropertiesNode.getPath(), Locale.GERMAN);
        assertEquals("testVal", bundle.getString("test"));
        try {
            bundle.getString("foo");
            fail("Key 'foo' is only present in fr bundle");
        } catch (MissingResourceException e) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void void_test_bundle_invalid_path() throws RepositoryException {
        final Node folder = MockNode.root().addNode("foo", "nt:folder");
        ResourceBundleUtils.getBundle(folder.getSession(), folder.getPath(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void void_test_bundle_non_existing_path() throws RepositoryException {
        final Node folder = MockNode.root().addNode("foo", "nt:folder");
        ResourceBundleUtils.getBundle(folder.getSession(), folder.getPath() + ".properties", null);
    }

    @Test(expected = IllegalStateException.class)
    public void void_test_bundle_missing_base_properties_file() throws RepositoryException {
        final Node basePropertiesNode = MockNode.root().addNode("foo", "nt:folder")
                .addNode("bar", "nt:folder")
                .addNode("test_fr_FR.properties", JcrConstants.NT_FILE);
        basePropertiesNode.addNode("jcr:content", JcrConstants.NT_RESOURCE);

        ResourceBundleUtils.getBundle(basePropertiesNode.getSession(), basePropertiesNode.getPath(), Locale.FRANCE);
    }
}
