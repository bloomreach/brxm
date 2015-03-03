/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HstLinkLowestDepthFirstAndTheLexicalComparatorTest {

    private final static DefaultHstLinkCreator.LowestDepthFirstAndThenLexicalComparator comparator = new DefaultHstLinkCreator.LowestDepthFirstAndThenLexicalComparator();

    @Test
    public void lowest_depth_first() {
        final List<HstLink> links = new ArrayList<>();
        String[] paths = {"a", "a/b", "a/b/c"};
        for (String path : paths) {
            links.add(new HstLinkImpl(path, null));
        }
        Collections.sort(links, comparator);
        String[] expectedSort = {"a", "a/b", "a/b/c"};
        for (int i = 0; i < 3; i++) {
            assertEquals(links.get(i).getPath(), expectedSort[i]);
        }
    }


    @Test
    public void equal_depth_first_lexical_sorted() {
        final List<HstLink> links = new ArrayList<>();
        String[] paths = {"a", "a/b", "a/b/c", "z/a", "b", "c"};
        for (String path : paths) {
            links.add(new HstLinkImpl(path, null));
        }
        Collections.sort(links, comparator);
        String[] expectedSort = {"a", "b", "c", "a/b", "z/a", "a/b/c"};
        for (int i = 0; i < 6; i++) {
            assertEquals(links.get(i).getPath(), expectedSort[i]);
        }
    }

    @Test
    public void null_path_sorted_last() {
        final List<HstLink> links = new ArrayList<>();

        links.add(new HstLinkImpl(null, null));
        String[] paths = {"a", "a/b", "a/b/c"};
        for (String path : paths) {
            links.add(new HstLinkImpl(path, null));
        }
        links.add(new HstLinkImpl(null, null));

        Collections.sort(links, comparator);
        String[] expectedSort = {"a", "a/b", "a/b/c", null, null};
        for (int i = 0; i < 5; i++) {
            assertEquals(links.get(i).getPath(), expectedSort[i]);
        }
    }
}
