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

import org.hippoecm.frontend.plugins.tagging.Tag;
import org.junit.Test;
import static org.junit.Assert.*;


public class TagTest {
    
    @Test
    public void construct() {
        Tag t = new Tag();
        assertTrue(t instanceof Tag);
        t = new Tag("test");
        assertEquals("test", t.getName());
        t = new Tag("test", 3);
        assertEquals("test", t.getName());
        assertEquals(3, t.getScore());
    }
    
    @Test
    public void addScore(){
        Tag t = new Tag();
        t.addScore(2);
        assertEquals(3, t.getScore());
        t.setScore(2);
        t.addScore(3);
        assertEquals(5, t.getScore());
    }
    
    @Test
    public void compare(){
        Tag first = new Tag("number1", 100);
        Tag middel = new Tag("middel", 50);
        assertEquals(1, first.compareTo(middel));
        
        Tag middel2 = new Tag("middeltwo", 50);
        assertEquals(1, middel.compareTo(middel2));
        
        assertEquals(-1, middel.compareTo(first));
    }
}
