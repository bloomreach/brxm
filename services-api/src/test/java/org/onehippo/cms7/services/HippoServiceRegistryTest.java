/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
public class HippoServiceRegistryTest {

    @SingletonService
    interface TestService {

        void doSomething();
    }

    interface AnotherInterface extends TestService {

    }

    @Test
    public void serviceIsRegistered() {
        TestService testService = new TestService() {
            @Override
            public void doSomething() {
            }
        };
        HippoServiceRegistry.registerService(testService, TestService.class);
        try {
            TestService service = HippoServiceRegistry.getService(TestService.class);
            assertNotNull(service);
            assertFalse(service instanceof AnotherInterface);
        } finally {
            HippoServiceRegistry.unregisterService(testService, TestService.class);
        }
    }

    @Test
    public void serviceIsRegisteredImplementingAnotherInterface() {
        TestService testService = new AnotherInterface() {
            @Override
            public void doSomething() {
            }
        };
        HippoServiceRegistry.registerService(testService, TestService.class);
        try {
            TestService service = HippoServiceRegistry.getService(TestService.class);
            assertNotNull(service);
            assertFalse(service instanceof AnotherInterface);
        } finally {
            HippoServiceRegistry.unregisterService(testService, TestService.class);
        }
    }

    @Test
    public void serviceIsRegisteredWithAdditionalInterface() {
        TestService testService = new AnotherInterface() {
            @Override
            public void doSomething() {
            }
        };
        HippoServiceRegistry.registerService(testService, new Class[]{TestService.class, AnotherInterface.class});
        try {
            TestService service = HippoServiceRegistry.getService(TestService.class);
            assertNotNull(service);
            assertTrue(service instanceof AnotherInterface);
        } finally {
            HippoServiceRegistry.unregisterService(testService, TestService.class);
        }
    }


    @Test(expected = HippoServiceException.class)
    public void fail_to_register_unnamed_services_if_singleton_is_missing_and_prove_singleton_not_inherited_from_super() {
        TestService testService = new AnotherInterface() {
            @Override
            public void doSomething() {
            }
        };
        HippoServiceRegistry.registerService(testService, new Class[]{AnotherInterface.class});
    }

    interface NamedService {
        String getName();
    }

    @Test
    public void register_multiple_named_services_with_same_iface_different_name() {
        NamedService service1 = () -> "foo";
        NamedService service2 = () -> "bar";

        HippoServiceRegistry.registerService(service1, new Class[]{NamedService.class}, "foo");
        HippoServiceRegistry.registerService(service2, new Class[]{NamedService.class}, "bar");

        assertNull("Service is registered with name 'foo' or 'bar'", HippoServiceRegistry.getService(NamedService.class));
        NamedService foo = HippoServiceRegistry.getService(NamedService.class, "foo");
        assertEquals("foo", foo.getName());
        NamedService bar = HippoServiceRegistry.getService(NamedService.class, "bar");
        assertEquals("bar", bar.getName());

        List<NamedService> services = HippoServiceRegistry.getServices(NamedService.class);
        assertEquals(2, services.size());


        HippoServiceRegistry.unregisterService(service1, NamedService.class, "foo");

        List<NamedService> servicesAgain = HippoServiceRegistry.getServices(NamedService.class);
        assertEquals(1, servicesAgain.size());
        assertEquals("bar", servicesAgain.get(0).getName());

        HippoServiceRegistry.unregisterService(service2, NamedService.class, "bar");
        assertEquals(0, HippoServiceRegistry.getServices(NamedService.class).size());
    }
}
