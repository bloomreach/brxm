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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * These tests try all kind of variants of browsing through facetted navigation and
 * changing, with and without saving physical content (real nodes and external nodes), 
 * changing content from within other sessions, and concurrent runs. The tests should indicate
 * when there is a problem with the HippoLocalISM. Though take into account, that we are not testing
 * the HippoLocalISM directly because this is simply not possible, but only indirect by doing many
 * different tests, involving different JCR calls. This class cannot garantuee that there are
 * no possible errors in the HippoLocalISM. Though, it might be the first indication when something 
 * regargding the HippoLocalISM is broken.
 * 
 * If one of these tests fails, it might indicate that the <code>HippoLocalItemStateManager</code>
 * has a problem. Also changes to the item state managers in Jackrabbit trunk might result
 * in errors in these tests (the HippoLocalItemStateManager is pretty close coupled to the ISM;'s 
 * in Jackrabbit)
 */
public class HippoISMTests extends FacetedNavigationAbstractTest {
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();
    
    public void testTrivialMultipleTraverseVirtualNavigation() throws RepositoryException{
        try{
            Node ExternalNode = commonStart();
            traverse(ExternalNode);
            traverse(ExternalNode);
            traverse(ExternalNode);
        } catch(NullPointerException ex) {
            fail(ex.getMessage());
        } catch(RepositoryException ex) {
            fail(ex.getMessage());
        } finally {
            session.logout();
            session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            commonEnd();
        }
    }
    
    
    
}
