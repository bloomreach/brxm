/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.observation.Event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class InitializeItemTest {
    
    private static String TEST_HOME;
    
    static {
        // Where are we?
        File basedir = new File(System.getProperty("user.dir"));
        if (basedir.getName().equals("target")) {
            TEST_HOME = basedir.getParent() + "/src/test/resources/initializeitemtest";
        } else {
            TEST_HOME = basedir.getPath() + "/src/test/resources/initializeitemtest";
        }
    }

    @Test
    public void testParseContentResourceDelta() {
        InitializeItem item = new InitializeItem("test", 0.0, "delta.xml", "/", null, null, null, new File(TEST_HOME), null);
        assertEquals("/test", item.getContextPath());
        assertTrue(item.isDelta());
        DeltaInstruction instruction = item.getDelta().getRootInstruction();
        assertNotNull(instruction);
        assertTrue(instruction.isCombineDirective());
        assertEquals("/test", instruction.getContextPath());

        List<DeltaInstruction> nodeInstructions = new ArrayList<>(instruction.getNodeInstructions());
        assertNotNull(nodeInstructions);
        assertEquals(2, nodeInstructions.size());

        Collections.sort(nodeInstructions, new Comparator<DeltaInstruction>() {
            @Override
            public int compare(final DeltaInstruction o1, final DeltaInstruction o2) {
                return o1.getContextPath().compareTo(o2.getContextPath());
            }
        });
        DeltaInstruction child1 = nodeInstructions.get(0);
        assertEquals("/test/bar", child1.getContextPath());
        assertTrue(child1.isCombineDirective());
        assertNull(child1.getNodeInstructions());
        assertNotNull(child1.getPropertyInstructions());
        assertEquals(1, child1.getPropertyInstructions().size());
        DeltaInstruction child2 = nodeInstructions.get(1);
        assertEquals("/test/foo", child2.getContextPath());
        assertTrue(child2.isNoneDirective());
        assertNull(child2.getNodeInstructions());
        assertNull(child2.getPropertyInstructions());

        List<DeltaInstruction> propertyInstructions = new ArrayList<>(instruction.getPropertyInstructions());
        assertNotNull(propertyInstructions);
        assertEquals(2, propertyInstructions.size());

        Collections.sort(propertyInstructions, new Comparator<DeltaInstruction>() {
            @Override
            public int compare(final DeltaInstruction o1, final DeltaInstruction o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        DeltaInstruction child3 = propertyInstructions.get(0);
        assertTrue(child3.isOverrideDirective());
        assertTrue(!child3.isNodeInstruction());
        assertNull(child3.getNodeInstructions());
        assertNull(child3.getPropertyInstructions());
        assertEquals("bar", child3.getName());

        DeltaInstruction child4 = propertyInstructions.get(1);
        assertTrue(child4.isNoneDirective());
        assertTrue(!child4.isNodeInstruction());
        assertEquals("foo", child4.getName());

    }

    @Test
    public void testParseResourceBundlesDelta() {
        InitializeItem item = new InitializeItem("test", 0.0, null, null, null, null, null, "translations.json", new File(TEST_HOME), null);
        assertTrue(item.isDelta());
        DeltaInstruction instruction = item.getDelta().getRootInstruction();
        assertNotNull(instruction);
        assertTrue(instruction.isCombineDirective());
        assertEquals("/hippo:configuration/hippo:translations", instruction.getContextPath());

        Collection<DeltaInstruction> nodeInstructions = instruction.getNodeInstructions();
        assertNotNull(nodeInstructions);
        assertEquals(1, nodeInstructions.size());

        DeltaInstruction fooInstruction = nodeInstructions.iterator().next();
        assertEquals("foo", fooInstruction.getName());
        assertTrue(fooInstruction.isCombineDirective());
        nodeInstructions = fooInstruction.getNodeInstructions();
        assertNotNull(nodeInstructions);
        assertEquals(1, nodeInstructions.size());

        final DeltaInstruction enInstruction = nodeInstructions.iterator().next();
        assertEquals("en", enInstruction.getName());
        assertTrue(enInstruction.isCombineDirective());
        final Collection<DeltaInstruction> propertyInstructions = enInstruction.getPropertyInstructions();
        assertEquals(1, propertyInstructions.size());
        final DeltaInstruction propertyInstruction = propertyInstructions.iterator().next();
        assertEquals("bar", propertyInstruction.getName());
    }

    @Test
    public void testHandleEvent() throws Exception {
        InitializeItem item = new InitializeItem("test", 0.0, "delta.xml", "/", null, null, null, new File(TEST_HOME), null);
        
        item.handleEvent(new ExportEvent(Event.PROPERTY_ADDED, "/test/baz"));
        assertTrue(item.processEvents());
        Collection<DeltaInstruction> propertyInstructions = item.getDelta().getRootInstruction().getPropertyInstructions();
        assertEquals(3, propertyInstructions.size());
        DeltaInstruction instruction = item.getDelta().getRootInstruction().getInstruction("baz", false);
        assertEquals("/test/baz", instruction.getContextPath());
        assertEquals("baz", instruction.getName());
        assertTrue(instruction.isNoneDirective());
        
        item.handleEvent(new ExportEvent(Event.PROPERTY_ADDED, "/test/foo/bar"));
        assertTrue(item.processEvents());
        assertEquals(2, item.getDelta().getRootInstruction().getNodeInstructions().size());
        
        item.handleEvent(new ExportEvent(Event.PROPERTY_ADDED, "/test/bar/quz"));
        assertTrue(item.processEvents());
        instruction = item.getDelta().getRootInstruction().getInstruction("bar", true).getInstruction("quz", false);
        assertNotNull(instruction);
        assertEquals("/test/bar/quz", instruction.getContextPath());
        assertEquals("quz", instruction.getName());
        assertTrue(instruction.isNoneDirective());
        assertTrue(!instruction.isNodeInstruction());
        
        item.handleEvent(new ExportEvent(Event.PROPERTY_REMOVED, "/test/foo"));
        assertTrue(item.processEvents());
        assertEquals(2, item.getDelta().getRootInstruction().getPropertyInstructions().size());
        assertNull(item.getDelta().getRootInstruction().getInstruction("foo", false));
        
        item.handleEvent(new ExportEvent(Event.PROPERTY_REMOVED, "/test/bar/baz"));
        item.handleEvent(new ExportEvent(Event.PROPERTY_REMOVED, "/test/bar/quz"));
        assertTrue(item.processEvents());
        assertEquals(1, item.getDelta().getRootInstruction().getNodeInstructions().size());

    }

}
