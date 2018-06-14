/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.platform.configuration.sitemap.HstSiteMapItemService;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestHstSiteMapItemIntegerPlaceHolders {

    @Test
    public void testContainsNonIntegerPlaceholders() {

        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders(null));
        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${1}"));
        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${1}${2}"));
        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${14}"));
        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${1}ff${255}"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${}"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${asdsad"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${1"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${foo}"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${1}${foo}"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${23foo}"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${1}h${f23oo}"));
        // we do not so much check invalid property place holders (yet)
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${aa"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("/a/b/${aa/${1}"));
        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("a"));
        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("$"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("${"));
        assertTrue(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("${}"));
        assertFalse(HstSiteMapItemService.containsInvalidOrNonIntegerPlaceholders("${1}"));
    }
}
