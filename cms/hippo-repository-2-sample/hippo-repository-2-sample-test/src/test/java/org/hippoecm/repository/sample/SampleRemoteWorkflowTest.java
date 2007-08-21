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
import java.rmi.AlreadyBoundException;

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

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.servicing.DocumentManager;
import org.hippoecm.repository.servicing.Document;
import org.hippoecm.repository.servicing.ServicingWorkspace;
import org.hippoecm.repository.servicing.WorkflowManager;

import org.hippoecm.repository.workflow.Workflow;
import org.hippoecm.repository.workflow.WorkflowException;

public class SampleRemoteWorkflowTest extends TestCase {
    private HippoRepositoryServer backgroundServer;
    private HippoRepository server;

    public void setUp() throws Exception {
        backgroundServer = new HippoRepositoryServer();
        backgroundServer.run(true);
        Thread.sleep(3000);
        server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/jackrabbit.repository");
    }

    public void tearDown() throws Exception {
        server.close();
        backgroundServer.close();
        Thread.sleep(3000);
    }

    public void testWorkflow() throws RepositoryException, WorkflowException, IOException, Exception {
        SampleWorkflowSetup.commonStart(backgroundServer);
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

            WorkflowManager manager = ((ServicingWorkspace) session.getWorkspace()).getWorkflowManager();

            //Transaction tx = tm.getTransaction();
            //XAResource sessionXARes = ((XASession) session).getXAResource();
            //tx.enlistResource(sessionXARes);
            try {
                Workflow workflow = manager.getWorkflow("mycategory", node);
                assertNotNull(workflow);
                if (workflow instanceof SampleWorkflow) {
                    SampleWorkflow myworkflow = (SampleWorkflow) workflow;
                    myworkflow.renameAuthor("Jan Smit");
                } else
                    fail("workflow not of proper type " + workflow.getClass().getName());

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
        } catch (NotSupportedException ex) {
            System.err.println("NotSupportedException: " + ex.getMessage());
            ex.printStackTrace(System.err);
            fail("NotSupportedException: " + ex.getMessage());
        } catch (SystemException ex) {
            System.err.println("SystemException: " + ex.getMessage());
            ex.printStackTrace(System.err);
            fail("SystemException: " + ex.getMessage());
        } finally {
            SampleWorkflowSetup.commonEnd(backgroundServer);
        }
    }

}
