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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.servicing.ServicingNodeImpl;

import org.apache.jackrabbit.core.NodeImpl;

public class WorkTest extends TestCase
{
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "systemuser";
    private static final char[] SYSTEMUSER_PASSWORD = "systempass".toCharArray();

    private HippoRepository server;

    private void checkSessionData(Session session) throws RepositoryException {
        assertFalse(session.getRootNode().hasProperty("hippo:property"));
        assertTrue(session.getRootNode().hasNode("hippo"));
        assertFalse(session.getRootNode().getNode("hippo").hasNode("testing"));
        assertTrue(session.getRootNode().getNode("hippo").hasProperty("hippo:property"));
        assertTrue(session.getRootNode().getNode("hippo").hasNode("test"));
        assertTrue(session.getRootNode().getNode("hippo").getNode("test").hasProperty("hippo:property"));
        assertTrue(session.getRootNode().getNode("hippo").getProperty("hippo:property").getLong() > 1);
        assertTrue(session.getRootNode().getNode("hippo").getNode("test").getProperty("hippo:property").getLong() > 1);

        assertTrue(session.getRootNode().getNode("hippo").getNode("test").hasNode("oosteinde"));
        assertTrue(session.getRootNode().getNode("hippo").getNode("test").getNode("oosteinde").hasProperty("ape"));
        assertTrue(session.getRootNode().getNode("hippo").getNode("test").getNode("oosteinde").hasProperty("leesplank"));
        assertEquals("nootjes", session.getRootNode().getNode("hippo").getNode("test").getNode("oosteinde").getProperty("ape").getString());
        Property p = session.getRootNode().getNode("hippo").getNode("test").getNode("oosteinde").getProperty("leesplank");
        assertTrue(p.getDefinition().isMultiple());
        Value[] values = p.getValues();
        assertEquals(3, values.length);
        assertEquals("mies", values[0].getString());
        assertEquals("vuur", values[1].getString());
        assertEquals("gijs", values[2].getString());
    }

    public void test() throws RepositoryException {
        server = HippoRepositoryFactory.getHippoRepository();
        assertNotNull(server);

        Session alternate, session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node node, root = session.getRootNode();

        node = root.addNode("oosteinde");
        node.setProperty("ape","nootjes");
        node.setProperty("leesplank",new String[] { "mies", "vuur", "gijs" });
        session.save();

        HippoLocalItemStateManager.workaround(((NodeImpl)(((ServicingNodeImpl)(session.getRootNode().getNode("oosteinde"))).unwrap())).getNodeId());

        root.setProperty("hippo:property",0);
        node = root.addNode("hippo","hippo:external");
        node.addNode("testing","hippo:virtual").setProperty("hippo:property",1);

        node = null;
        assertTrue(session.getRootNode().hasProperty("hippo:property"));
        assertTrue(session.getRootNode().hasNode("hippo"));
        assertTrue(session.getRootNode().getNode("hippo").hasNode("testing"));
        assertTrue(session.getRootNode().getNode("hippo").getNode("testing").hasProperty("hippo:property"));
        assertFalse(session.getRootNode().hasNode("test"));
        assertEquals(0, session.getRootNode().getProperty("hippo:property").getLong());
        assertEquals(1, session.getRootNode().getNode("hippo").getNode("testing").getProperty("hippo:property").getLong());

        session.save();
        session.refresh(false);
        checkSessionData(session);

        session.logout();

        alternate = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        checkSessionData(alternate);
        alternate.logout();

        server.close();
    }
}
