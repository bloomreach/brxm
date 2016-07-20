/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.l10n;

import java.util.Map;

import org.apache.commons.lang.LocaleUtils;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
import static org.hippoecm.repository.api.HippoNodeType.NT_RESOURCEBUNDLES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceBundleLoaderTest extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test", NT_RESOURCEBUNDLES);
        session.importXML("/test", getClass().getResourceAsStream("test-translations.xml"), IMPORT_UUID_COLLISION_THROW);
        session.save();
    }

    @Test
    public void testLoadBundles() throws Exception {
        final Map<ResourceBundleKey, ResourceBundle> bundles = ResourceBundleLoader.load(session.getNode("/test"));
        assertEquals("Expected two bundles to be present", 2, bundles.size());
        final ResourceBundle en = bundles.get(new ResourceBundleKey("foo.bar", LocaleUtils.toLocale("en")));
        assertNotNull(en);
        final ResourceBundle nl = bundles.get(new ResourceBundleKey("foo.bar", LocaleUtils.toLocale("nl")));
        assertNotNull(nl);
        assertEquals("value1", en.getString("key1"));
        assertEquals("waarde1", nl.getString("key1"));
        assertEquals("value2", en.getString("key2"));
        // fallback on default locale (en)
        assertEquals("value2", nl.getString("key2"));
    }

    @Test
    public void testJavaResourceBundles() throws Exception {
        final Map<ResourceBundleKey, ResourceBundle> bundles = ResourceBundleLoader.load(session.getNode("/test"));
        final java.util.ResourceBundle en = bundles.get(new ResourceBundleKey("foo.bar", LocaleUtils.toLocale("en"))).toJavaResourceBundle();
        final java.util.ResourceBundle nl = bundles.get(new ResourceBundleKey("foo.bar", LocaleUtils.toLocale("nl"))).toJavaResourceBundle();

        // containsKey requires the parents to be wired correctly
        assertTrue(en.containsKey("key1"));
        assertTrue(en.containsKey("key2"));
        assertTrue(nl.containsKey("key1"));
        // fallback on default locale (en)
        assertTrue(nl.containsKey("key2"));
    }

}
