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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProxiedServiceHolderTest {

    private interface SomeService {
    }

    private interface AnotherService {
    }

    private static class DoubleService implements SomeService, AnotherService {
    }

    @Test
    public void testNotNullParameters() {
        try {
            new ProxiedServiceHolder<>(null, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceObject must not be null", e.getMessage());
        }
        try {
            new ProxiedServiceHolder<>(new SomeService(){}, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("serviceInterface must not be null", e.getMessage());
        }
        try {
            new ProxiedServiceHolder<>(new SomeService(){}, SomeService.class, SomeService.class, null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("extraInterfaces must not be null", e.getMessage());
        }
    }

    @Test
    public void testNotImplementedExtraInterface() {
        try {
            new ProxiedServiceHolder<>(new SomeService(){}, SomeService.class, AnotherService.class);
            fail();
        } catch (HippoServiceException e) {
            assertEquals("Service object not implementing interface " + AnotherService.class.getName(), e.getMessage());
        }
    }

    @Test
    public void testIgnoredDuplicateExtraInterface() {
        new ProxiedServiceHolder<>(new SomeService(){}, SomeService.class, SomeService.class);
    }

    @Test
    public void testGetService() {
        ProxiedServiceHolder<SomeService> serviceHolder =
                new ProxiedServiceHolder<>(new SomeService(){}, SomeService.class);
        assertEquals(SomeService.class, serviceHolder.getServiceInterface());
        assertNotNull(serviceHolder.getServiceProxy());
        assertTrue(SomeService.class.isInstance(serviceHolder.getServiceProxy()));
        assertNotSame(serviceHolder.getServiceProxy(), serviceHolder.getServiceObject());
    }

    @Test
    public void testExtraInterfaces() {
        SomeService proxy = new ProxiedServiceHolder<>(new DoubleService(), SomeService.class).getServiceProxy();
        assertFalse(AnotherService.class.isInstance(proxy));
        ProxiedServiceHolder<SomeService> serviceHolder =
                new ProxiedServiceHolder<>(new DoubleService(), SomeService.class, AnotherService.class);
        assertTrue(AnotherService.class.isInstance(serviceHolder.getServiceProxy()));
        assertEquals(1, serviceHolder.getExtraInterfaces().count());
        assertEquals(AnotherService.class, serviceHolder.getExtraInterfaces().iterator().next());
    }
}
