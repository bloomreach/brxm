/*
 *  Copyright 2012 Hippo.
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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ExclusionContextTest {

    @Test
    public void wildcardCompileTest() {
        assertEquals("/foo:bar/.*", ExclusionContext.compile("/foo:bar/**"));
        assertEquals("/foo:bar/.*/foo:bar", ExclusionContext.compile("/foo:bar/**/foo:bar"));
        assertEquals("/foo:bar/[^/]*", ExclusionContext.compile("/foo:bar/*"));
        assertEquals("/foo:bar/[^/]*/foo:bar", ExclusionContext.compile("/foo:bar/*/foo:bar"));
        assertEquals("/foo:bar/[^/]*/[^/]*", ExclusionContext.compile("/foo:bar/*/*"));
        assertEquals("/foo:bar/[^/]*/.*", ExclusionContext.compile("/foo:bar/*/**"));
        assertEquals("/foo:bar/.*/[^/]*", ExclusionContext.compile("/foo:bar/**/*"));
    }

    @Test
    public void wildcardMatchesTest() {
        List<String> patterns;
        ExclusionContext ExclusionContext;

        patterns = Arrays.asList("/foo:bar/**");
        ExclusionContext = new ExclusionContext(patterns);
        assertTrue(ExclusionContext.isExcluded("/foo:bar/"));
        assertTrue(ExclusionContext.isExcluded("/foo:bar/quz"));
        assertTrue(ExclusionContext.isExcluded("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/foo:bar/*");
        ExclusionContext = new ExclusionContext(patterns);
        assertTrue(ExclusionContext.isExcluded("/foo:bar/"));
        assertTrue(ExclusionContext.isExcluded("/foo:bar/quz"));
        assertFalse(ExclusionContext.isExcluded("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/foo:bar/**/baz");
        ExclusionContext = new ExclusionContext(patterns);
        assertFalse(ExclusionContext.isExcluded("/foo:bar/"));
        assertFalse(ExclusionContext.isExcluded("/foo:bar/quz"));
        assertTrue(ExclusionContext.isExcluded("/foo:bar/quz/baz"));
        assertTrue(ExclusionContext.isExcluded("/foo:bar/quz/quz:baz/baz"));

        patterns = Arrays.asList("/foo:bar/*/baz");
        ExclusionContext = new ExclusionContext(patterns);
        assertFalse(ExclusionContext.isExcluded("/foo:bar/"));
        assertFalse(ExclusionContext.isExcluded("/foo:bar/quz"));
        assertTrue(ExclusionContext.isExcluded("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/foo:bar/*/*");
        ExclusionContext = new ExclusionContext(patterns);
        assertFalse(ExclusionContext.isExcluded("/foo:bar/"));
        assertFalse(ExclusionContext.isExcluded("/foo:bar/quz"));
        assertTrue(ExclusionContext.isExcluded("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/hst:hst/hst:configurations/*-preview**");
        ExclusionContext = new ExclusionContext(patterns);
        assertTrue(ExclusionContext.isExcluded("/hst:hst/hst:configurations/project-preview"));
        assertTrue(ExclusionContext.isExcluded("/hst:hst/hst:configurations/project-preview/hst:components"));
    }
}
