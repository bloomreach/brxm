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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.TestCase;
import org.junit.*;
import static org.junit.Assert.*;

public class BasicTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    public void testBasics() throws Exception {
        boolean exceptionOccurred = false;
        Exception firstException = null;
        HippoRepositoryServer repositoryServer = null;
        HippoRepository repositoryClient = null;
        try {
            repositoryServer = new HippoRepositoryServer();
            assertNotNull(repositoryServer);
            repositoryServer.run(true);
            Thread.sleep(3000);
            repositoryClient = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
            assertNotNull(repositoryClient);
            Session session = repositoryClient.login();
            assertNotNull(session);
            Node root = session.getRootNode();
            assertNotNull(root);
            session.save();
            session.logout();
        } catch (RepositoryException ex) {
            fail("unexpected repository exception " + ex.getMessage());
            firstException = ex;
            exceptionOccurred = true;
        } finally {
            try {
                if (repositoryClient != null) {
                    repositoryClient.close();
                    repositoryClient = null;
                }
            } catch (Exception ex) {
                if (firstException == null) {
                    firstException = ex;
                    exceptionOccurred = true;
                }
            }
            try {
                if (repositoryServer != null) {
                    repositoryServer.close();
                    repositoryServer = null;
                }
            } catch (Exception ex) {
                if (firstException == null) {
                    firstException = ex;
                    exceptionOccurred = true;
                }
            }
            if (exceptionOccurred)
                throw firstException;
        }
    }
}
