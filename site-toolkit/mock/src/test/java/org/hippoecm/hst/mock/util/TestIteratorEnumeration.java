/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.mock.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;

public class TestIteratorEnumeration {
    
    @Test
    public void testEnumeration() {
        List<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(new String [] { "one", "two", "three" }));
        Enumeration<String> e = new IteratorEnumeration<String>(list.iterator());
        
        assertTrue(e.hasMoreElements());
        assertEquals("one", e.nextElement());
        assertTrue(e.hasMoreElements());
        assertEquals("two", e.nextElement());
        assertTrue(e.hasMoreElements());
        assertEquals("three", e.nextElement());
        assertFalse(e.hasMoreElements());
    }
    
}
