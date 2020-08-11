/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.easymock.EasyMock;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestRequestContextProvider {

    public static void setCurrentRequestContext(HstRequestContext hrc) {
        new RequestContextProvider.ModifiableRequestContextProvider(){}.set(hrc);
    }

    @Test
    public void testLifecycle() throws Exception {
        HstRequestContext hrc = EasyMock.createNiceMock(HstRequestContext.class);
        EasyMock.replay(hrc);

        assertNull(RequestContextProvider.get());

        setCurrentRequestContext(hrc);

        try {
            assertTrue(hrc == RequestContextProvider.get());
        } finally {
            new RequestContextProvider.ModifiableRequestContextProvider(){}.clear();
            // should be okay with multiple clearances.
            new RequestContextProvider.ModifiableRequestContextProvider(){}.clear();
        }

        assertNull(RequestContextProvider.get());
    }

    @Test
    public void testRequestContextProviderVisibilities() throws Exception {
        assertEquals(Modifier.PUBLIC, Modifier.PUBLIC & RequestContextProvider.class.getModifiers());
        assertEquals(Modifier.FINAL, Modifier.FINAL & RequestContextProvider.class.getModifiers());
        
        Constructor<?> constructor = RequestContextProvider.class.getDeclaredConstructor(null);
        assertEquals(Modifier.PRIVATE, Modifier.PRIVATE & constructor.getModifiers());
        
        Method getter = RequestContextProvider.class.getDeclaredMethod("get", null);
        assertEquals(Modifier.STATIC, Modifier.STATIC & getter.getModifiers());
        assertEquals(Modifier.PUBLIC, Modifier.PUBLIC & getter.getModifiers());

        Method setter = RequestContextProvider.class.getDeclaredMethod("set", HstRequestContext.class);
        assertEquals(Modifier.STATIC, Modifier.STATIC & setter.getModifiers());
        assertFalse(Modifier.PUBLIC == (Modifier.PUBLIC & setter.getModifiers()));

        Method clearer = RequestContextProvider.class.getDeclaredMethod("clear", null);
        assertEquals(Modifier.STATIC, Modifier.STATIC & clearer.getModifiers());
        assertFalse(Modifier.PUBLIC == (Modifier.PUBLIC & clearer.getModifiers()));
    }
}
