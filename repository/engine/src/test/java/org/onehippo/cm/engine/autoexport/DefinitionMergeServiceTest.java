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

package org.onehippo.cm.engine.autoexport;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.onehippo.cm.model.impl.path.JcrPathSegment;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cm.engine.autoexport.DefinitionMergeService.getIncorrectlyOrdered;

public class DefinitionMergeServiceTest {

    @Test
    public void test_getIncorrectlyOrdered() {
        expect("[]", "[]", "[]");
        expect("[a]", "[a]", "[]");
        expect("[a, b]", "[a, b]", "[]");
        expect("[a, b, c]", "[a, b, c]", "[]");

        // ignore items in expected that are not in intermediate yet
        expect("[i1, a, i2, b, i3]", "[a, b]", "[]");

        // expect that items flagged as incorrectly ordered can be inserted before a correctly ordered item
        expect("[a, b]", "[b, a]", "[a]");
        expect("[a, b, c]", "[b, c, a]", "[a]");
        expect("[a, b, c]", "[c, b, a]", "[b, a]");
        expect("[a, b, c]", "[c, a, b]", "[a, b]");

        expect("[a, b, c, d, e]", "[b, a, c, e, d]", "[a, d]");

        expect("[a[1], a[2], b]", "[b, a[1], a[2]]", "[a[1], a[2]]");
    }

    private void expect(final String expectedOrder, final String intermediateOrder, final String incorrect) {
        final ImmutableList<JcrPathSegment> expectedOrderList = parse(expectedOrder);
        final List<JcrPathSegment> intermediateOrderList = parse(intermediateOrder);
        assertEquals(incorrect, getIncorrectlyOrdered(expectedOrderList, intermediateOrderList).toString());
    }

    private ImmutableList<JcrPathSegment> parse(final String string) {
        final List<JcrPathSegment> result = new ArrayList<>();
        for (final String segment : string.substring(1, string.length() - 1).split(",")) {
            result.add(JcrPathSegment.get(segment.trim()));
        }
        return ImmutableList.copyOf(result);
    }
}
