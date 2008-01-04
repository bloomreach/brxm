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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;

import com.atomikos.icatch.jta.UserTransactionManager;

public class SampleWorkflowTest extends TestCase {
    private HippoRepository server;

    @Override
    public void setUp() throws Exception {
        System.setProperty("com.atomikos.icatch.file", "../src/test/resources/jta.properties");
        server = HippoRepositoryFactory.getHippoRepository();
    }

    @Override
    public void tearDown() throws Exception {
        server.close();
    }

    /**
     * Create UserTransActionManger instance
     * @return
     */
    public TransactionManager getTransactionManager() {
        return new UserTransactionManager();
    }

    public void testWorkflow() throws RepositoryException, WorkflowException, IOException, Exception {
        SampleWorkflowSetup.commonStart(server);
        try {
            Session session = server.login("admin","admin".toCharArray());

            UserTransaction ut = server.getUserTransaction(getTransactionManager(), session);
            ut.begin();

            Node root = session.getRootNode();
            Node node = root.getNode("files/myarticle");
            assertEquals(node.getProperty("hipposample:authorId").getLong(), SampleWorkflowSetup.oldAuthorId);
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

                ut.commit();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                ut.rollback();
                throw ex;
            }

            session.save();
            session.refresh(false);
            assertEquals(node.getProperty("hipposample:authorId").getLong(), SampleWorkflowSetup.newAuthorId);

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
            SampleWorkflowSetup.commonEnd(server);
        }
    }

    public void testWorkflowGui() throws RepositoryException, IOException, ClassNotFoundException,
            NoSuchMethodException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        SampleWorkflowSetup.commonStart(server);
        try {
            Session session = server.login("dummy","dummy".toCharArray());
            Node root = session.getRootNode();

            Node node = root.getNode("files/myarticle");
            WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            WorkflowDescriptor descriptor = manager.getWorkflowDescriptor("mycategory", node);
            Class rendererClass = Class.forName(descriptor.getRendererName());
            Object[] actualArgs = new Object[2];
            actualArgs[0] = manager;
            actualArgs[1] = descriptor;
            Class[] formalArgsTypes = new Class[2];
            formalArgsTypes[0] = Class.forName("org.hippoecm.repository.api.WorkflowManager");
            formalArgsTypes[1] = Class.forName("org.hippoecm.repository.api.WorkflowDescriptor");
            Constructor constructor = rendererClass.getConstructor(formalArgsTypes);
            GenericWorkflowRenderer renderer = (GenericWorkflowRenderer) constructor.newInstance(actualArgs);
            try {
                renderer.invoke();
            } catch (WorkflowException ex) {
            }
        } catch (ClassNotFoundException ex) {
            throw ex;
        } catch (NoSuchMethodException ex) {
            throw ex;
        } catch (InstantiationException ex) {
            throw ex;
        } catch (IllegalAccessException ex) {
            throw ex;
        } catch (InvocationTargetException ex) {
            throw ex;
        } finally {
            SampleWorkflowSetup.commonEnd(server);
        }
    }
}
