/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PropertyMultiplicityTest extends RepositoryTestCase {

    @Test
    public void change_property_from_single_to_multiple() throws RepositoryException, InterruptedException {

        Session session2 = session.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
        Session session3 = session.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));

        try {
            Node foobar = session.getRootNode().addNode("test").addNode("foobar");
            foobar.setProperty("foo", "bar");

            assertTrue(session.nodeExists("/test/foobar"));
            assertEquals("bar", session.getNode("/test/foobar").getProperty("foo").getString());

            session.save();

            assertTrue(session2.nodeExists("/test/foobar"));
            assertEquals("bar", session2.getNode("/test/foobar").getProperty("foo").getString());

            assertFalse(session.getNode("/test/foobar").getProperty("foo").isMultiple());
            assertFalse(session.getNode("/test/foobar").getProperty("foo").getDefinition().isMultiple());
            assertFalse(session2.getNode("/test/foobar").getProperty("foo").isMultiple());
            assertFalse(session2.getNode("/test/foobar").getProperty("foo").getDefinition().isMultiple());

            Node foobarNode = session.getNode("/test/foobar");
            foobarNode.getProperty("foo").remove();
            foobarNode.setProperty("foo", new String[]{"foo"});

            assertTrue(session.getNode("/test/foobar").getProperty("foo").isMultiple());
            assertTrue(session.getNode("/test/foobar").getProperty("foo").getDefinition().isMultiple());
            assertFalse(session2.getNode("/test/foobar").getProperty("foo").isMultiple());
            assertFalse(session2.getNode("/test/foobar").getProperty("foo").getDefinition().isMultiple());

            session.save();

            assertTrue(session.getNode("/test/foobar").getProperty("foo").isMultiple());
            assertTrue(session.getNode("/test/foobar").getProperty("foo").getDefinition().isMultiple());

            assertTrue(session3.getNode("/test/foobar").getProperty("foo").isMultiple());
            assertTrue(session3.getNode("/test/foobar").getProperty("foo").getDefinition().isMultiple());

            assertTrue(session2.getNode("/test/foobar").getProperty("foo").isMultiple());

            // below fails with unpatched JR, see REPO-1732
            assertTrue(session2.getNode("/test/foobar").getProperty("foo").getDefinition().isMultiple());


        } finally {
            session2.logout();
            session3.logout();
        }
    }


}


