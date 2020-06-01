/**
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock.observation;

import static org.junit.Assert.assertEquals;

import javax.jcr.observation.Event;

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.mock.MockSession;

/**
 * Test for {@link MockEvent}.
 */
public class MockEventTest {

    @Test
    public void testBasic() throws Exception {
        MockNode root = MockNode.root();
        MockSession session = (MockSession) root.getSession();

        int type = Event.NODE_ADDED;
        String path = "/foo/bar";
        String identifier = "id1";
        String userData = "userdata1";
        long timestamp = System.currentTimeMillis();

        MockEvent event = new MockEvent(session, type, path, identifier, userData, timestamp);
        event.getInfo().put("hello", "world");

        assertEquals(type, event.getType());
        assertEquals(session.getUserID(), event.getUserID());
        assertEquals(path, event.getPath());
        assertEquals(identifier, event.getIdentifier());
        assertEquals(userData, event.getUserData());
        assertEquals(timestamp, event.getDate());
        assertEquals("world", event.getInfo().get("hello"));
    }

}
