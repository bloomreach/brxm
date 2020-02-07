/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.editor.plugins.field;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlagListTest {

    private FlagList flagList;

    @Before
    public void setUp() {
        flagList = new FlagList();
    }


    @Test
    public void move() {

        flagList.set(0, true);
        assertTrue(flagList.get(0));
        assertFalse(flagList.get(1));

        flagList.moveUp(1);
        assertFalse(flagList.get(0));
        assertTrue(flagList.get(1));

        flagList.set(1, false);
        assertFalse(flagList.get(0));
        assertFalse(flagList.get(1));

        flagList.set(2, true);
        flagList.moveBy(2, 0);
        assertFalse(flagList.get(0));
        assertFalse(flagList.get(1));
        assertTrue(flagList.get(2));

        flagList.moveUp(2);
        assertFalse(flagList.get(0));
        assertTrue(flagList.get(1));
        assertFalse(flagList.get(2));

        flagList.moveDown(1);
        flagList.moveTo(2, 0);
        assertTrue(flagList.get(0));
        assertFalse(flagList.get(1));
        assertFalse(flagList.get(2));

        flagList.set(100, true);
        assertTrue(flagList.get(0));
        assertFalse(flagList.get(1));
        assertFalse(flagList.get(2));
        assertTrue(flagList.get(100));

        flagList.moveTo(100, 0);
        assertTrue(flagList.get(0));
        assertTrue(flagList.get(1));
        assertFalse(flagList.get(2));
        assertFalse(flagList.get(100));

        flagList.set(100, true);
        assertTrue(flagList.get(0));
        assertTrue(flagList.get(1));
        assertFalse(flagList.get(2));
        assertTrue(flagList.get(100));

        flagList.remove(0);
        assertTrue(flagList.get(0));
        assertFalse(flagList.get(1));
        assertFalse(flagList.get(2));
        assertTrue(flagList.get(99));
        assertFalse(flagList.get(101));
    }

    @Test
    public void remove() {
        flagList.set(0, true);
        flagList.set(2, true);

        flagList.remove(0);
        assertFalse(flagList.get(0));
        assertTrue(flagList.get(1));

        flagList.set(2, true);
        assertFalse(flagList.get(0));
        assertTrue(flagList.get(1));
        assertTrue(flagList.get(2));

        flagList.remove(1);
        assertFalse(flagList.get(0));
        assertTrue(flagList.get(1));
        assertFalse(flagList.get(2));
    }

    @Test
    public void testToggle() {
        flagList.set(7, true);
        assertTrue(flagList.get(7));
        flagList.set(7, false);
        assertFalse(flagList.get(7));
    }

    @Test
    public void testSetTwice() {
        flagList.set(17, true);
        flagList.set(17, true);
        assertTrue(flagList.get(17));

        flagList.set(17, false);
        flagList.set(17, false);
        assertFalse(flagList.get(17));
    }

    @Test
    public void testRemoveRemovesItemAndUpdatesSubsequentItems() {
        flagList.set(2, true);
        flagList.set(7, true);
        flagList.set(8, true);
        flagList.set(10, true);

        flagList.remove(7);

        assertTrue(flagList.get(2));
        assertTrue(flagList.get(7));
        assertFalse(flagList.get(8));
        assertTrue(flagList.get(9));
    }

    @Test
    public void testUpdateMoveToTop() {
        flagList.set(0, true);
        flagList.set(1, true);
        flagList.set(7, true);

        flagList.moveTo(7, 0);

        assertTrue(flagList.get(0));
        assertTrue(flagList.get(1));
        assertTrue(flagList.get(2));
        assertFalse(flagList.get(7));
    }

    @Test
    public void testUpdateMoveUp() {
        flagList.set(7, true);
        flagList.set(9, true);
        flagList.set(17, true);

        flagList.moveUp(9);

        assertTrue(flagList.get(7));
        assertTrue(flagList.get(8));
        assertFalse(flagList.get(9));
        assertTrue(flagList.get(17));
    }

    @Test
    public void testUpdateMoveToBottom() {
        flagList.set(7, true);
        flagList.set(17, true);
        flagList.set(19, true);

        flagList.moveTo(7, 19);

        assertFalse(flagList.get(7));
        assertTrue(flagList.get(16));
        assertTrue(flagList.get(18));
        assertTrue(flagList.get(19));
    }

    @Test
    public void testUpdateMoveDown() {
        flagList.set(1, true);
        flagList.set(7, true);
        flagList.set(9, true);

        flagList.moveDown(7);

        assertTrue(flagList.get(1));
        assertFalse(flagList.get(7));
        assertTrue(flagList.get(8));
        assertTrue(flagList.get(9));
    }
}
