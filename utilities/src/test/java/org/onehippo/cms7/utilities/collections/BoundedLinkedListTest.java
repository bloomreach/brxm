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
package org.onehippo.cms7.utilities.collections;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BoundedLinkedListTest {

    @Test
    public void testCreateWithSizeZero() {
        try {
            new BoundedLinkedList<Integer>(0);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException iae) {
            assertTrue(true);
        }
    }

    @Test
    public void testAdd() {
        List<Integer> integers = new BoundedLinkedList<Integer>(3);

        fill(integers, 6);
        assertTrue(Arrays.asList(3, 4, 5).equals(integers));
    }

    @Test
    public void testAddWithIndex() {
        List<Integer> integers = new BoundedLinkedList<Integer>(1);

        try {
            integers.add(1, 1);
            fail("UnsupportedOperationException should have been thrown");
        } catch (UnsupportedOperationException uoe) {
            assertTrue(true);
        }
    }

    @Test
    public void testAddAll() {
        List<Integer> integers = new BoundedLinkedList<Integer>(3);

        integers.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertTrue(Arrays.asList(7, 8, 9).equals(integers));
    }

    @Test
    public void testAddAllWithIndex() {
        List<Integer> integers = new BoundedLinkedList<Integer>(1);

        try {
            integers.addAll(3, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
            fail("UnsupportedOperationException should have been thrown");
        } catch (UnsupportedOperationException uoe) {
            assertTrue(true);
        }
    }

    @Test
    public void testAddFirst() {
        List<Integer> integers = new BoundedLinkedList<Integer>(1);

        fill(integers, 3);
        ((Deque) integers).addFirst(5);
        assertTrue(Arrays.asList(5).equals(integers));
    }

    @Test
    public void testAddLast() {
        List<Integer> integers = new BoundedLinkedList<Integer>(3);

        fill(integers, 3);
        ((Deque) integers).addLast(5);
        assertTrue(Arrays.asList(1, 2, 5).equals(integers));
    }

    @Test
    public void testOffer() {
        List<Integer> integers = new BoundedLinkedList<Integer>(3);

        fill(integers, 3);
        ((Queue) integers).offer(5);
        assertTrue(Arrays.asList(1, 2, 5).equals(integers));
    }

    @Test
    public void testOfferFirst() {
        List<Integer> integers = new BoundedLinkedList<Integer>(3);

        fill(integers, 3);
        ((Deque) integers).offerFirst(5);
        assertTrue(Arrays.asList(5, 1, 2).equals(integers));
    }

    @Test
    public void testOfferLast() {
        List<Integer> integers = new BoundedLinkedList<Integer>(3);

        fill(integers, 3);
        ((Deque) integers).offerLast(5);
        assertTrue(Arrays.asList(1, 2, 5).equals(integers));
    }

    @Test
    public void testPush() {
        List<Integer> integers = new BoundedLinkedList<Integer>(3);

        fill(integers, 3);
        ((Deque) integers).push(5);
        assertTrue(Arrays.asList(5, 1, 2).equals(integers));
    }

    private void fill(List<Integer> integers, int max) {
        for (int i = 0; i < max; i++) {
            integers.add(i);
        }
    }

}
