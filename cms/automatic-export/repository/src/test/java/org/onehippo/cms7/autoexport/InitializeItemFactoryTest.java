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
package org.onehippo.cms7.autoexport;

import java.io.File;

import javax.jcr.observation.Event;

import static org.junit.Assert.*;

import org.junit.Test;

public class InitializeItemFactoryTest {

    @Test
    public void testCreateInitializeItemByPath() {
        InitializeItemRegistry registry = new InitializeItemRegistry();
        InitializeItemFactory factory = new InitializeItemFactory(null, registry, null);
        
        InitializeItem item = factory.createInitializeItem("/jcr:system/jcr:nodeTypes/foo:doctype", Event.NODE_ADDED);
        assertNotNull(item);
        assertEquals("foo-nodetypes", item.getName());
        assertEquals(new Double(30000.1), item.getSequence());
        assertEquals("namespaces/foo.cnd", item.getNodeTypesResource());
        
        item = factory.createInitializeItem("/foo", Event.NODE_ADDED);
        assertNotNull(item);
        assertEquals("foo", item.getName());
        assertEquals("foo.xml", item.getContentResource());
        assertEquals("/", item.getContentRoot());
        assertEquals(new Double(30000.3), item.getSequence());
        
        item = factory.createInitializeItem("/foo/bar", Event.NODE_ADDED);
        assertNotNull(item);
        assertEquals("foo", item.getName());
        
        item.setContextPath("/foo");
        registry.addInitializeItem(item);
        
        item = factory.createInitializeItem("/foo/bar/baz", Event.NODE_ADDED);
        assertNotNull(item);
        assertEquals("foo-bar-baz", item.getName());
        assertEquals("foo/bar/baz.xml", item.getContentResource());
        assertEquals("/foo/bar", item.getContentRoot());
        assertEquals(new Double(30001.3), item.getSequence());
    }
    
    @Test
    public void testCreatInitializeItemByNamespace() {
        InitializeItemFactory factory = new InitializeItemFactory(null, null, null);
        InitializeItem item = factory.createInitializeItem("http://www.foo.com/1.0", "foo_1_0");
        assertNotNull(item);
        assertEquals("foo", item.getName());
        assertEquals("http://www.foo.com/1.0", item.getNamespace());
        assertEquals(new Double(30000.0), item.getSequence());
    }

}
