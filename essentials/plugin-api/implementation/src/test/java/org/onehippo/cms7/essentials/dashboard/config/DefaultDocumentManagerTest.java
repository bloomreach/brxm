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
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class DefaultDocumentManagerTest extends BaseRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultDocumentManagerTest.class);
    @Test
    public void testSaveDocument() throws Exception {

        // TODO mm: introduce porperties or remove testcase:

        final DocumentManager manager = new DefaultDocumentManager(getContext());
        final String parentPath = "/foo/bar";
        Document document = new BaseDocument("myConfig", parentPath);
        document.addProperty("foo");
        document.addProperty("bar");
        document.addProperty("foobar");
        final boolean saved = manager.saveDocument(document);
        assertTrue("Expected document to be saved", saved);
        Document fetched = manager.fetchDocument(document.getPath(), BaseDocument.class);
        assertEquals("myConfig", fetched.getName());
        assertEquals("/foo/bar", fetched.getParentPath());
        assertEquals("/foo/bar/myConfig", fetched.getPath());
        assertEquals(fetched.getProperties().get(0), "foo");
        assertEquals(fetched.getProperties().get(1), "bar");
        assertEquals(fetched.getProperties().get(2), "foobar");
        // save as class
        final String classPath = BaseDocument.class.getName();
        document = new BaseDocument(BaseDocument.class.getSimpleName(), GlobalUtils.getParentConfigPath(classPath));
        document.addProperty("foo");
        document.addProperty("bar");
        document.addProperty("foobar");
        manager.saveDocument(document);
        fetched = manager.fetchDocument(classPath);
        assertEquals(fetched.getProperties().get(0), "foo");
        assertEquals(fetched.getProperties().get(1), "bar");
        assertEquals(fetched.getProperties().get(2), "foobar");

    }
}
