/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.path;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cm.model.path.JcrPaths.ROOT;
import static org.onehippo.cm.model.path.JcrPaths.ROOT_NAME;

public class JcrPathTest {

    @Test
    public void root_is_constant() {
        JcrPathSegment rootName = JcrPaths.getSegment("/");
        assertTrue("Parsing '/' should always produce constants ROOT_NAME or ROOT", rootName == ROOT_NAME);

        JcrPath root = JcrPaths.getPath("/");
        assertTrue("Parsing '/' should always produce constants ROOT_NAME or ROOT", root == ROOT);

        root = JcrPaths.getPath("/name").getParent();
        assertTrue("Getting parent of top-level node should always produce constant ROOT", root == ROOT);
    }

    @Test
    public void name_index_zero_equals_one() {
        JcrPathSegment zero = JcrPaths.getSegment("name", 0);
        JcrPathSegment one = JcrPaths.getSegment("name", 1);

        assertTrue("name and name[1] should be considered equal", zero.equals(one));

        assertEquals("name and name[1] should be round-trip convertible",
                zero.toString(), one.suppressIndex().toString());

        assertEquals("name and name[1] should be round-trip convertible",
                one.toString(), zero.forceIndex().toString());

        assertEquals("name and name[1] should be round-trip convertible",
                zero.toString(), zero.forceIndex().suppressIndex().toString());

        assertEquals("name and name[1] should be round-trip convertible",
                one.toString(), one.suppressIndex().forceIndex().toString());

        Set<JcrPathSegment> set = new HashSet<>();
        set.add(zero);

        assertTrue("HashSet containing zero should automatically also contain one", set.contains(one));

        set.remove(zero);
        set.add(one);

        assertTrue("HashSet containing one should automatically also contain zero", set.contains(zero));

        set = new TreeSet<>();
        set.add(zero);

        assertTrue("TreeSet containing zero should automatically also contain one", set.contains(one));

        set.remove(zero);
        set.add(one);

        assertTrue("TreeSet containing one should automatically also contain zero", set.contains(zero));
    }

    @Test
    public void minimally_indexed_path_equals_fully_indexed_path() {
        JcrPath minimal = JcrPaths.getPath("/one/two/three/four");
        JcrPath full = JcrPaths.getPath("/one[1]/two[1]/three[1]/four[1]");

        assertTrue("minimally-indexed path and fully-indexed path should be considered equal",
                minimal.equals(full));

        assertEquals("minimally- and fully-indexed paths should be round-trip convertible",
                minimal.toString(), full.suppressIndices().toString());

        assertEquals("minimally- and fully-indexed paths should be round-trip convertible",
                full.toString(), minimal.forceIndices().toString());

        assertEquals("minimally- and fully-indexed paths should be round-trip convertible",
                minimal.toString(), minimal.forceIndices().suppressIndices().toString());

        assertEquals("minimally- and fully-indexed paths should be round-trip convertible",
                full.toString(), full.suppressIndices().forceIndices().toString());

        Set<JcrPath> set = new HashSet<>();
        set.add(minimal);

        assertTrue("HashSet containing minimally-indexed path should automatically also contain fully-indexed path",
                set.contains(full));

        set.remove(minimal);
        set.add(full);

        assertTrue("HashSet containing fully-indexed path should automatically also contain minimally-indexed path",
                set.contains(minimal));

        set = new TreeSet<>();
        set.add(minimal);

        assertTrue("TreeSet containing minimally-indexed path should automatically also contain fully-indexed path",
                set.contains(full));

        set.remove(minimal);
        set.add(full);

        assertTrue("TreeSet containing fully-indexed path should automatically also contain minimally-indexed path",
                set.contains(minimal));
    }

    @Test
    public void starts_with() {
        assertTrue(JcrPaths.getPath("/my/test/path").startsWith("/my/test/"));
        assertTrue(JcrPaths.getPath("/my/test/path").startsWith("my/test/"));
        assertTrue(JcrPaths.getPath("/my/test/path").startsWith("/"));

        assertTrue(JcrPaths.getPath("/my/test/path").startsWith(JcrPaths.getPath("/my/test/")));
        assertTrue(JcrPaths.getPath("/my/test/path").startsWith(JcrPaths.getPath("my/test/")));
        assertTrue(JcrPaths.getPath("/my/test/path").startsWith(JcrPaths.getPath("/")));

        assertTrue(JcrPaths.getPath("/my/test/path").startsWith(JcrPaths.getSegment("/my")));
        assertTrue(JcrPaths.getPath("/my/test/path").startsWith(JcrPaths.getSegment("my")));
        assertTrue(JcrPaths.getPath("/my/test/path").startsWith(JcrPaths.getSegment("/")));
    }

    @Test
    public void ends_with() {
        assertTrue(JcrPaths.getPath("/my/test/path").endsWith("/test/path"));
        assertTrue(JcrPaths.getPath("/my/test/path").endsWith("test/path"));
        assertTrue(JcrPaths.getPath("/my/test/path").endsWith("/"));

        assertTrue(JcrPaths.getPath("/my/test/path").endsWith(JcrPaths.getPath("/test/path")));
        assertTrue(JcrPaths.getPath("/my/test/path").endsWith(JcrPaths.getPath("test/path")));
        assertTrue(JcrPaths.getPath("/my/test/path").endsWith(JcrPaths.getPath("/")));

        assertTrue(JcrPaths.getPath("/my/test/path").endsWith(JcrPaths.getSegment("/path")));
        assertTrue(JcrPaths.getPath("/my/test/path").endsWith(JcrPaths.getSegment("path")));
        assertTrue(JcrPaths.getPath("/my/test/path").endsWith(JcrPaths.getSegment("/")));
    }
}
