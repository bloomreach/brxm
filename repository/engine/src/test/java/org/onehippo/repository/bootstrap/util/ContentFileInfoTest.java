/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.util;

import javax.jcr.Node;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.NT_INITIALIZEITEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ContentFileInfoTest {

    @Test
    public void testReadContentFileInfo() throws Exception {
        final Node item = MockNode.root().addNode("item", NT_INITIALIZEITEM);
        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/foo.xml").toString());
        item.setProperty(HIPPO_CONTENTROOT, "/test");

        ContentFileInfo contentFileInfo = ContentFileInfo.readInfo(item);

        assertEquals(1, contentFileInfo.contextPaths.size());
        assertEquals("/test/foo", contentFileInfo.contextPaths.get(0));
        assertNull(contentFileInfo.deltaDirective);

        item.setProperty(HIPPO_CONTENTRESOURCE, getClass().getResource("/bootstrap/delta.xml").toString());

        contentFileInfo = ContentFileInfo.readInfo(item);

        assertEquals(2, contentFileInfo.contextPaths.size());
        assertEquals("/test/foo", contentFileInfo.contextPaths.get(0));
        assertEquals("/test/foo/bar", contentFileInfo.contextPaths.get(1));
        assertEquals("combine", contentFileInfo.deltaDirective);
    }

}
