/*
 *  Copyright 2012-2020 Bloomreach
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class HippoServiceRegistryTest {

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
    public void testNotNullParameters() {
        try {
            HippoServiceRegistry.register(null, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceObject must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.unregister(null, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceObject must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.register(new TestService(){}, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceInterface must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.unregister(new TestService(){}, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceInterface must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.getService(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceInterface must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.getService(null, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceInterface must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.getService(TestService.class, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("extraInterface must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.addTracker(null, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("tracker must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.removeTracker(null, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("tracker must not be null", e.getMessage());
        }
        final ProxiedServiceTracker<TestService> tracker = new ProxiedServiceTracker<TestService>() {
            public void serviceRegistered(final ProxiedServiceHolder<TestService> serviceHolder) {
            }
            public void serviceUnregistered(final ProxiedServiceHolder<TestService> serviceHolder) {
            }
        };
        try {
            HippoServiceRegistry.addTracker(tracker, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceInterface must not be null", e.getMessage());
        }
        try {
            HippoServiceRegistry.removeTracker(tracker, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceInterface must not be null", e.getMessage());
        }
    }

    @Test
    public void serviceIsRegistered() {
        TestService testService = new TestService(){};
        HippoServiceRegistry.register(testService, TestService.class);
        try {
            TestService service = HippoServiceRegistry.getService(TestService.class);
            assertNotNull(service);
            assertFalse(service instanceof AnotherInterface);
        } finally {
            HippoServiceRegistry.unregister(testService, TestService.class);
        }
    }

    @Test
    public void serviceIsAlreadyRegistered() {
        TestService testService = new TestService(){};
        HippoServiceRegistry.register(testService, TestService.class);
        try {
            HippoServiceRegistry.register(new TestService(){}, TestService.class);
            fail();
        } catch (HippoServiceException e) {
            assertEquals("A service of type "+TestService.class.getName()+" is already registered.", e.getMessage());
        } finally {
            HippoServiceRegistry.unregister(testService, TestService.class);
        }
    }

    @Test
    public void serviceIsRegisteredImplementingAnotherInterface() {
        TestService testService = new AnotherInterface(){};
        HippoServiceRegistry.register(testService, TestService.class);
        try {
            TestService service = HippoServiceRegistry.getService(TestService.class);
            assertNotNull(service);
            assertFalse(service instanceof AnotherInterface);
        } finally {
            HippoServiceRegistry.unregister(testService, TestService.class);
        }
    }

    @Test
    public void serviceIsRegisteredWithAnotherInterface() {
        TestService testService = new AnotherInterface(){};
        HippoServiceRegistry.register(testService, TestService.class, AnotherInterface.class);
        try {
            TestService service = HippoServiceRegistry.getService(TestService.class);
            assertNotNull(service);
            assertTrue(service instanceof AnotherInterface);
            AnotherInterface castedService = HippoServiceRegistry.getService(TestService.class, AnotherInterface.class);
            assertNotNull(castedService);
        } finally {
            HippoServiceRegistry.unregister(testService, TestService.class);
        }
    }

    @Test
    public void testAddRemoveTracker() {
        ProxiedServiceTracker<TestService> tracker = new ProxiedServiceTracker<TestService>() {
            public void serviceRegistered(final ProxiedServiceHolder<TestService> serviceHolder) {}
            public void serviceUnregistered(final ProxiedServiceHolder<TestService> serviceHolder) {}
        };
        try {
            HippoServiceRegistry.addTracker(tracker, TestService.class);
            try {
                HippoServiceRegistry.addTracker(tracker, TestService.class);
                fail();
            } catch (HippoServiceException e) {
                assertEquals("tracker already added for service interface "+TestService.class.getName(), e.getMessage());
            }
            assertTrue(HippoServiceRegistry.removeTracker(tracker, TestService.class));
            assertFalse(HippoServiceRegistry.removeTracker(tracker, TestService.class));
        } finally {
            HippoServiceRegistry.removeTracker(tracker, TestService.class);
        }
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
        try {
            HippoServiceRegistry.register(testService, TestService.class);
            assertNull(valueObject.value);
            HippoServiceRegistry.addTracker(tracker, TestService.class);
            assertNotNull(valueObject.value);
            assertEquals(TestService.class, valueObject.value.getServiceInterface());
            HippoServiceRegistry.unregister(testService, TestService.class);
            assertNull(valueObject.value);
            HippoServiceRegistry.register(testService, TestService.class);
            assertNotNull(valueObject.value);
            assertEquals(TestService.class, valueObject.value.getServiceInterface());
            HippoServiceRegistry.removeTracker(tracker, TestService.class);
            assertNotNull(valueObject.value);
            HippoServiceRegistry.unregister(testService, TestService.class);
            assertNotNull(valueObject.value);
        } finally {
            HippoServiceRegistry.removeTracker(tracker, TestService.class);
            HippoServiceRegistry.unregister(testService, TestService.class);
        }
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
        TestService testService = new TestService(){};
        try {
            // Always verify that currentIteration and counter have the same value, which means that the failingTracker was executed first, and even if it failed, the other tracker was executed as well.
            HippoServiceRegistry.register(testService, TestService.class);
            assertEquals(0, counter.counter);
            assertEquals(0, currentIteration.value);
            HippoServiceRegistry.addTracker(failingTracker, TestService.class);
            HippoServiceRegistry.addTracker(tracker, TestService.class);
            assertEquals(1, counter.counter);
            assertEquals(1, currentIteration.value);
            HippoServiceRegistry.unregister(testService, TestService.class);
            assertEquals(2, counter.counter);
            assertEquals(2, currentIteration.value);
            HippoServiceRegistry.register(testService, TestService.class);
            assertEquals(3, counter.counter);
            assertEquals(3, currentIteration.value);
            HippoServiceRegistry.removeTracker(tracker, TestService.class);
            HippoServiceRegistry.unregister(testService, TestService.class);
            // In this case currentIteration doesn't increment, as the tracker was removed
            assertEquals(4, counter.counter);
            assertEquals(3, currentIteration.value);
        } finally {
            HippoServiceRegistry.removeTracker(tracker, TestService.class);
            HippoServiceRegistry.removeTracker(failingTracker, TestService.class);
            HippoServiceRegistry.unregister(testService, TestService.class);
        }
    }
}
