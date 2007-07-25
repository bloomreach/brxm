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
package org.hippocms.repository.jr.servicing;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;

public class PathsTest extends TestCase {
    private final static String SVN_ID = "$Id$";

    public void testPaths() throws Exception {
        Exception firstException = null;
        HippoRepository repository = null;
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            assertNotNull(repository);
            Session session = repository.login();
            Node root = session.getRootNode();
            Node sub1 = root.addNode("sub");
            Node sub2 = sub1.addNode("subsub");

            assertTrue(root instanceof ServicingNodeImpl);
            assertTrue(sub1 instanceof ServicingNodeImpl);
            assertTrue(sub2 instanceof ServicingNodeImpl);
            Node realroot = ((ServicingNodeImpl) root).unwrap(root);
            Node realsub1 = ((ServicingNodeImpl) sub1).unwrap(sub1);
            Node realsub2 = ((ServicingNodeImpl) sub2).unwrap(sub2);

            sub2.remove();
            sub1.remove();
            session.logout();
        } catch (RepositoryException ex) {
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
