/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.transaction.NotSupportedException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.atomikos.icatch.jta.UserTransactionManager;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TransactionTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /**
     * Handle atomikos setup and create transaction test node
     */
    public void setUp() throws Exception {
        System.setProperty("com.atomikos.icatch.file", "../src/test/resources/jta.properties"); // FIXME
        super.setUp();
        Node root = session.getRootNode();
        root.addNode("transactiontest");
        session.save();
    }

    public void tearDown() throws Exception {
        session.getRootNode().getNode("transactiontest").remove();
        session.save();
        super.tearDown();
    }

    /**
     * Create Atomikos UserTransActionManger instance
     * @return
     * @throws NotSupportedException
     */
    public TransactionManager getTransactionManager() throws NotSupportedException {
        TransactionManager tm = new UserTransactionManager();
        return tm;
    }

    @Test
    public void testTransactionCommit() throws Exception {
        boolean rollback = true;
        UserTransaction ut = null;
        Node root = session.getRootNode();
        Node txRoot = root.getNode("transactiontest");
        assertNotNull(txRoot);
        try {
            ut = server.getUserTransaction(getTransactionManager(), session);
            ut.begin();
            txRoot.addNode("x1");
            assertNotNull(txRoot.getNode("x1"));
            session.save();
            assertNotNull(txRoot.getNode("x1"));
            rollback = false;
        } finally {
            if (rollback) {
                ut.rollback();
                fail("Unable to commit UserTransaction.");
            } else {
                ut.commit();            }
        }
        assertNotNull(txRoot.getNode("x1"));
    }

    @Test
    public void testTransactionRollback() throws Exception {
        UserTransaction ut = null;
        Node root = session.getRootNode();
        assertNotNull(root);
        Node txRoot = root.getNode("transactiontest");
        try {
            ut = server.getUserTransaction(getTransactionManager(), session);
            ut.begin();
            txRoot.addNode("x2");
            assertNotNull(txRoot.getNode("x2"));
            session.save();
        } finally {
            // always rollback for test
            ut.rollback();
        }
        try {
            txRoot.getNode("x2");
            fail("Node node not removed after rollback.");
        } catch (PathNotFoundException e) {
            // expected
        }
    }
}
