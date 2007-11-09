/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNodeType;

public class WorkTest extends TestCase
{
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "systemuser";
    private static final char[] SYSTEMUSER_PASSWORD = "systempass".toCharArray();

    private HippoRepository server;

    public void test() throws RepositoryException {
        server = HippoRepositoryFactory.getHippoRepository();
        assertNotNull(server);

        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node, root = session.getRootNode();

        root.setProperty("hippo:property","aap");
        node = root.addNode("foo","hippo:external");
        node.addNode("bar","hippo:virtual").addNode("qux","hippo:virtual").setProperty("hippo:property","mies");
        node.setProperty("hippo:property","noot");

        node = null;
        assertTrue(session.getRootNode().getNode("foo").hasProperty("hippo:property"));
        assertTrue(session.getRootNode().getNode("foo").hasNode("bar"));
        assertTrue(session.getRootNode().getNode("foo").getNode("bar").hasNode("qux"));
        assertTrue(session.getRootNode().getNode("foo").getNode("bar").getNode("qux").hasProperty("hippo:property"));
        assertTrue(session.getRootNode().hasProperty("hippo:property"));
        assertEquals("aap", session.getRootNode().getProperty("hippo:property").getString());
        assertEquals("noot", session.getRootNode().getNode("foo").getProperty("hippo:property").getString());
        assertEquals("mies", session.getRootNode().getNode("foo").getNode("bar").getNode("qux").getProperty("hippo:property").getString());

        session.save();
        session.refresh(false);
        //Utilities.dump(session.getRootNode());
        assertTrue(session.getRootNode().hasNode("foo"));
        assertFalse(session.getRootNode().getNode("foo").hasNode("bar"));
        assertFalse(session.getRootNode().getNode("foo").hasProperty("hippo:property"));
        assertFalse(session.getRootNode().hasProperty("hippo:property"));

        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        assertFalse(session.getRootNode().getNode("foo").hasNode("bar"));
        assertFalse(session.getRootNode().getNode("foo").hasProperty("hippo:property"));
        assertFalse(session.getRootNode().hasProperty("hippo:property"));

        session.logout();
        server.close();
    }
}
