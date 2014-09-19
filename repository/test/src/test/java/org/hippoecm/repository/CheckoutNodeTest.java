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

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckoutNodeTest extends RepositoryTestCase {

    @Test
    public void testCheckoutWithVirtualNodesPresent() throws RepositoryException {
        Node node, root = session.getRootNode().addNode("test","nt:unstructured");
        root.addMixin("mix:referenceable");
        node = root.addNode("documents");
        node = node.addNode("document","hippo:testdocument");
        node.addMixin("mix:versionable");
        node.setProperty("aap", "noot");
        session.save();

        Node navigation = root.addNode("navigation", "hippo:testdocument");
        navigation.addMixin("mix:versionable");
        node = navigation.addNode("search",HippoNodeType.NT_FACETSELECT);
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[0]);
        node.setProperty(HippoNodeType.HIPPO_VALUES, new String[0]);
        node.setProperty(HippoNodeType.HIPPO_MODES, new String[0]);
        session.save();
        session.refresh(false);
        navigation.checkin();

        assertTrue(navigation.getNode("search").hasNode("documents"));
        assertTrue(navigation.getNode("search").getNode("documents").hasNode("document"));

        navigation.checkout();

        assertEquals("documents", root.getNode("navigation").getNode("search").getNode("documents").getName());
    }
}
