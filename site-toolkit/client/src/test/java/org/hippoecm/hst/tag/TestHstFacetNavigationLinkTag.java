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
package org.hippoecm.hst.tag;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestHstFacetNavigationLinkTag {

    @Test
    public void testRemoveFacetKeyValueFromMiddleOfPath() throws Exception {
        String replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("a/b/key/val/c","key","val");
        assertEquals("a/b/c", replaced);

        replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("a/key/val/b/key/val/c","key","val");
        assertEquals("a/b/c", replaced);

        replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("a/key/val/key/val/b/c","key","val");
        assertEquals("a/b/c", replaced);

        replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("/a/key/val/b/key/val/c/","key","val");
        assertEquals("/a/b/c/", replaced);

    }

    @Test
    public void testRemoveFacetKeyValueFromBeginOfPath() throws Exception {
        String replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("key/val/a/b/c","key","val");
        assertEquals("a/b/c", replaced);

        replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("/key/val/a/b/c/","key","val");
        assertEquals("/a/b/c/", replaced);

        replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("/key/val/key/val/a/key/val/b/c/","key","val");
        assertEquals("/a/b/c/", replaced);
    }

    @Test
      public void testRemoveFacetKeyValueFromEndOfPath() throws Exception {
        String replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("a/b/c/key/val","key","val");
        assertEquals("a/b/c", replaced);

        replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("/a/b/c/key/val/","key","val");
        assertEquals("/a/b/c/", replaced);

        replaced = HstFacetNavigationLinkTag.removeFacetKeyValueFromPath("/a/b/key/val/c/key/val/key/val/","key","val");
        assertEquals("/a/b/c/", replaced);

    }

}

