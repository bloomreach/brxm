/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertNotNull;

public class AuthorizationSanityTest extends RepositoryTestCase {

    private void visit(Node node, Session check) throws RepositoryException {
        if(node.getDepth() > 0 && check.itemExists(node.getPath())) {
            assertNotNull("should be able to reach node "+node.getPath(), traverse(check, node.getPath()));
        }
        for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            visit(iter.nextNode(), check);
        }
    }

    @Test
    public void testTraversability() throws Exception {
        Node root = session.getRootNode();
        Session anonymous = server.login();
        assertNotNull(anonymous);
        visit(root, anonymous);
    }
}
