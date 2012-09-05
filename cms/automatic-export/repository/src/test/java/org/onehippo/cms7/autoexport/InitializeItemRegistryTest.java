/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.autoexport;

import javax.jcr.observation.Event;

import static org.junit.Assert.*;

import org.junit.Test;

public class InitializeItemRegistryTest {

    @Test
    public void testInitializeItemRegistryLookupByContextPath() {
        InitializeItemRegistry registry = new InitializeItemRegistry();
        
        InitializeItem item = new InitializeItem("foo", 30000.1, null, "/", "/foo", null, null, null, null);
        registry.addInitializeItem(item);
        assertNotNull(registry.getInitializeItemsByPath("/foo", Event.NODE_ADDED));
        assertNull(registry.getInitializeItemsByPath("/bar", Event.NODE_ADDED));
        assertNull(registry.getInitializeItemsByPath("/foobar", Event.NODE_ADDED));
        
        item = new InitializeItem("bar", 30000.1, null, "/", "/bar", null, null, null, null);
        registry.addInitializeItem(item);
        assertEquals(item, registry.getInitializeItemsByPath("/bar", Event.NODE_ADDED).iterator().next());
        
        assertEquals(item, registry.getInitializeItemsByPath("/bar/bar", Event.NODE_ADDED).iterator().next());
        
        item = new InitializeItem("foobar", 30000.1, null, "/foo", "/foo/bar", null, null, null, null);
        registry.addInitializeItem(item);
        assertNotNull(registry.getInitializeItemsByPath("/foo/bar/prop", Event.PROPERTY_ADDED));
        assertEquals(item, registry.getInitializeItemsByPath("/foo/bar/prop", Event.PROPERTY_ADDED).iterator().next());
    }
    
    @Test
    public void testInitializeItemRegistryLookupByNamespace() {
        InitializeItemRegistry registry = new InitializeItemRegistry();
        
        InitializeItem item = new InitializeItem("foo", 30000.3, null, null, null, null, "http://www.foobar.com/1.0", null, null);
        registry.addInitializeItem(item);
        assertNotNull(registry.getInitializeItemByNamespace("http://www.foobar.com/1.0"));
        assertNotNull(registry.getInitializeItemByNamespace("http://www.foobar.com/2.0"));
        assertNull(registry.getInitializeItemByNamespace("http://www.foobar.net/1.0"));
    }
    
    @Test
    public void testInitializeItemRegistryLookupByNamespacePrefix() {
        InitializeItemRegistry registry = new InitializeItemRegistry();
        
        InitializeItem item = new InitializeItem("foo", 30000.2, null, null, null, "foo.cnd", null, null, null);
        registry.addInitializeItem(item);
        assertNotNull(registry.getInitializeItemByNamespacePrefix("foo"));
        
        assertNotNull(registry.getInitializeItemsByPath("/jcr:system/jcr:nodeTypes/foo:doctype", Event.NODE_ADDED));
    }
    
    @Test
    public void testInitializeItemRegistryLookupDescendentItems() {
        InitializeItemRegistry registry = new InitializeItemRegistry();
        
        InitializeItem item = new InitializeItem("foobar", 30000.2, null, "/foo", "/foo/bar", null, null, null, null);
        registry.addInitializeItem(item);
        assertEquals(1, registry.getDescendentInitializeItems("/foo").size());
        
        item = new InitializeItem("foobaz", 30000.2, null, "/foo", "/foo/baz", null, null, null, null);
        registry.addInitializeItem(item);
        assertEquals(2, registry.getDescendentInitializeItems("/foo").size());
    }
    
}
