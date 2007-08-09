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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import junit.framework.TestCase;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;
import org.hippocms.repository.jr.servicing.DocumentManager;
import org.hippocms.repository.jr.servicing.Document;
import org.hippocms.repository.jr.servicing.ServicingWorkspace;
import org.hippocms.repository.jr.servicing.WorkflowManager;

import org.apache.jackrabbit.core.XASession;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.RollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.xa.XAResource;
import com.atomikos.icatch.jta.UserTransactionManager;

public class MyWorkflowTest extends TestCase
{
  private HippoRepository server;

  public void setUp() throws RepositoryException, IOException {
    server = HippoRepositoryFactory.getHippoRepository();

    Session session = server.login();
    Node root = session.getRootNode();

    // set up the workflow specification as a node "/configuration/workflows/mycategory/myworkflow"
    Node node = root.getNode("configuration");
    node = node.addNode("workflows");
    node.addMixin("mix:referenceable");
    node = node.addNode("mycategory");
    node = node.addNode("myworkflow");
    node.setProperty("nodetype","hippo:newsArticle");
    node.setProperty("display","My Workflow");
    node.setProperty("renderer","org.hippocms.repository.workflow.MyWorkflowRenderer");
    node.setProperty("service","org.hippocms.repository.workflow.MyWorkflowImpl");
    Node types = node.addNode("types");
    node = types.addNode("org.hippocms.repository.workflow.AuthorDocument");
    node.setProperty("nodetype","hippo:author");
    node.setProperty("display","AuthorDocument");
    node.setProperty("classname","org.hippocms.repository.workflow.AuthorDocument");
    node = types.addNode("org.hippocms.repository.workflow.ArticleDocument");
    node.setProperty("nodetype","hippo:newsArticle");
    node.setProperty("display","ArticleDocument");
    node.setProperty("classname","org.hippocms.repository.workflow.ArticleDocument");

    // set up the queryable document specification as a node "/configuration/documents/authors"
    node = root.getNode("configuration");
    node = node.addNode("documents");
    node.addMixin("mix:referenceable");
    node = node.addNode("authors");
    node.setProperty("query","files//*[@jcr:primaryType='hippo:author' and @hippo:name='?']");
    node.setProperty("language",Query.XPATH);
    node.setProperty("classname","org.hippocms.repository.workflow.AuthorDocument");
    node = node.addNode("types");
    node = node.addNode("org.hippocms.repository.workflow.AuthorDocument");
    node.setProperty("nodetype","hippo:author");
    node.setProperty("display","AuthorDocument");
    node.setProperty("classname","org.hippocms.repository.workflow.AuthorDocument");

    root.addNode("files");

    node = root.getNode("files");
    node = node.addNode("myauthor","hippo:author");
    node.setProperty("hippo:id",666);
    node.setProperty("hippo:name","Jan Smit");

    node = root.getNode("files");
    node = node.addNode("myarticle","hippo:newsArticle");
    node.setProperty("hippo:id",1);
    node.setProperty("hippo:authorId",999);

    session.save();
    session.logout();
  }

  public void tearDown() throws RepositoryException {
    Session session = server.login();
    Node root = session.getRootNode();
    root.getNode("files").remove();
    root.getNode("configuration").remove();
    session.save();
    session.logout();
    server.close();
  }

  public void testAuthorDocument() throws RepositoryException {
    Session session = server.login();
    Node root = session.getRootNode();

    DocumentManager manager = ((ServicingWorkspace)session.getWorkspace()).getDocumentManager();
    Document document = manager.getDocument("authors","Jan Smit");

    assertTrue(document instanceof AuthorDocument);
    AuthorDocument author = (AuthorDocument) document;
    assertTrue(author.authorId == 666);

    session.logout();
  }

  public void testWorkflow() throws RepositoryException, WorkflowException {
    UserTransactionManager utm = new UserTransactionManager();
    utm.setStartupTransactionService(false);
    try {
      utm.init();
      TransactionManager tm = utm;
      tm.begin();

      Session session = server.login();

      Node root = session.getRootNode();

      Node node = root.getNode("files/myarticle");
      assertTrue(node.getProperty("hippo:authorId").getString().equals("999"));

      WorkflowManager manager = ((ServicingWorkspace)session.getWorkspace()).getWorkflowManager();

      //Transaction tx = tm.getTransaction();
      //XAResource sessionXARes = ((XASession) session).getXAResource();
      //tx.enlistResource(sessionXARes);
      try {
        Workflow workflow = manager.getWorkflow("mycategory",node);
        assertNotNull(workflow);
        if(workflow instanceof MyWorkflow) {
          MyWorkflow myworkflow = (MyWorkflow) workflow;
          myworkflow.renameAuthor("Jan Smit");
        } else
          fail("workflow not of proper type "+workflow.getClass().getName());

        //tx.commit();
      } catch (Exception ex) {
        System.err.println(ex.getMessage());
        ex.printStackTrace(System.err);
        // if(tx.getStatus() == Status.STATUS_ACTIVE || tx.getStatus() == Status.STATUS_UNKNOWN) {
        //tx.rollback();
        //}
        // tx.delistResource(sessionXARes, XAResource.TMSUCCESS);
      }

      session.save();
      session.refresh(false);
      assertEquals(node.getProperty("hippo:authorId").getString(), "666");

      session.logout();
    } catch(NotSupportedException ex) {
      System.err.println("NotSupportedException: "+ex.getMessage());
      ex.printStackTrace(System.err);
      //} catch(RollbackException ex) {
      //System.err.println("RollbackException: "+ex.getMessage());
      //ex.printStackTrace(System.err);
    } catch(SystemException ex) {
      System.err.println("SystemException: "+ex.getMessage());
      ex.printStackTrace(System.err);
    }
  }

  public void tstWorkflowGui() throws RepositoryException, WorkflowException,
                                      ClassNotFoundException, NoSuchMethodException, NoSuchMethodException,
                                      InstantiationException, IllegalAccessException, InvocationTargetException {
    Session session = server.login();
    Node root = session.getRootNode();

    Node node = root.getNode("files/myarticle");
    WorkflowManager manager = ((ServicingWorkspace)session.getWorkspace()).getWorkflowManager();
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
      throw ex;
    } catch(NoSuchMethodException ex) {
      throw ex;
    } catch(InstantiationException ex) {
      throw ex;
    } catch(IllegalAccessException ex) {
      throw ex;
    } catch(InvocationTargetException ex) {
      throw ex;
    }
  }
}
