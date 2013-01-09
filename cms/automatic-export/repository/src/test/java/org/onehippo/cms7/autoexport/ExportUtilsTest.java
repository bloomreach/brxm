/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class ExportUtilsTest {

    @Test
    public void testGetSubModuleExclusionPatterns() throws Exception {
        Map<String, Collection<String>> modules = new HashMap<String, Collection<String>>();
        modules.put("foo", Arrays.asList("/"));
        modules.put("bar", Arrays.asList("/bar", "/quz"));
        Configuration configuration = new Configuration(true, modules, null, null);
        Module foo = new Module("foo", Arrays.asList("/"), new File("foo"), null, null, configuration);
        List<String> subModuleExclusionPatterns = ExportUtils.getSubModuleExclusionPatterns(configuration, foo);
        assertTrue(subModuleExclusionPatterns.contains("/bar"));
        assertTrue(subModuleExclusionPatterns.contains("/bar/**"));
        assertTrue(subModuleExclusionPatterns.contains("/quz"));
        assertTrue(subModuleExclusionPatterns.contains("/quz/**"));
        assertFalse(subModuleExclusionPatterns.contains("/foo"));
    }

    @Test
    public void testGetBestMatchingInitializeItem() throws Exception {
        Collection<InitializeItem> candidates = null;
        Map<String, Collection<String>> modules = new HashMap<String, Collection<String>>();
        modules.put("foo", Arrays.asList("/"));
        modules.put("bar", Arrays.asList("/bar"));
        Configuration configuration = new Configuration(true, modules, null, null);
        Module foo = new Module("foo", Arrays.asList("/"), new File("foo"), null, null, configuration);
        Module bar = new Module("bar", Arrays.asList("/bar"), new File("bar"), null, null, configuration);

        InitializeItem candidate1 = new InitializeItem("candidate1", true, foo);
        InitializeItem candidate2 = new InitializeItem("candidate2", true, bar);
        candidates = Arrays.asList(candidate1, candidate2);

        // choose candidate in mapped module
        InitializeItem result = ExportUtils.getBestMatchingInitializeItem(candidates, bar);
        assertEquals(candidate2, result);

        candidate1 = new InitializeItem("candidate1", true, bar);
        candidate2 = new InitializeItem("candidate2", false, bar);
        candidates = Arrays.asList(candidate1, candidate2);

        // choose candidate in mapped module that is enabled
        result = ExportUtils.getBestMatchingInitializeItem(candidates, bar);
        assertEquals(candidate1, result);

        // if none of the candidate are in the mapped module return null
        result = ExportUtils.getBestMatchingInitializeItem(candidates, foo);
        assertNull(result);
    }

}
