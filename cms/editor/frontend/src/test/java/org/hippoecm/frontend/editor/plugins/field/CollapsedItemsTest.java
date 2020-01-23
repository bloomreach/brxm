/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollapsedItemsTest {
    CollapsedItems collapsed;

    @Before
    public void setUp() {
        collapsed = new CollapsedItems();
    }

    @Test
    public void testToggle() {
        collapsed.set(7, true);
        assertTrue(collapsed.contains(7));
        collapsed.set(7, false);
        assertFalse(collapsed.contains(7));
    }

    @Test
    public void testSetTwice() {
        collapsed.set(17, true);
        collapsed.set(17, true);
        assertTrue(collapsed.contains(17));

        collapsed.set(17, false);
        collapsed.set(17, false);
        assertFalse(collapsed.contains(17));
    }

    @Test
    public void testClearRemovesItemAndUpdatesSubsequentItems() {
        collapsed.set(2, true);
        collapsed.set(7, true);
        collapsed.set(8, true);
        collapsed.set(10, true);

        collapsed.clear(7, 20);

        assertTrue(collapsed.contains(2));
        assertTrue(collapsed.contains(7));
        assertFalse(collapsed.contains(8));
        assertTrue(collapsed.contains(9));
    }

    @Test
    public void testUpdateMoveToTop() {
        collapsed.set(0, true);
        collapsed.set(1, true);
        collapsed.set(7, true);

        collapsed.update(7, 0, 10);

        assertTrue(collapsed.contains(0));
        assertTrue(collapsed.contains(1));
        assertTrue(collapsed.contains(2));
        assertFalse(collapsed.contains(7));
    }

    @Test
    public void testUpdateMoveUp() {
        collapsed.set(7, true);
        collapsed.set(9, true);
        collapsed.set(17, true);

        collapsed.update(9, 8, 20);

        assertTrue(collapsed.contains(7));
        assertTrue(collapsed.contains(8));
        assertFalse(collapsed.contains(9));
        assertTrue(collapsed.contains(17));
    }

    @Test
    public void testUpdateMoveToBottom() {
        collapsed.set(7, true);
        collapsed.set(17, true);
        collapsed.set(19, true);

        collapsed.update(7, -1, 20);

        assertFalse(collapsed.contains(7));
        assertTrue(collapsed.contains(16));
        assertTrue(collapsed.contains(18));
        assertTrue(collapsed.contains(19));
    }

    @Test
    public void testUpdateMoveDown() {
        collapsed.set(1, true);
        collapsed.set(7, true);
        collapsed.set(9, true);

        collapsed.update(7, 8, 10);

        assertTrue(collapsed.contains(1));
        assertFalse(collapsed.contains(7));
        assertTrue(collapsed.contains(8));
        assertTrue(collapsed.contains(9));
    }
}
