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
package org.hippoecm.hst.core.jcr;

import javax.jcr.observation.Event;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * TestEventListenerImpl
 * 
 * @version $Id:
 */
public class TestEventListenerImpl {
    
    
    @Test
    public void testEventListenerImpl() throws Exception {
        EventListenerItemImpl eventListenerItem = new EventListenerItemImpl();

        assertFalse("event listener should not yet listen  to nodes added", Event.NODE_ADDED == (eventListenerItem.getEventTypes() & Event.NODE_ADDED) );
        assertFalse("event listener should not yet listen  to nodes removed", Event.NODE_REMOVED == (eventListenerItem.getEventTypes() & Event.NODE_REMOVED) );
        assertFalse("event listener should not yet  listen  to properties added", Event.PROPERTY_ADDED == (eventListenerItem.getEventTypes() & Event.PROPERTY_ADDED) );
        assertFalse("event listener should not yet  listen  to properties changed", Event.PROPERTY_CHANGED == (eventListenerItem.getEventTypes() & Event.PROPERTY_CHANGED) );
        assertFalse("event listener should not yet  listen  to properties removed", Event.PROPERTY_REMOVED == (eventListenerItem.getEventTypes() & Event.PROPERTY_REMOVED) );
        
        eventListenerItem.setNodeAddedEnabled(true);
        assertTrue("event listener should listen to nodes added", Event.NODE_ADDED == (eventListenerItem.getEventTypes() & Event.NODE_ADDED) );
        assertFalse("event listener should not yet listen  to nodes removed", Event.NODE_REMOVED == (eventListenerItem.getEventTypes() & Event.NODE_REMOVED) );
        assertFalse("event listener should not yet  listen  to properties added", Event.PROPERTY_ADDED == (eventListenerItem.getEventTypes() & Event.PROPERTY_ADDED) );
        assertFalse("event listener should not yet  listen  to properties changed", Event.PROPERTY_CHANGED == (eventListenerItem.getEventTypes() & Event.PROPERTY_CHANGED) );
        assertFalse("event listener should not yet  listen  to properties removed", Event.PROPERTY_REMOVED == (eventListenerItem.getEventTypes() & Event.PROPERTY_REMOVED) );
        
        
        eventListenerItem.setNodeRemovedEnabled(true);
        assertTrue("event listener should listen to nodes added", Event.NODE_ADDED == (eventListenerItem.getEventTypes() & Event.NODE_ADDED) );
        assertTrue("event listener should listen  to nodes removed", Event.NODE_REMOVED == (eventListenerItem.getEventTypes() & Event.NODE_REMOVED) );
        assertFalse("event listener should not yet  listen  to properties added", Event.PROPERTY_ADDED == (eventListenerItem.getEventTypes() & Event.PROPERTY_ADDED) );
        assertFalse("event listener should not yet  listen  to properties changed", Event.PROPERTY_CHANGED == (eventListenerItem.getEventTypes() & Event.PROPERTY_CHANGED) );
        assertFalse("event listener should not yet  listen  to properties removed", Event.PROPERTY_REMOVED == (eventListenerItem.getEventTypes() & Event.PROPERTY_REMOVED) );
        
        
        eventListenerItem.setPropertyAddedEnabled(true);
        assertTrue("event listener should listen to nodes added", Event.NODE_ADDED == (eventListenerItem.getEventTypes() & Event.NODE_ADDED) );
        assertTrue("event listener should listen  to nodes removed", Event.NODE_REMOVED == (eventListenerItem.getEventTypes() & Event.NODE_REMOVED) );
        assertTrue("event listener should listen  to properties added", Event.PROPERTY_ADDED == (eventListenerItem.getEventTypes() & Event.PROPERTY_ADDED) );
        assertFalse("event listener should not yet  listen  to properties changed", Event.PROPERTY_CHANGED == (eventListenerItem.getEventTypes() & Event.PROPERTY_CHANGED) );
        assertFalse("event listener should not yet  listen  to properties removed", Event.PROPERTY_REMOVED == (eventListenerItem.getEventTypes() & Event.PROPERTY_REMOVED) );
        
        eventListenerItem.setPropertyChangedEnabled(true);
        assertTrue("event listener should listen to nodes added", Event.NODE_ADDED == (eventListenerItem.getEventTypes() & Event.NODE_ADDED) );
        assertTrue("event listener should listen  to nodes removed", Event.NODE_REMOVED == (eventListenerItem.getEventTypes() & Event.NODE_REMOVED) );
        assertTrue("event listener should listen  to properties added", Event.PROPERTY_ADDED == (eventListenerItem.getEventTypes() & Event.PROPERTY_ADDED) );
        assertTrue("event listener should listen  to properties changed", Event.PROPERTY_CHANGED == (eventListenerItem.getEventTypes() & Event.PROPERTY_CHANGED) );
        assertFalse("event listener should not yet  listen  to properties removed", Event.PROPERTY_REMOVED == (eventListenerItem.getEventTypes() & Event.PROPERTY_REMOVED) );
        
        eventListenerItem.setPropertyRemovedEnabled(true);
        assertTrue("event listener should listen to nodes added", Event.NODE_ADDED == (eventListenerItem.getEventTypes() & Event.NODE_ADDED) );
        assertTrue("event listener should listen  to nodes removed", Event.NODE_REMOVED == (eventListenerItem.getEventTypes() & Event.NODE_REMOVED) );
        assertTrue("event listener should listen  to properties added", Event.PROPERTY_ADDED == (eventListenerItem.getEventTypes() & Event.PROPERTY_ADDED) );
        assertTrue("event listener should listen  to properties changed", Event.PROPERTY_CHANGED == (eventListenerItem.getEventTypes() & Event.PROPERTY_CHANGED) );
        assertTrue("event listener should listen  to properties removed", Event.PROPERTY_REMOVED == (eventListenerItem.getEventTypes() & Event.PROPERTY_REMOVED) );
        
        
        eventListenerItem.setNodeAddedEnabled(false);
        eventListenerItem.setNodeRemovedEnabled(false);
        eventListenerItem.setPropertyAddedEnabled(false);
        eventListenerItem.setPropertyChangedEnabled(false);
        eventListenerItem.setPropertyRemovedEnabled(false);
        
        assertFalse("event listener should not yet listen  to nodes added", Event.NODE_ADDED == (eventListenerItem.getEventTypes() & Event.NODE_ADDED) );
        assertFalse("event listener should not yet listen  to nodes removed", Event.NODE_REMOVED == (eventListenerItem.getEventTypes() & Event.NODE_REMOVED) );
        assertFalse("event listener should not yet  listen  to properties added", Event.PROPERTY_ADDED == (eventListenerItem.getEventTypes() & Event.PROPERTY_ADDED) );
        assertFalse("event listener should not yet  listen  to properties changed", Event.PROPERTY_CHANGED == (eventListenerItem.getEventTypes() & Event.PROPERTY_CHANGED) );
        assertFalse("event listener should not yet  listen  to properties removed", Event.PROPERTY_REMOVED == (eventListenerItem.getEventTypes() & Event.PROPERTY_REMOVED) );
       
    }
}
