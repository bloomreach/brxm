/*
 *  Copyright 2008-2010 Hippo.
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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import org.hippoecm.repository.api.HippoNodeType;

import org.junit.Ignore;
import org.junit.Test;

public class RefreshFacetSearchTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    public void testRefreshAfterAddingNodes() throws RepositoryException {
        Repository repository = HippoRepositoryFactory.getHippoRepository().getRepository();
        Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        while(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        Node node = session.getRootNode().addNode("test");
        node.addNode("documents", "nt:unstructured").addMixin("mix:referenceable");
        session.save();
        node = node.addNode("navigation", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "query");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "x" });
        session.save();
        session.refresh(false);
        session.getRootNode().getNode("test/documents").addNode("dummy");
        //session.getRootNode().getNode("test/documents").addNode("test", "hippo:testdocument");
        //session.getRootNode().getNode("test/navigation/xyz").setProperty(HippoNodeType.HIPPO_QUERYNAME, "blaat");
        session.save();
        session.refresh(false);
        session.getRootNode().getNode("test/documents").addNode("aap");
        session.save();
        session.getRootNode().getNode("test/navigation").remove();
        session.save();
        session.getRootNode().getNode("test").remove();
        session.save();
    }
}
