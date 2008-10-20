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
        assertEquals(2, test1.getScore());
        
        Tag tag3 = new Tag("test3");
        col.add(tag3);
        Tag tag22 = new Tag("test2", 5);
        col.add(tag22);
        
        assertEquals(3, col.size());
        Tag test2 = col.get(tag22.getName());
        assertEquals(6, test2.getScore());
    }
    
    @Test
    public void addAll(){
        TagCollection col1 = new TagCollection();
        col1.add(new Tag("tag1"));
        col1.add(new Tag("tag2"));
        col1.add(new Tag("tag3"));
        
        TagCollection col2 = new TagCollection();
        col2.setMultiplier(2);
        col2.add(new Tag("test1"));
        col2.add(new Tag("test2", 2));
        col2.add(new Tag("tag1"));
        col2.add(new Tag("tag2", 2));
        
        col1.addAll(col2);
        
        assertEquals(5, col1.size());
        Tag tag1 = col1.getTag(new Tag("tag1"));
        assertEquals("tag1", tag1.getName());
        assertEquals(3, tag1.getScore());
        assertEquals(5, col1.getTag(new Tag("tag2")).getScore());
        assertEquals(4, col1.getTag(new Tag("test2")).getScore());
    }
}
