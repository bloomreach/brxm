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
import junit.framework.TestSuite;

import java.rmi.RemoteException;
import java.rmi.AlreadyBoundException;

import org.hippocms.repository.jr.servicing.ServicingWorkspace;
import org.hippocms.repository.jr.servicing.ServicingNode;
import org.hippocms.repository.jr.servicing.Service;

import org.apache.jackrabbit.core.XASession;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
// FIXME: depend only on JTA, not on Atomikos
import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * @version $Id$
 */
public class ServiceTest extends TestCase {
    private final static String SVN_ID = "$Id$";

    private static String NODENAME = "documentWithService";
    private Server backgroundServer;
    private Server server;
    private boolean startService = true;

    protected void setUp() throws Exception {
        if (startService) {
            backgroundServer = new Server();
            backgroundServer.run(true);
            Thread.sleep(3000);
        }
        server = new Server("rmi://localhost:1099/jackrabbit.repository");
    }

    protected void tearDown() throws Exception {
        server.close();
        if (startService) {
            backgroundServer.close();
            Thread.sleep(3000);
        }
    }

    private Session commonStart() throws Exception {
        Session session = server.login();
        Node node, root = session.getRootNode();
        assertNotNull(session);
        node = root.addNode(NODENAME);
        node.setProperty("HasAction1", false);
        node.setProperty("HasAction2", false);
        session.save();
        return session;
    }

    private void commonEnd(Session session) throws Exception {
        session.save();
        Node root = session.getRootNode();
        Node node = root.getNode(NODENAME);
        if (node != null)
            node.remove();
        session.save();
        session.logout();
    }

    public void testGetService() throws Exception {
        Session session = commonStart();
        Node node = session.getRootNode().getNode(NODENAME);
        assertNotNull(node);
        MyService service = (MyService) ((ServicingNode) node).getService();
        assertNotNull(service);
        commonEnd(session);
    }

    public void testBasicService() throws Exception {
        Session session = commonStart();
        Node node = session.getRootNode().getNode(NODENAME);
        MyService service = (MyService) ((ServicingNode) node).getService();
        assertFalse(node.getProperty("HasAction1").getBoolean());
        try {
            service.doAction1();
            session.save();
        } catch (Exception ex) {
            fail();
        }
        assertTrue(node.getProperty("HasAction1").getBoolean());
        commonEnd(session);
    }

    public void testCompoundService() throws Exception {
        Session session = commonStart();
        Node node = session.getRootNode().getNode(NODENAME);
        MyService service = (MyService) ((ServicingNode) node).getService();
        assertFalse(node.getProperty("HasAction1").getBoolean());
        try {
            service.doAction1();
            service.doAction2();
            session.save();
        } catch (Exception ex) {
            fail();
        }
        assertTrue(node.getProperty("HasAction1").getBoolean());
        assertTrue(node.getProperty("HasAction2").getBoolean());
        commonEnd(session);
    }

    public void testFailingService() throws Exception {
        try {
            Session session = commonStart();
            Node node = session.getRootNode().getNode(NODENAME);

            UserTransactionManager utm = new UserTransactionManager();
            utm.setStartupTransactionService(false);
            utm.init();

            TransactionManager tm = utm;
            tm.begin();
            Transaction tx = tm.getTransaction();
            XAResource sessionXARes = ((XASession) session).getXAResource();
            tx.enlistResource(sessionXARes);

            MyService service = (MyService) ((ServicingNode) node).getService();
            assertFalse(node.getProperty("HasAction1").getBoolean());
            try {
                service.doAction2();
                session.save();
                service.doAction1();
                session.save();
                fail("service should have failed");
                tx.commit();
            } catch (Exception ex) {
                // if(tx.getStatus() == Status.STATUS_ACTIVE || tx.getStatus() == Status.STATUS_UNKNOWN) {
                tx.rollback();
                //}
                // tx.delistResource(sessionXARes, XAResource.TMSUCCESS);
            }
            session.refresh(false);
            node = session.getRootNode().getNode(NODENAME);
            assertFalse(node.getProperty("HasAction1").getBoolean());
            assertFalse(node.getProperty("HasAction2").getBoolean());
            commonEnd(session);
        } catch (SystemException ex) {
            throw new RepositoryException("cannot initialize transaction manager", ex);
        }
    }
}
