/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WhiteboardServiceRegistryTest {

    private static class ValueObject {
        ServiceHolder value;
    }

    @Test
    public void testRegisterUnregisterServiceObject() {
        Object serviceObject = new Object();
        WhiteboardServiceRegistry<Object> registry = new WhiteboardServiceRegistry<Object>(){};
        registry.register(serviceObject);
        try {
            registry.register(serviceObject);
            fail();
        } catch (HippoServiceException e) {
            assertEquals("serviceObject already registered", e.getMessage());
        }
        assertEquals(1, registry.size());
        assertTrue(registry.unregister(serviceObject));
        assertEquals(0, registry.size());
        assertFalse(registry.unregister(serviceObject));
    }

    @Test
    public void testAddRemoveTracker() {
        ServiceTracker<Object> tracker = new ServiceTracker<Object>() {
            public void serviceRegistered(final ServiceHolder<Object> serviceHolder) {}
            public void serviceUnregistered(final ServiceHolder<Object> serviceHolder) {}
        };
        WhiteboardServiceRegistry<Object> registry = new WhiteboardServiceRegistry<Object>(){};
        registry.addTracker(tracker);
        try {
            registry.addTracker(tracker);
            fail();
        } catch (HippoServiceException e) {
            assertEquals("tracker already added", e.getMessage());
        }
        assertTrue(registry.removeTracker(tracker));
        assertFalse(registry.removeTracker(tracker));
    }

    @Test
    public void testServiceObjectTracker() {
        final ValueObject valueObject = new ValueObject();
        final ServiceTracker<Object> tracker = new ServiceTracker<Object>() {
            public void serviceRegistered(final ServiceHolder<Object> serviceHolder) {
                valueObject.value = serviceHolder;
            }
            public void serviceUnregistered(final ServiceHolder<Object> serviceHolder) {
                valueObject.value = null;
            }
        };
        Object serviceObject = new Object();
        WhiteboardServiceRegistry<Object> registry = new WhiteboardServiceRegistry<Object>(){};
        registry.register(serviceObject);
        assertNull(valueObject.value);
        registry.addTracker(tracker);
        assertNotNull(valueObject.value);
        assertSame(valueObject.value.getServiceObject(), serviceObject);
        registry.unregister(serviceObject);
        assertNull(valueObject.value);
        registry.register(serviceObject);
        assertNotNull(valueObject.value);
        assertEquals(valueObject.value.getServiceObject(), serviceObject);
        registry.removeTracker(tracker);
        assertNotNull(valueObject.value);
        registry.unregister(serviceObject);
        assertNotNull(valueObject.value);
        assertEquals(0, registry.size());
    }
}
