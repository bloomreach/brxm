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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RepositoryResourceBundleLoaderTest {

    @Test
    public void testLoadBundles() throws Exception {
        final Collection<ResourceBundle> bundles = new RepositoryResourceBundleLoader(
                Arrays.asList("en"), getClass().getClassLoader()).loadBundles();
        assertEquals(2, bundles.size());

        Optional<ResourceBundle> bundle = bundles.stream().filter(b -> "bundle".equals(b.getName())).findFirst();
        assertTrue(bundle.isPresent());
        assertEquals("bundle", bundle.get().getName());
        assertEquals(1, bundle.get().getEntries().size());
        Map.Entry<String, String> entry = bundle.get().getEntries().entrySet().iterator().next();
        assertEquals("key", entry.getKey());
        assertEquals("value", entry.getValue());
        assertEquals("en", bundle.get().getLocale());

        Optional<ResourceBundle> anotherBundle = bundles.stream().filter(b -> "anotherBundle".equals(b.getName())).findFirst();
        assertTrue(anotherBundle.isPresent());
        assertEquals("anotherBundle", anotherBundle.get().getName());
    }

}
