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
package org.hippoecm.hst.content.beans;

import java.util.HashSet;
import java.util.TreeSet;

import org.hippoecm.hst.content.beans.standard.HippoItem;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestHippoItem  {

    @Test
    public void testEqualsAndCompareTo() throws Exception {

        HippoItem item1 = new TestItem("item1", "/foo/bar/lux/item1");
        HippoItem itemSame1 = new TestItem("item1", "/foo/bar/lux/item1");
        HippoItem item2 = new TestItem("item2", "/foo/bar/lux/item2");
        HippoItem sns1 = new TestItem("sns", "/foo/bar/lux/sns1");
        HippoItem sns2 = new TestItem("sns", "/foo/bar/lux/sns2[2]");


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

        System.out.println(set.size());
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
