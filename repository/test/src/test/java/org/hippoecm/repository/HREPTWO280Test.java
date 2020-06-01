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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HREPTWO280Test extends FacetedNavigationAbstractTest {

    @Test
    public void testIssue() throws RepositoryException {
        commonStart();

        Node node, searchNode = getSearchNode();
        traverse(session.getRootNode().getNode("test/navigation"));
        node = session.getRootNode().getNode("test/documents").addNode("aap");
        node.setProperty("x", "x1");
        session.save();
        searchNode = getSearchNode();
        traverse(searchNode);

        commonEnd();
    }
}
