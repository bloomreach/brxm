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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import org.hippoecm.repository.api.HippoNodeType;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RefreshFacetSearchTest extends TestCase{

    @Test
    public void testRefreshAfterAddingNodes() throws RepositoryException {
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

    @Test
    public void testRefreshIndexAfterPropertyChange() throws RepositoryException {
        Node test = session.getRootNode().addNode("test");
        test.addMixin("mix:referenceable");
        Node documents = test.addNode("documents", "nt:unstructured");
        documents.addMixin("hippo:harddocument");
        documents.setProperty("x", "xValue");
        documents.setProperty("y", "yValue");
        session.save();
        session.refresh(false);

        Node navigation = test.addNode("navigation", HippoNodeType.NT_FACETSEARCH);
        navigation.setProperty(HippoNodeType.HIPPO_QUERYNAME, "query");
        navigation.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test").getIdentifier());
        navigation.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{"x"});
        session.save();
        session.refresh(false);

        navigation = test.getNode("navigation");
        assertEquals(1, navigation.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
        session.save();
        session.refresh(false);

        QueryManager queryMgr = session.getWorkspace().getQueryManager();
        Query query = queryMgr.createQuery("select * from " + HippoNodeType.NT_FACETSEARCH, Query.SQL);
        QueryResult result = query.execute();
        NodeIterator nodes = result.getNodes();
        assertTrue(nodes.hasNext());
        assertTrue(nodes.nextNode().isSame(navigation));

        navigation = test.getNode("navigation");
        navigation.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{"y"});
        session.save();
        session.refresh(false);

        queryMgr = session.getWorkspace().getQueryManager();
        query = queryMgr.createQuery("select * from " + HippoNodeType.NT_FACETSEARCH, Query.SQL);
        result = query.execute();
        nodes = result.getNodes();
        assertTrue(nodes.hasNext());
        assertTrue(nodes.nextNode().isSame(navigation));
    }
}
