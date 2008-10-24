/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.tagging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.hippoecm.frontend.plugins.tagging.Tag;
import org.hippoecm.frontend.plugins.tagging.TagCollection;
import org.junit.Test;

public class TagCollectionTest {
    
    @Test
    public void add(){
        TagCollection col = new TagCollection();
        
        Tag tag1 = new Tag("test1");
        col.add(tag1);
        Tag tag2 = new Tag("test2");
        col.add(tag2);
        assertEquals(2, col.size());
        
        Tag tag1dubbel = new Tag("test1");
        boolean False = col.add(tag1dubbel);
        assertFalse(False);
        assertEquals(2, col.size());
        Tag test1 = col.getTag(tag1);
        assertEquals("test1", test1.getName());
        assertEquals(2.0, test1.getScore());
        
        Tag tag3 = new Tag("test3");
        col.add(tag3);
        Tag tag22 = new Tag("test2", 5);
        col.add(tag22);
        
        assertEquals(3, col.size());
        Tag test2 = col.get(tag22.getName());
        assertEquals(6.0, test2.getScore());
    }
    
    @Test
    public void normalize(){
        // create 1 collection with tags and normalize
        TagCollection col = new TagCollection();
        col.add(new Tag("tag1", 0.0));
        col.add(new Tag("tag2", 100.0));
        col.normalizeScores();
        assertEquals(0.0, col.get("tag1").getScore());
        assertEquals(100.0, col.get("tag2").getScore());
        // create an other collection with another multiplier and add it (does implicit normalize on col2)
        TagCollection col2 = new TagCollection();
        col2.add(new Tag("tag3", 25.0));
        col2.add(new Tag("tag4", 50.0));
        col2.setMultiplier(2);
        col.addAll(col2);
        assertEquals(200.0, col.get("tag4").getScore());
        assertEquals(100.0, col.get("tag3").getScore());
        // now normalize main collection (again)
        col.normalizeScores();
        assertEquals(100.0, col.get("tag4").getScore());
        assertEquals(50.0, col.get("tag3").getScore());
        assertEquals(50.0, col.get("tag2").getScore());
    }
    
    @Test
    public void addAll(){
        TagCollection col1 = new TagCollection();
        col1.add(new Tag("tag1")); // score 1
        col1.add(new Tag("tag2")); // score 1
        col1.add(new Tag("tag3")); // score 1
        
        TagCollection col2 = new TagCollection();
        // will double the scores of this collection (after normalization)
        col2.setMultiplier(2);
        col2.add(new Tag("test1")); // score 1, after norm. 50.0
        col2.add(new Tag("test2", 2)); // score 2, after norm. 100.0
        col2.add(new Tag("tag1")); // score 1, after norm. 50.0
        col2.add(new Tag("tag2", 2)); // score 1, after norm. 100.0
        
        col1.addAll(col2); // normalizes, multiplies and adds
        
        assertEquals(5, col1.size());
        Tag tag1 = col1.get("tag1");
        assertEquals("tag1", tag1.getName());
        assertEquals(101.0, tag1.getScore());
        assertEquals(201.0, col1.get("tag2").getScore());
        assertEquals(200.0, col1.get("test2").getScore());
    }
    
    @Test
    public void ToString(){
        TagCollection col = new TagCollection();
        col.add(new Tag("tag1"));
        col.add(new Tag("tag2"));
        assertEquals("tag1, tag2, ", col.toString());
    }
}
