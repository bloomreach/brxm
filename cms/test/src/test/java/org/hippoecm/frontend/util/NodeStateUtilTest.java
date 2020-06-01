/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.hippoecm.frontend.util.NodeStateUtil;
import org.junit.Test;

public class NodeStateUtilTest {

    @Test
    public void testAdded() {
        List<String> old = Arrays.asList(new String[] { "2", "4" });
        
        List<String> nu = Arrays.asList(new String[] { "2", "4", "6"});
        assertEquals(Arrays.asList(new String[] { "6" }), NodeStateUtil.added(old, nu));
        
        nu = Arrays.asList(new String[] { "0", "2", "4" });
        assertEquals(Arrays.asList(new String[] { "0" }), NodeStateUtil.added(old, nu));
        
        nu = Arrays.asList(new String[] { "2", "3", "4" });
        assertEquals(Arrays.asList(new String[] { "3" }), NodeStateUtil.added(old, nu));
        
        nu = Arrays.asList(new String[] { "0", "3", "6" });
        assertEquals(Arrays.asList(new String[] { "0", "3", "6" }), NodeStateUtil.added(old, nu));
        
        nu = Arrays.asList(new String[] { "2", "4" });
        assertNull(NodeStateUtil.added(old, nu));
    }
    
    @Test
    public void testRemoved() {
        List<String> old = Arrays.asList(new String[] { "0", "2", "4", "6" });
        
        List<String> nu = Arrays.asList(new String[] { "2", "4", "6" });
        assertEquals(Arrays.asList(new String[] { "0" }), NodeStateUtil.removed(old, nu));
        
        nu = Arrays.asList(new String[] { "0", "2", "4" });
        assertEquals(Arrays.asList(new String[] { "6" }), NodeStateUtil.removed(old, nu));
        
        nu = Arrays.asList(new String[] { "0", "6" });
        assertEquals(Arrays.asList(new String[] { "2", "4" }), NodeStateUtil.removed(old, nu));
        
        nu = Arrays.asList(new String[] { "0", "2", "4", "6" });
        assertNull(NodeStateUtil.removed(old, nu));
    }
    
    @Test
    public void testMoved() {
        List<String> old = Arrays.asList(new String[] { "0", "2", "4", "6" });
        
        List<String> nu = Arrays.asList(new String[] { "0", "4", "2", "6" });
        assertEquals(Arrays.asList(new String[] { "2", "4" }), NodeStateUtil.moved(old, nu, null, null));
        
        nu = Arrays.asList(new String[] { "0", "2", "6", "4" });
        assertEquals(Arrays.asList(new String[] { "4", "6" }), NodeStateUtil.moved(old, nu, null, null));
        
        List<String> removed = Arrays.asList(new String[] { "0" });
        nu = Arrays.asList(new String[] { "2", "4", "6" });
        assertNull(NodeStateUtil.moved(old, nu, null, removed));
        
        nu = Arrays.asList(new String[] { "2", "6", "4" });
        assertEquals(Arrays.asList(new String[] { "4", "6" }), NodeStateUtil.moved(old, nu, null, removed));
        
        removed = Arrays.asList(new String[] { "4" });
        nu = Arrays.asList(new String[] { "0", "6", "2" });
        assertEquals(Arrays.asList(new String[] { "2", "6" }), NodeStateUtil.moved(old, nu, null, removed));
        
        List<String> added = Arrays.asList(new String[] { "8" });
        nu = Arrays.asList(new String[] { "0", "2", "4", "6", "8" });
        assertNull(NodeStateUtil.moved(old, nu, added, null));
        
        nu = Arrays.asList(new String[] { "0", "4", "2", "6", "8" });
        assertEquals(Arrays.asList(new String[] { "2", "4" }), NodeStateUtil.moved(old, nu, added, null));
        
        nu = Arrays.asList(new String[] { "2", "4", "0", "6", "8" });
        assertEquals(Arrays.asList(new String[] { "0", "2", "4" }), NodeStateUtil.moved(old, nu, added, null));
        
        nu = Arrays.asList(new String[] { "0", "2", "4", "8", "6" });
        assertNull(NodeStateUtil.moved(old, nu, added, null));
        
        nu = Arrays.asList(new String[] { "0", "2", "6", "8" });
        assertNull(NodeStateUtil.moved(old, nu, added, removed));
        
        nu = Arrays.asList(new String[] { "0", "6", "2", "8" });
        assertEquals(Arrays.asList(new String[] { "2", "6" }), NodeStateUtil.moved(old, nu, added, removed));
    }
}
