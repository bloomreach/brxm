/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WhiteboardProxiedServiceRegistryTest {

    private interface TestService {
    }

    private interface AnotherInterface extends TestService {
    }

    private static class ValueObject {
        ProxiedServiceHolder value;
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
    public void testCommonTypeIsAnInterface() {
        try {
            new WhiteboardProxiedServiceRegistry<Object>(Object.class){};
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("serviceInterface "+Object.class.getName()+" is not an interface", e.getMessage());
        }
    }

    @Test
    public void testRegisterUnregisterServiceObject() {
        TestService serviceObject = new TestService(){};
        WhiteboardProxiedServiceRegistry<TestService> registry = new WhiteboardProxiedServiceRegistry<TestService>(TestService.class){};
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
    public void serviceIsRegisteredWithAnotherInterface() {
        TestService testService = new AnotherInterface(){};
        WhiteboardProxiedServiceRegistry<TestService> registry = new WhiteboardProxiedServiceRegistry<TestService>(TestService.class){};
        registry.register(testService, AnotherInterface.class);
        assertEquals(1, registry.size());
        TestService service = registry.getServices().iterator().next();
        assertNotNull(service);
        assertTrue(service instanceof AnotherInterface);
    }

    @Test
    public void testAddRemoveTracker() {
        ProxiedServiceTracker<TestService> tracker = new ProxiedServiceTracker<TestService>() {
            public void serviceRegistered(final ProxiedServiceHolder<TestService> serviceHolder) {}
            public void serviceUnregistered(final ProxiedServiceHolder<TestService> serviceHolder) {}
        };
        WhiteboardProxiedServiceRegistry<TestService> registry = new WhiteboardProxiedServiceRegistry<TestService>(TestService.class){};
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
    public void testServiceTracker() {
        final ValueObject valueObject = new ValueObject();
        final ProxiedServiceTracker<TestService> tracker = new ProxiedServiceTracker<TestService>() {
            public void serviceRegistered(final ProxiedServiceHolder<TestService> serviceHolder) {
                valueObject.value = serviceHolder;
            }
            public void serviceUnregistered(final ProxiedServiceHolder<TestService> serviceHolder) {
                valueObject.value = null;
            }
        };
        TestService testService = new TestService(){};
        WhiteboardProxiedServiceRegistry<TestService> registry = new WhiteboardProxiedServiceRegistry<TestService>(TestService.class){};
        registry.register(testService);
        assertNull(valueObject.value);
        registry.addTracker(tracker);
        assertNotNull(valueObject.value);
        assertEquals(TestService.class, valueObject.value.getServiceInterface());
        registry.unregister(testService);
        assertNull(valueObject.value);
        registry.register(testService);
        assertNotNull(valueObject.value);
        assertEquals(TestService.class, valueObject.value.getServiceInterface());
        registry.removeTracker(tracker);
        assertNotNull(valueObject.value);
        registry.unregister(testService);
        assertNotNull(valueObject.value);
        assertEquals(0, registry.size());
    }

    @Test
    public void testServiceTrackerResilience() {

        final Counter counter = new Counter();
        final MutableInteger currentIteration = new MutableInteger();
        final ProxiedServiceTracker<TestService> failingTracker = new ProxiedServiceTracker<TestService>() {
            public void serviceRegistered(final ProxiedServiceHolder<TestService> serviceHolder) {
                counter.increment();
                throw new RuntimeException("This tracker is failing");
            }
            public void serviceUnregistered(final ProxiedServiceHolder<TestService> serviceHolder) {
                counter.increment();
                throw new RuntimeException("This tracker is failing");
            }
        };
        final ProxiedServiceTracker<TestService> tracker = new ProxiedServiceTracker<TestService>() {
            public void serviceRegistered(final ProxiedServiceHolder<TestService> serviceHolder) {
                currentIteration.value = counter.counter;
            }
            public void serviceUnregistered(final ProxiedServiceHolder<TestService> serviceHolder) {
                currentIteration.value = counter.counter;
            }
        };
        // suppress expected exception logging from WhiteboardProxiedServiceRegistry
        try (Log4jInterceptor ignored = Log4jInterceptor.onError().deny(WhiteboardProxiedServiceRegistry.class).build()) {
            TestService testService = new TestService(){};
            WhiteboardProxiedServiceRegistry<TestService> registry = new WhiteboardProxiedServiceRegistry<TestService>(TestService.class){};
            // Always verify that currentIteration and counter have the same value, which means that the failingTracker was executed first, and even if it failed, the other tracker was executed as well.
            registry.register(testService, TestService.class);
            assertEquals(0, counter.counter);
            assertEquals(0, currentIteration.value);
            registry.addTracker(failingTracker);
            registry.addTracker(tracker);
            assertEquals(1, counter.counter);
            assertEquals(1, currentIteration.value);
            registry.unregister(testService);
            assertEquals(2, counter.counter);
            assertEquals(2, currentIteration.value);
            registry.register(testService, TestService.class);
            assertEquals(3, counter.counter);
            assertEquals(3, currentIteration.value);
            registry.removeTracker(tracker);
            registry.unregister(testService);
            // In this case currentIteration doesn't increment, as the tracker was removed
            assertEquals(4, counter.counter);
            assertEquals(3, currentIteration.value);
        }
    }
}
