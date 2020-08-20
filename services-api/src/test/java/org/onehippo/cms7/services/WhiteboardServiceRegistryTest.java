/*
 *  Copyright 2018-2020 Bloomreach
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
import org.onehippo.testutils.log4j.Log4jInterceptor;

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

    private static class Counter {
        int counter;

        public void increment() {
            counter++;
        }
    }

    private static class MutableInteger {
        int value;
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

    @Test
    public void testServiceTrackerResilience() {

        final Counter counter = new Counter();
        final MutableInteger currentIteration = new MutableInteger();
        final ServiceTracker<Object> failingTracker = new ServiceTracker<Object>() {
            public void serviceRegistered(final ServiceHolder<Object> serviceHolder) {
                counter.increment();
                throw new RuntimeException("This tracker is failing");
            }
            public void serviceUnregistered(final ServiceHolder<Object> serviceHolder) {
                counter.increment();
                throw new RuntimeException("This tracker is failing");
            }
        };
        final ServiceTracker<Object> tracker = new ServiceTracker<Object>() {
            public void serviceRegistered(final ServiceHolder<Object> serviceHolder) {
                currentIteration.value = counter.counter;
            }
            public void serviceUnregistered(final ServiceHolder<Object> serviceHolder) {
                currentIteration.value = counter.counter;
            }
        };
        // suppress expected exception logging from WhiteboardServiceRegistry
        try (Log4jInterceptor ignored = Log4jInterceptor.onError().deny(WhiteboardServiceRegistry.class).build()) {
            Object serviceObject = new Object();
            WhiteboardServiceRegistry<Object> registry = new WhiteboardServiceRegistry<Object>(){};
            // Always verify that currentIteration and counter have the same value, which means that the failingTracker was executed first, and even if it failed, the other tracker was executed as well.
            registry.register(serviceObject);
            assertEquals(0, counter.counter);
            assertEquals(0, currentIteration.value);
            registry.addTracker(failingTracker);
            registry.addTracker(tracker);
            assertEquals(1, counter.counter);
            assertEquals(1, currentIteration.value);
            registry.unregister(serviceObject);
            assertEquals(2, counter.counter);
            assertEquals(2, currentIteration.value);
            registry.register(serviceObject);
            assertEquals(3, counter.counter);
            assertEquals(3, currentIteration.value);
            registry.removeTracker(tracker);
            registry.unregister(serviceObject);
            // In this case currentIteration doesn't increment, as the tracker was removed
            assertEquals(4, counter.counter);
            assertEquals(3, currentIteration.value);
        }
    }
}
