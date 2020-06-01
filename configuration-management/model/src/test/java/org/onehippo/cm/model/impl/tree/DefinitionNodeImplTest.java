/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl.tree;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;

import static org.junit.Assert.assertEquals;
import static org.onehippo.cm.model.impl.tree.ConfigurationTreeBuilderTest.collectionToString;

public class DefinitionNodeImplTest {

    @Test
    public void test_reorder_javadoc_examples() throws Exception {
        DefinitionNodeImpl definitionNode = createDefinitionNode("a", "b");
        definitionNode.reorder(createSegmentList("a", "b"));
        assertEquals("[a, b]", collectionToString(definitionNode.getNodes()));

        definitionNode = createDefinitionNode("a", "b", "c", "d");
        definitionNode.reorder(createSegmentList("d", "c"));
        assertEquals("[d, c, a, b]", collectionToString(definitionNode.getNodes()));

        definitionNode = createDefinitionNode("a", "b");
        definitionNode.reorder(createSegmentList("b", "a", "c", "d"));
        assertEquals("[b, a]", collectionToString(definitionNode.getNodes()));
    }

    @Test
    public void test_reorder_expected_or_existing_names_can_omit_index_one() throws Exception {
        DefinitionNodeImpl definitionNode = createDefinitionNode("a", "a[2]");
        definitionNode.reorder(createSegmentList("a[1]", "a[2]"));
        assertEquals("[a, a[2]]", collectionToString(definitionNode.getNodes()));

        definitionNode = createDefinitionNode("a[1]", "a[2]");
        definitionNode.reorder(createSegmentList("a", "a[2]"));
        assertEquals("[a[1], a[2]]", collectionToString(definitionNode.getNodes()));
    }

    private DefinitionNodeImpl createDefinitionNode(final String... childNodeNames) {
        final DefinitionNodeImpl result = new DefinitionNodeImpl(JcrPaths.getPath("/test"), null);
        for (final String childName : childNodeNames) {
            result.addNode(childName);
        }
        return result;
    }

    private List<JcrPathSegment> createSegmentList(final String... segmentNames) {
        final List<JcrPathSegment> result = new ArrayList<>(segmentNames.length);
        for (final String segmentName : segmentNames) {
            result.add(JcrPaths.getSegment(segmentName));
        }
        return result;
    }
}
