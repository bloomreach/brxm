/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.service.categories;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class AbstractCategoriesBuilderTest {

    private AbstractCategoriesBuilder categoriesBuilder;

    @Before
    public void setUp() {
        categoriesBuilder = new AbstractCategoriesBuilder() {
            @Override
            public String[] build() {
                return new String[0];
            }
        };
    }

    @Test()
    public void testFrozenNode() {
        final MockNode notFrozenNode = new MockNode("not-frozen");
        final MockNode frozenNode = new MockNode("frozen", NT_FROZEN_NODE);

        assertTrue(categoriesBuilder.isFrozenNode(frozenNode));
        assertFalse(categoriesBuilder.isFrozenNode(notFrozenNode));
    }

    @Test()
    public void testHandleNode() {
        final MockNode notHandleNode = new MockNode("not-handle");
        final MockNode handleNode = new MockNode("handle", NT_HANDLE);

        assertTrue(categoriesBuilder.isHandle(handleNode));
        assertFalse(categoriesBuilder.isHandle(notHandleNode));
    }
}
