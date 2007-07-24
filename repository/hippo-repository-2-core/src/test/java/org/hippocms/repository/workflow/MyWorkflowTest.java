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
package org.hippocms.repository.workflow;

import java.io.File;
import java.io.IOException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import junit.framework.TestCase;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;
import org.hippocms.repository.jr.servicing.DocumentManager;
import org.hippocms.repository.jr.servicing.WorkflowManager;
import org.hippocms.repository.workflow.WorkflowDescriptor;
import org.hippocms.repository.jr.servicing.ServicingSession;
import org.hippocms.repository.jr.servicing.ServicingWorkspace;

public class MyWorkflowTest extends TestCase
{
  private HippoRepository server;

  public void setUp() throws RepositoryException, IOException {
    server = HippoRepositoryFactory.getHippoRepository();

    Session session = server.login();
    Node root = session.getRootNode();

    // set up the workflow specification
    Node node = root.addNode("configuration");
    node = node.addNode("workflows");
    node = node.addNode("mycategory");
    node = node.addNode("myworkflow");
    node.setProperty("nodetype","hippo:newsArticle");
    node.setProperty("display","My Workflow");
    node.setProperty("renderer","org.hippocms.repository.workflow.MyWorkflowRenderer");
    node.setProperty("service","org.hippocms.repository.workflow.MyWorkflowImpl");

    root.addNode("files");

    node = root.getNode("files");
    node = node.addNode("myauthor","hippo:author");
    node.setProperty("hippo:id",666);
    node.setProperty("hippo:name","Jan Smit");

    node = root.getNode("files");
    node = node.addNode("myarticle","hippo:newsArticle");
    node.setProperty("hippo:authorId",999);

    session.save();
    session.logout();
  }

  public void tearDown() {
    server.close();
  }

  public void testDocument() throws RepositoryException {
    Session session = server.login();
    Node root = session.getRootNode();

    Node node = root.getNode("files/myarticle");
    DocumentManager manager = ((ServicingWorkspace)session.getWorkspace()).getDocumentManager();

    session.logout();
  }

  public void tstWorkflow() throws RepositoryException {
    Session session = server.login();
    Node root = session.getRootNode();

    // actual test
    Node node = root.getNode("files/myarticle");
    WorkflowManager manager = ((ServicingWorkspace)session.getWorkspace()).getWorkflowManager();
    Workflow workflow = manager.getWorkflow("mycategory",node);
    if(workflow instanceof MyWorkflow) {
      MyWorkflow myworkflow = (MyWorkflow) workflow;
      try {
        myworkflow.renameAuthor("Jan Smit");
      } catch(WorkflowException ex) {
      }
    }

    // a gui example
    node = root.getNode("files/myarticle");
    manager = ((ServicingWorkspace)session.getWorkspace()).getWorkflowManager();
    WorkflowDescriptor descriptor = manager.getWorkflowDescriptor("mycategory",node);

    try {
      Class rendererClass = Class.forName(descriptor.getRendererName());
      Object[] args = new Object[2];
      args[0] = manager;
      args[1] = descriptor;
      Constructor constructor = rendererClass.getConstructor(args[0].getClass(), args[1].getClass());
      GenericWorkflowRenderer renderer = (GenericWorkflowRenderer) constructor.newInstance(args);
      try {
        renderer.invoke();
      } catch(WorkflowException ex) {
      }
    } catch(ClassNotFoundException ex) {
    } catch(NoSuchMethodException ex) {
    } catch(InstantiationException ex) {
    } catch(IllegalAccessException ex) {
    } catch(InvocationTargetException ex) {
    }

    session.logout();
  }
}
