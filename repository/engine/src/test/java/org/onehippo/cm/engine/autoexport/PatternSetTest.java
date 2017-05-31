/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PatternSetTest {

    @Test
    public void wildcardCompileTest() {
        assertEquals("/foo:bar/.*", PatternSet.compile("/foo:bar/**"));
        assertEquals("/foo:bar/.*/foo:bar", PatternSet.compile("/foo:bar/**/foo:bar"));
        assertEquals("/foo:bar/[^/]*", PatternSet.compile("/foo:bar/*"));
        assertEquals("/foo:bar/[^/]*/foo:bar", PatternSet.compile("/foo:bar/*/foo:bar"));
        assertEquals("/foo:bar/[^/]*/[^/]*", PatternSet.compile("/foo:bar/*/*"));
        assertEquals("/foo:bar/[^/]*/.*", PatternSet.compile("/foo:bar/*/**"));
        assertEquals("/foo:bar/.*/[^/]*", PatternSet.compile("/foo:bar/**/*"));
    }

    @Test
    public void wildcardMatchesTest() {
        List<String> patterns;
        PatternSet PatternSet;

        patterns = Arrays.asList("/foo:bar/**");
        PatternSet = new PatternSet(patterns);
        assertTrue(PatternSet.matches("/foo:bar/"));
        assertTrue(PatternSet.matches("/foo:bar/quz"));
        assertTrue(PatternSet.matches("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/foo:bar/*");
        PatternSet = new PatternSet(patterns);
        assertTrue(PatternSet.matches("/foo:bar/"));
        assertTrue(PatternSet.matches("/foo:bar/quz"));
        assertFalse(PatternSet.matches("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/foo:bar/**/baz");
        PatternSet = new PatternSet(patterns);
        assertFalse(PatternSet.matches("/foo:bar/"));
        assertFalse(PatternSet.matches("/foo:bar/quz"));
        assertTrue(PatternSet.matches("/foo:bar/quz/baz"));
        assertTrue(PatternSet.matches("/foo:bar/quz/quz:baz/baz"));

        patterns = Arrays.asList("/foo:bar/*/baz");
        PatternSet = new PatternSet(patterns);
        assertFalse(PatternSet.matches("/foo:bar/"));
        assertFalse(PatternSet.matches("/foo:bar/quz"));
        assertTrue(PatternSet.matches("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/foo:bar/*/*");
        PatternSet = new PatternSet(patterns);
        assertFalse(PatternSet.matches("/foo:bar/"));
        assertFalse(PatternSet.matches("/foo:bar/quz"));
        assertTrue(PatternSet.matches("/foo:bar/quz/baz"));

        patterns = Arrays.asList("/hst:hst/hst:configurations/*-preview**");
        PatternSet = new PatternSet(patterns);
        assertTrue(PatternSet.matches("/hst:hst/hst:configurations/project-preview"));
        assertTrue(PatternSet.matches("/hst:hst/hst:configurations/project-preview/hst:components"));
    }
}
