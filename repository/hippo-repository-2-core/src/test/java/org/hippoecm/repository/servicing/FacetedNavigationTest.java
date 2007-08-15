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
package org.hippoecm.repository.servicing;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class FacetedNavigationTest extends TestCase {
    private final static String SVN_ID = "$Id$";

    public void testPaths() throws Exception {
        Exception firstException = null;
        HippoRepository repository = null;
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            assertNotNull(repository);
            Session session = repository.login();
            Node node = session.getRootNode().getNode("navigation").getNode("byAuthorSource").getNode("resultset");
            assertNotNull(node);
            Item item = session.getItem("/navigation/byAuthorSource/resultset");
            assertNotNull(node);
            session.logout();
        } catch (RepositoryException ex) {
            System.err.println("RepositoryException: "+ex.getMessage());
            ex.printStackTrace(System.err);
            fail("unexpected repository exception " + ex.getMessage());
            firstException = ex;
        } finally {
            boolean exceptionOccurred = false;
            try {
                if (repository != null) {
                    repository.close();
                    repository = null;
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
