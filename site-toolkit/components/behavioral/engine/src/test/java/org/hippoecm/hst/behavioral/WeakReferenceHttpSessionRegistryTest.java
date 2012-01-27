/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.behavioral;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.hippoecm.hst.mock.util.IteratorEnumeration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link WeakReferenceHttpSessionRegistry}.
 */
public class WeakReferenceHttpSessionRegistryTest {

    private static WeakReferenceHttpSessionRegistry registry;

    @BeforeClass
    public static void init() {
        registry = new WeakReferenceHttpSessionRegistry();
    }
    
    @Before
    public void setUp() {
        registry.clear();
        assertTrue(registry.isEmpty());
    }

    @Test
    public void emptyRegistryWorkCorrectly() {
        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());

        Collection<HttpSession> c = registry.getHttpSessions();
        assertTrue(c.isEmpty());

        assertNull(registry.getHttpSession("session"));
    }

    @Test
    public void sessionIsRegistered() {
        DummyHttpSession session = new DummyHttpSession("session");
        registry.add(session);
        assertEquals(1, registry.size());
        assertEquals(session, registry.getHttpSession("session"));
        
        Collection<HttpSession> sessions = registry.getHttpSessions();
        assertEquals(1, sessions.size());
        assertEquals(session, sessions.iterator().next());
    }

    @Test
    public void sessionIsOnlyRegisteredOnce() {
        DummyHttpSession session = new DummyHttpSession("session");
        registry.add(session);
        registry.add(session);
        assertEquals(1, registry.size());
        assertEquals(session, registry.getHttpSession("session"));
    }

    @Test
    public void registeredSessionIsRemovedWhenWeaklyReachable() {
        DummyHttpSession session = new DummyHttpSession("session");
        registry.add(session);
        assertEquals(1, registry.size());

        // force memory cleanup
        session = null;
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        registry.processReferenceQueue();

        assertTrue(registry.isEmpty());
    }

    @Test
    public void sessionIsDeregistered() {
        assertNull(registry.getHttpSession("session"));

        DummyHttpSession session = new DummyHttpSession("session");
        registry.add(session);
        assertEquals(1, registry.size());
        
        registry.remove(session);
        assertTrue(registry.isEmpty());
    }

    private class DummyHttpSession implements HttpSession {

        private final String id;
        private final Map<String, Object> attributes;
        
        DummyHttpSession(String id) {
            this.id = id;
            attributes = new HashMap<String, Object>();
        }

        @Override
        public long getCreationTime() {
            return 0;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public long getLastAccessedTime() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public void setMaxInactiveInterval(final int interval) {
        }

        @Override
        public int getMaxInactiveInterval() {
            return 0;
        }

        @Override
        @SuppressWarnings("deprecation")
        public HttpSessionContext getSessionContext() {
            return null;
        }

        @Override
        public Object getAttribute(final String name) {
            return attributes.get(name);
        }

        @Override
        public Object getValue(final String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration getAttributeNames() {
            return new IteratorEnumeration<String>(attributes.keySet().iterator());
        }

        @Override
        public String[] getValueNames() {
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void setAttribute(final String name, final Object value) {
            attributes.put(name, value);
        }

        @Override
        public void putValue(final String name, final Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAttribute(final String name) {
            attributes.remove(name);
        }

        @Override
        public void removeValue(final String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void invalidate() {
            // do nothing
        }

        @Override
        public boolean isNew() {
            return true;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o instanceof DummyHttpSession) {
                DummyHttpSession other = (DummyHttpSession)o;
                return id.equals(other.id);
            } else {
                return false;
            }
        }
        
        @Override
        public int hashCode() {
            return id.hashCode();
        }
        
    }
    
}
