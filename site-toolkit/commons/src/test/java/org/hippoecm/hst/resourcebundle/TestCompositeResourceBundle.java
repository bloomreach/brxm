/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.collections.EnumerationUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * TestCompositeResourceBundle
 */
public class TestCompositeResourceBundle {

    private ResourceBundle globalBundle;

    @Before
    public void before() throws Exception {
        globalBundle = new ListResourceBundle() {
            protected Object[][] getContents() {
                return new Object[][] {
                    {"animal", "fox"},
                    {"target", "dog"}
                };
            }
        };
    }

    @Test
    public void testRawGlobalResourceBundle() throws Exception {
        ResourceBundle bundle = globalBundle;
        assertEquals("fox", bundle.getObject("animal"));
        assertEquals("fox", bundle.getString("animal"));
        assertEquals("dog", bundle.getObject("target"));
        assertEquals("dog", bundle.getString("target"));

        try {
            assertNull(bundle.getObject("number"));
            fail("number resource key shouldn't be found.");
        } catch (MissingResourceException e) {
            // intended
        }

        try {
            assertNull(bundle.getString("number"));
            fail("number resource key shouldn't be found.");
        } catch (MissingResourceException e) {
            // intended
        }
    }

    @Test
    public void testCompositeGlobalResourceBundle() throws Exception {
        CompositeResourceBundle bundle = new CompositeResourceBundle(globalBundle);
        assertEquals("fox", bundle.getObject("animal"));
        assertEquals("fox", bundle.getString("animal"));
        assertEquals("dog", bundle.getObject("target"));
        assertEquals("dog", bundle.getString("target"));

        try {
            assertNull(bundle.getObject("number"));
            fail("number resource key shouldn't be found.");
        } catch (MissingResourceException e) {
            // intended
        }

        try {
            assertNull(bundle.getString("number"));
            fail("number resource key shouldn't be found.");
        } catch (MissingResourceException e) {
            // intended
        }

        List keyList = EnumerationUtils.toList(bundle.getKeys());
        assertEquals(2, keyList.size());
        assertTrue(keyList.contains("animal"));
        assertTrue(keyList.contains("target"));
        assertFalse(keyList.contains("number"));
    }

    @Test
    public void testCompositeLocalResourceBundle() throws Exception {
        ResourceBundle localBundle = new ListResourceBundle() {
            protected Object[][] getContents() {
                return new Object[][] {
                    {"animal", "cat"},
                    {"number", "1234567890"}
                };
            }
        };

        CompositeResourceBundle bundle = new CompositeResourceBundle(localBundle, globalBundle);
        assertEquals("cat", bundle.getObject("animal"));
        assertEquals("cat", bundle.getString("animal"));
        assertEquals("dog", bundle.getObject("target"));
        assertEquals("dog", bundle.getString("target"));
        assertEquals("1234567890", bundle.getString("number"));

        try {
            assertNull(bundle.getObject("unknown.number"));
            fail("number resource key shouldn't be found.");
        } catch (MissingResourceException e) {
            // intended
        }

        try {
            assertNull(bundle.getString("unknown.number"));
            fail("number resource key shouldn't be found.");
        } catch (MissingResourceException e) {
            // intended
        }

        List keyList = EnumerationUtils.toList(bundle.getKeys());
        assertEquals(3, keyList.size());
        assertTrue(keyList.contains("animal"));
        assertTrue(keyList.contains("target"));
        assertTrue(keyList.contains("number"));
        assertFalse(keyList.contains("unknown.number"));
    }

}
