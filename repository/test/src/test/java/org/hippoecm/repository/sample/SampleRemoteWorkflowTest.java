/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.sample;

import java.io.File;
import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import com.atomikos.icatch.jta.UserTransactionManager;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SampleRemoteWorkflowTest {

    private HippoRepositoryServer backgroundServer;
    private HippoRepository server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        RepositoryTestCase.clear();
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        RepositoryTestCase.clear();
        HippoRepositoryFactory.setDefaultRepository((String)null);
    }

    @Before
    public void setUp() throws Exception {
        String bindingAddress = "rmi://localhost:1098/hipporepository";
        backgroundServer = new HippoRepositoryServer(new File(System.getProperty("user.dir")).getAbsolutePath(), bindingAddress);
        backgroundServer.run(true);
        Thread.sleep(3000);
        server = HippoRepositoryFactory.getHippoRepository(bindingAddress);
    }

    @After
    public void tearDown() throws Exception {
        server.close();
        backgroundServer.close();
        Thread.sleep(3000);
    }

    @Test
    public void testWorkflow() throws Exception {
        SampleWorkflowSetup.commonStart(backgroundServer);
        try {

            Session session = server.login("admin", "admin".toCharArray());

            // UserTransaction ut = server.getUserTransaction(getTransactionManager(), session);
            // ut.begin();

            Node root = session.getRootNode();

            Node node = root.getNode("files/myarticle");
            assertEquals(node.getProperty("sample:authorId").getLong(), SampleWorkflowSetup.oldAuthorId);

            WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

            try {
                Workflow workflow = manager.getWorkflow("mycategory", node);
                assertNotNull(workflow);
                if (workflow instanceof SampleWorkflow) {
                    SampleWorkflow myworkflow = (SampleWorkflow) workflow;
                    myworkflow.renameAuthor("Jan Smit");
                } else {
                    fail("workflow not of proper type " + workflow.getClass().getName());
                }
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                // ut.rollback();
                throw ex;
            }

            session.save();
            session.refresh(false);
            assertEquals(node.getProperty("sample:authorId").getLong(), SampleWorkflowSetup.newAuthorId);

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

    @Test
    public void testReturnDocument() throws RepositoryException, WorkflowException, IOException, Exception {
        SampleWorkflowSetup.commonStart(backgroundServer);
        try {

            Session session = server.login("admin", "admin".toCharArray());

            Node root = session.getRootNode();

            Node node = root.getNode("files/myarticle");

            WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

            try {
                Workflow workflow = manager.getWorkflow("mycategory", node);
                assertNotNull(workflow);
                if (workflow instanceof SampleWorkflow) {
                    SampleWorkflow myworkflow = (SampleWorkflow) workflow;
                    Document document = myworkflow.getArticle();
                    assertTrue(node.getIdentifier().equals(document.getIdentity()));
                } else {
                    fail("workflow not of proper type " + workflow.getClass().getName());
                }
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                throw ex;
            }

            session.save();
            session.refresh(false);

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
