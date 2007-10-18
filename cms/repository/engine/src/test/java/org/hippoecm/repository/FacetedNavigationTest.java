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
package org.hippoecm.repository;

import java.io.IOException;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;

public class FacetedNavigationTest extends FacetedNavigationAbstractTest {

    private static final String SYSTEMUSER_ID = "systemuser";
    private static final char[] SYSTEMUSER_PASSWORD = "systempass".toCharArray();

    public void testTraversal() throws RepositoryException, IOException {
        Node node = commonStart();
        traverse(node); // for a full verbose dump use: Utilities.dump(root);
        commonEnd();
    }

    public void testCounts() throws RepositoryException, IOException {
        numDocs = 500;
        commonStart();
        check("/navigation/xyz/x1", 1, 0, 0);
        check("/navigation/xyz/x2", 2, 0, 0);
        check("/navigation/xyz/x1/y1", 1, 1, 0);
        check("/navigation/xyz/x1/y2", 1, 2, 0);
        check("/navigation/xyz/x2/y1", 2, 1, 0);
        check("/navigation/xyz/x2/y2", 2, 2, 0);
        check("/navigation/xyz/x1/y1/z1", 1, 1, 1);
        check("/navigation/xyz/x1/y1/z2", 1, 1, 2);
        check("/navigation/xyz/x1/y2/z1", 1, 2, 1);
        check("/navigation/xyz/x1/y2/z2", 1, 2, 2);
        check("/navigation/xyz/x2/y1/z1", 2, 1, 1);
        check("/navigation/xyz/x2/y1/z2", 2, 1, 2);
        check("/navigation/xyz/x2/y2/z1", 2, 2, 1);
        check("/navigation/xyz/x2/y2/z2", 2, 2, 2);
        commonEnd();
    }
    
    public void testGetItemFromSession() throws RepositoryException {
        commonStart();
        
        String basePath = "/navigation/xyz/x1/y1/z2";
        Item item = session.getItem(basePath);
        assertNotNull(item);
        assertTrue(item instanceof Node);        
        Node baseNode = (Node)item;
        
        Node resultSetNode_1 = baseNode.getNode(HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_1);
        
        Node resultSetNode_2 = (Node)session.getItem(basePath + "/" + HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_2);
        
        commonEnd();
    }

    public void getGetItemFromNode() throws RepositoryException {
        commonStart();
        
        String basePath = "/navigation/xyz/x1/y1/z2";
        Item item = session.getItem(basePath);
        assertNotNull(item);
        assertTrue(item instanceof Node);        
        Node baseNode = (Node)item;
        
        Node resultSetNode_1 = baseNode.getNode(HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_1);
        
        Node resultSetNode_2 = (Node)session.getItem(basePath + "/" + HippoNodeType.HIPPO_RESULTSET);
        assertNotNull(resultSetNode_2);
        
        commonEnd();
    }

    public static void main(String[] args) {
        try {
            String location = args.length > 0 ? args[0] : "rmi://localhost:1099/jackrabbit.repository";
            HippoRepository repository;
            if (location != null) {
                repository = HippoRepositoryFactory.getHippoRepository(location);
            } else {
                repository = HippoRepositoryFactory.getHippoRepository();
            }
            FacetedNavigationTest filler = new FacetedNavigationTest();
            Session session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            filler.fill();
            session.logout();
            repository.close();
        } catch (Exception ex) {
            System.err.println("RepositoryException: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
