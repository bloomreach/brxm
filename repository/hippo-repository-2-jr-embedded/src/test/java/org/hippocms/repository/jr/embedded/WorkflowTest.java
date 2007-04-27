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
package org.hippocms.repository.jr.embedded;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import junit.framework.TestCase;

import org.hippocms.repository.jr.decoration.WorkspaceDecorator;
import org.hippocms.repository.jr.decoration.NodeDecorator;
import org.hippocms.repository.jr.decoration.Workflow;

/**
 * @version $Id$
 */
public class WorkflowTest extends TestCase {
  private final static String SVN_ID = "$Id$";

  private static  String NODENAME = "documentWithWorkflow";
  private Server server;

  /*
  public void setUp() throws RepositoryException {
    server = new Server("rmi://localhost:1099/jackrabbit.repository");
  }

  public void tearDown() {
    server.close();
  }

  private Session commonStart() throws RepositoryException {
    Session session = server.login();
    Node node, root = session.getRootNode();
    assertNotNull(session);
    node = root.addNode(NODENAME);
    node.setProperty("HasAction1",false);
    node.setProperty("HasAction2",false);
    session.save();
    return session;
  }
  private void commonEnd(Session session) throws RepositoryException {
    session.save();
    Node root = session.getRootNode();
    Node node = root.getNode(NODENAME);
    if(node != null)
      node.remove();
    session.save();
    session.logout();
  }

  public void testGetWorkflow() throws RepositoryException {
    Session session = commonStart();
    Node node = session.getRootNode().getNode(NODENAME);
    assertNotNull(node);
    Workflow workflow = ((NodeDecorator)node).getWorkflow();
    assertNotNull(workflow);
    commonEnd(session);
  }
  public void testBasicWorkflow() throws RepositoryException {
    Session session = commonStart();
    Node node = session.getRootNode().getNode(NODENAME);
    Workflow workflow = ((NodeDecorator)node).getWorkflow();
    assertFalse(node.getProperty("HasAction1").getBoolean());
    try {
      workflow.doAction1();
      session.save();
    } catch(Exception ex) {
      fail();
    }
    assertTrue(node.getProperty("HasAction1").getBoolean());
    commonEnd(session);
  }

  public void testCompoundWorkflow() throws RepositoryException {
    Session session = commonStart();
    Node node = session.getRootNode().getNode(NODENAME);
    Workflow workflow = ((NodeDecorator)node).getWorkflow();
    assertFalse(node.getProperty("HasAction1").getBoolean());
    try {
      workflow.doAction1();
      workflow.doAction2();
      session.save();
    } catch(Exception ex) {
      fail();
    }
    assertTrue(node.getProperty("HasAction1").getBoolean());
    assertTrue(node.getProperty("HasAction2").getBoolean());
    commonEnd(session);
  }

  public void testFailingWorkflow() throws RepositoryException {
    Session session = commonStart();
    Node node = session.getRootNode().getNode(NODENAME);
    Workflow workflow = ((NodeDecorator)node).getWorkflow();
    assertFalse(node.getProperty("HasAction1").getBoolean());
    try {
      workflow.doAction2();
      workflow.doAction1();
      session.save();
      fail("workflow should have failed");
    } catch(Exception ex) {
    }
    assertFalse(node.getProperty("HasAction1").getBoolean());
    assertFalse(node.getProperty("HasAction2").getBoolean());
    commonEnd(session);
  }
  */
}
