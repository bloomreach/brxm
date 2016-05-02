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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WicketResourceBundleLoaderTest {

    @Test
    public void testLoadBundles() throws Exception {
        final Collection<ResourceBundle> resourceBundles = new WicketResourceBundleLoader(
                Arrays.asList("en"), getClass().getClassLoader(), new String[]{}).loadBundles();
        assertEquals(1, resourceBundles.size());
        ResourceBundle bundle = resourceBundles.iterator().next();
        assertEquals("org/onehippo/cms/l10n/test/DummyWicketPlugin.properties", bundle.getName());
        assertEquals(BundleType.WICKET, bundle.getType());
        assertEquals(3, bundle.getEntries().size());
        assertEquals("en", bundle.getLocale());
    }

}
