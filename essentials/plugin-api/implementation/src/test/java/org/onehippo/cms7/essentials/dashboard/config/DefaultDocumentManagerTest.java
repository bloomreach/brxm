/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.config;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class DefaultDocumentManagerTest extends BaseRepositoryTest {

    @Test
    public void testSaveDocument() throws Exception {

        final DocumentManager manager = new DefaultDocumentManager(session);
        final String path = "/foo/bar";
        final Document document = new BaseDocument("myConfig", path);
        document.addProperty("foo");
        document.addProperty("bar");
        document.addProperty("foobar");
        final boolean saved = manager.saveDocument(document);
        assertTrue("Expected document to be saved", saved);
        final Document fetched = manager.fetchDocument(path);
        assertEquals(fetched.getProperties().get(0), "foo");
        assertEquals(fetched.getProperties().get(1), "bar");
        assertEquals(fetched.getProperties().get(2), "foobar");


    }
}
