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

import static org.junit.Assert.assertTrue;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;

public class HREPTWO425Test extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Test
    public void testIssue() throws RepositoryException {
        Node node = session.getRootNode().addNode("test");
        node.addMixin("hippo:harddocument");
        session.save();
        node = node.addNode("n", HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { });
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[] { });
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[] { });
        session.save();
        session.refresh(false);
        assertTrue(session.getRootNode().getNode("test").hasNode("n"));
        assertTrue(session.getRootNode().getNode("test").getNode("n").hasNode("n"));
        // Actual issue test follows
        assertTrue(session.getRootNode().getNode("test").getNode("n").getNode("n").hasNode("n"));
        assertTrue(session.getRootNode().getNode("test").getNode("n").getNode("n").getNode("n").hasNode("n"));
    }
}
