/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.bootstrap;

import javax.jcr.RepositoryException;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAMESPACE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NODETYPESRESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SEQUENCE;
import static org.junit.Assert.assertEquals;

public class InitializeItemComparatorTest {
    
    private InitializeItemComparator comparator = new InitializeItemComparator();
    
    @Test
    public void compareNamespaceWithNamespaceItem() throws Exception {
        InitializeItem a = createInitializeItem("a", HIPPO_NAMESPACE, 1);
        InitializeItem b = createInitializeItem("b", HIPPO_NAMESPACE, 1);
        assertEquals(0, comparator.compare(a, b));
    }
    
    @Test
    public void compareNamespaceWithNonNamespaceItem() throws Exception {
        final InitializeItem a = createInitializeItem("a", HIPPO_NAMESPACE, 2);
        final InitializeItem b = createInitializeItem("b", HIPPO_NODETYPESRESOURCE, 1);
        assertEquals(-1, comparator.compare(a, b));
        assertEquals(1, comparator.compare(b, a));
    }
    
    @Test
    public void compareNodeTypesWithNodeTypesItem() throws Exception {
        InitializeItem a = createInitializeItem("a", HIPPO_NODETYPESRESOURCE, 1);
        InitializeItem b = createInitializeItem("b", HIPPO_NODETYPESRESOURCE, 1);
        assertEquals(0, comparator.compare(a, b));
        b = createInitializeItem("b", HIPPO_NODETYPESRESOURCE, 2);
        assertEquals(-1, comparator.compare(a, b));
        assertEquals(1, comparator.compare(b, a));
    }
    
    @Test
    public void compareNodeTypesWithNonNodeTypesItem() throws Exception {
        final InitializeItem a = createInitializeItem("a", HIPPO_NODETYPESRESOURCE, 2);
        final InitializeItem b = createInitializeItem("b", HIPPO_CONTENTRESOURCE, 1);
        assertEquals(-1, comparator.compare(a, b));
        assertEquals(1, comparator.compare(b, a));
    }
    
    private InitializeItem createInitializeItem(final String name, final String instruction, final double sequence) throws RepositoryException {
        MockNode node = new MockNode(name);
        node.setProperty(instruction, "<dummy>");
        node.setProperty(HIPPO_SEQUENCE, sequence);
        return new InitializeItem(node);
    }
}
