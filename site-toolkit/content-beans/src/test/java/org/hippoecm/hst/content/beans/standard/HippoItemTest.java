/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.content.beans.standard;

import java.util.HashSet;
import java.util.TreeSet;

import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link org.hippoecm.hst.content.beans.standard.HippoItem HippoItem}.
 *
 */
public class HippoItemTest {

    @Test
    public void returnsFalseWhenBothPathsAreNull() {
        HippoItem item1 = new HippoItem();
        HippoItem item2 = new HippoItem();

        assertFalse(item1.equals(item2));
        assertFalse("Multiple calls to equals() are consistent", item1.equals(item2));
    }

    @Test
    public void returnsTrueWhenComparedToSelf() {
        HippoItem item = new HippoItem();

        assertTrue("equals() is reflexive", item.equals(item));
    }

    @Test
    public void returnsFalseWhenComparedToNull() {
        HippoItem item1 = new HippoItem();
        HippoItem item2 = null;

        assertFalse(item1.equals(item2));
    }

    @Test
    public void returnsFalseWhenNotComparedToHippoBean() {
        HippoItem item1 = new HippoItem();
        Object item2 = new Object();

        assertFalse(item1.equals(item2));
    }

    @Test
    public void returnsFalseWhenPathsAreDifferent() {
        JCRValueProvider mockValueProvider1 = createMock(JCRValueProvider.class);
        JCRValueProvider mockValueProvider2 = createMock(JCRValueProvider.class);

        HippoItem item1 = new HippoItem();
        item1.valueProvider = mockValueProvider1;

        HippoItem item2 = new HippoItem();
        item2.valueProvider = mockValueProvider2;

        expect(mockValueProvider1.getPath()).andReturn("/content/documents/1").anyTimes();
        expect(mockValueProvider2.getPath()).andReturn("/content/documents/2").anyTimes();

        replay(mockValueProvider1, mockValueProvider2);

        assertFalse(item1.equals(item2));

        verify(mockValueProvider1, mockValueProvider2);
    }

    @Test
    public void returnsTrueWhenPathsAreTheSame() {
        JCRValueProvider mockValueProvider1 = createMock(JCRValueProvider.class);
        JCRValueProvider mockValueProvider2 = createMock(JCRValueProvider.class);

        HippoItem item1 = new HippoItem();
        item1.valueProvider = mockValueProvider1;

        HippoItem item2 = new HippoItem();
        item2.valueProvider = mockValueProvider2;

        expect(mockValueProvider1.getPath()).andReturn("/content/documents").anyTimes();
        expect(mockValueProvider2.getPath()).andReturn("/content/documents").anyTimes();

        replay(mockValueProvider1, mockValueProvider2);

        assertTrue(item1.equals(item2));
        assertTrue("equals() is symmetric", item2.equals(item1));

        verify(mockValueProvider1, mockValueProvider2);
    }

    @Test
    public void returnsFalseWhenOneOfThePathsIsNull() {
        JCRValueProvider mockValueProvider = createMock(JCRValueProvider.class);

        HippoItem item1 = new HippoItem();

        HippoItem item2 = new HippoItem();
        item2.valueProvider = mockValueProvider;

        replay(mockValueProvider);

        assertFalse(item1.equals(item2));

        verify(mockValueProvider);
    }

    @Test
    public void testEqualsAndCompareTo() throws Exception {

        HippoItem item1 = new TestItem("item1", "/foo/bar/lux/item1");
        HippoItem itemSame1 = new TestItem("item1", "/foo/bar/lux/item1");
        HippoItem item2 = new TestItem("item2", "/foo/bar/lux/item2");
        HippoItem sns1 = new TestItem("sns", "/foo/bar/lux/sns1");
        HippoItem sns2 = new TestItem("sns", "/foo/bar/lux/sns2[2]");
        HippoItem nullPath1 = new TestItem("null", null);
        HippoItem nullPath2 = new TestItem("null", null);


        HippoItem sameNameDiffPath1 = new TestItem("same", "/foo/bar/one");
        HippoItem sameNameDiffPath2 = new TestItem("same", "/foo/bar/two");

        assertTrue(item1.equals(item1));
        assertTrue(item1.equals(itemSame1));
        assertTrue(item1.compareTo(item1) == 0);
        assertTrue(item1.compareTo(itemSame1) == 0);

        assertFalse(item1.equals(item2));
        assertFalse(item1.compareTo(item2) == 0);
        assertFalse(item2.equals(item1));

        assertFalse(sns1.equals(sns2));
        assertFalse(sns2.equals(sns1));

        assertTrue(sns2.compareTo(sns2) == 0);
        assertTrue(sns2.compareTo(sns1) != 0);
        assertTrue(sns1.compareTo(sns2) != 0);

        assertTrue(nullPath1.compareTo(nullPath2) == 0);
        assertTrue(nullPath2.compareTo(nullPath1) == 0);

        assertFalse(sameNameDiffPath1.equals(sameNameDiffPath2));
        assertTrue(sameNameDiffPath1.compareTo(sameNameDiffPath2) != 0);

        // make sure they end up in alphabetical order in a TreeSet and that a TreeSet
        // contains as many items as a HashSet (thus make sure the equals & compareTo are in sync)

        HashSet<HippoItem> set = new HashSet<HippoItem>();
        TreeSet<HippoItem> treeSet = new TreeSet<HippoItem>();

        set.add(item1);
        set.add(itemSame1); // this one is already in the set
        set.add(item2);
        set.add(sns1);
        set.add(sns2);
        set.add(sameNameDiffPath1);
        set.add(sameNameDiffPath2);

        assertTrue(set.size() == 6);

        treeSet.add(item1);
        treeSet.add(itemSame1); // this one is already in the set
        treeSet.add(item2);
        treeSet.add(sns1);
        treeSet.add(sns2);
        treeSet.add(sameNameDiffPath1);
        treeSet.add(sameNameDiffPath2);

        assertTrue(treeSet.size() == 6);

        String name = "a";

        // make sure the ordering is on Name alphabetically, starting with 'a'
        for (HippoItem item : treeSet) {
            assertTrue(name.compareTo(item.getName()) <= 0);
        }

    }


    class TestItem extends HippoItem {

        String name;
        String path;

        TestItem(String name, String path) {
            this.name = name;
            this.path = path;
        }
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPath() {
            return path;
        }
    }

    
}
