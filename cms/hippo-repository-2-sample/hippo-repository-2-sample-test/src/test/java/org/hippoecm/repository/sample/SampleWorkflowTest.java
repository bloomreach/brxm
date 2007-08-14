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
package org.hippoecm.repository.sample;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.transaction.SystemException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.RollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.xa.XAResource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.XASession;

import com.atomikos.icatch.jta.UserTransactionManager;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;
import org.hippocms.repository.jr.servicing.DocumentManager;
import org.hippocms.repository.jr.servicing.Document;
import org.hippocms.repository.jr.servicing.ServicingWorkspace;
import org.hippocms.repository.jr.servicing.WorkflowManager;

import org.hippocms.repository.workflow.Workflow;
import org.hippocms.repository.workflow.WorkflowDescriptor;
import org.hippocms.repository.workflow.WorkflowException;

public class SampleWorkflowTest extends TestCase
{
  private HippoRepository server;

  public void setUp() throws Exception {
    server = HippoRepositoryFactory.getHippoRepository();
  }

  public void tearDown() throws Exception {
    server.close();
  }

  public void testWorkflow() throws RepositoryException, WorkflowException, IOException, Exception {
    SampleWorkflowSetup.commonStart(server);
    try {
      //UserTransactionManager utm = new UserTransactionManager();
      //utm.setStartupTransactionService(false);

      //utm.init();
      //TransactionManager tm = utm;
      //tm.begin();

      Session session = server.login();

      Node root = session.getRootNode();

      Node node = root.getNode("files/myarticle");
      assertEquals(node.getProperty("hippo:authorId").getLong(), SampleWorkflowSetup.oldAuthorId);

      WorkflowManager manager = ((ServicingWorkspace)session.getWorkspace()).getWorkflowManager();

      //Transaction tx = tm.getTransaction();
      //XAResource sessionXARes = ((XASession) session).getXAResource();
      //tx.enlistResource(sessionXARes);
      try {
        Workflow workflow = manager.getWorkflow("mycategory",node);
        assertNotNull(workflow);
        if(workflow instanceof SampleWorkflow) {
          SampleWorkflow myworkflow = (SampleWorkflow) workflow;
          myworkflow.renameAuthor("Jan Smit");
        } else
          fail("workflow not of proper type "+workflow.getClass().getName());

        //tx.commit();
      } catch (Exception ex) {
        System.err.println(ex.getMessage());
        ex.printStackTrace(System.err);
        //if(tx.getStatus() == Status.STATUS_ACTIVE || tx.getStatus() == Status.STATUS_UNKNOWN) {
        //  tx.rollback();
        //}
        //tx.delistResource(sessionXARes, XAResource.TMSUCCESS);
        throw ex;
      }

      session.save();
      session.refresh(false);
      assertEquals(node.getProperty("hippo:authorId").getLong(), SampleWorkflowSetup.newAuthorId);

      session.logout();
    } catch(NotSupportedException ex) {
      System.err.println("NotSupportedException: "+ex.getMessage());
      ex.printStackTrace(System.err);
      fail("NotSupportedException: "+ex.getMessage());
    } catch(SystemException ex) {
      System.err.println("SystemException: "+ex.getMessage());
      ex.printStackTrace(System.err);
      fail("SystemException: "+ex.getMessage());
    } finally {
      SampleWorkflowSetup.commonEnd(server);
    }
  }

  public void testWorkflowGui() throws RepositoryException, WorkflowException, IOException,
                                       ClassNotFoundException, NoSuchMethodException, NoSuchMethodException,
                                       InstantiationException, IllegalAccessException, InvocationTargetException {
    SampleWorkflowSetup.commonStart(server);
    try {
      Session session = server.login();
      Node root = session.getRootNode();
      
      Node node = root.getNode("files/myarticle");
      WorkflowManager manager = ((ServicingWorkspace)session.getWorkspace()).getWorkflowManager();
      WorkflowDescriptor descriptor = manager.getWorkflowDescriptor("mycategory",node);
      Class rendererClass = Class.forName(descriptor.getRendererName());
      Object[] actualArgs = new Object[2];
      actualArgs[0] = manager;
      actualArgs[1] = descriptor;
      Class[] formalArgsTypes = new Class[2];
      formalArgsTypes[0] = Class.forName("org.hippocms.repository.jr.servicing.WorkflowManager");
      formalArgsTypes[1] = Class.forName("org.hippocms.repository.workflow.WorkflowDescriptor");
      Constructor constructor = rendererClass.getConstructor(formalArgsTypes);
      GenericWorkflowRenderer renderer = (GenericWorkflowRenderer) constructor.newInstance(actualArgs);
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
    } finally {
      SampleWorkflowSetup.commonEnd(server);
    }
  }
}
