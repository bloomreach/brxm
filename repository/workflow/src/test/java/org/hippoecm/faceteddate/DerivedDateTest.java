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
package org.hippoecm.faceteddate;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.util.TraversingItemVisitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DerivedDateTest extends RepositoryTestCase {

    private Calendar date;
    private Node root;

    @After
    @Override
    public void tearDown() throws Exception {
        root = session.getRootNode();
        if(root.hasNode("test"))
            root.getNode("test").remove();
        if(root.hasNode("hippo:configuration/hippo:derivatives/test1"))
            root.getNode("hippo:configuration/hippo:derivatives/test1").remove();
        if(root.hasNode("hippo:configuration/hippo:derivatives/test2"))
            root.getNode("hippo:configuration/hippo:derivatives/test2").remove();
        session.save();
        super.tearDown();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        root = session.getRootNode();
        if(root.hasNode("test"))
            root.getNode("test").remove();
        root = root.addNode("test");
        session.save();

        session.getWorkspace().copy("/hippo:configuration/hippo:derivatives/date", "/hippo:configuration/hippo:derivatives/test1");
        Node node = session.getRootNode().getNode("hippo:configuration/hippo:derivatives/test1");
        node.setProperty("hipposys:nodetype", "hippo:datedocument2");
        node.getNode("hipposys:accessed/date").setProperty("hipposys:relPath", "hippo:d1");
        for(NodeIterator iter = node.getNode("hipposys:derived").getNodes(); iter.hasNext(); ) {
            Node derivedDef = iter.nextNode();
            Property prop = derivedDef.getProperty("hipposys:relPath");
            prop.setValue("hippo:d1fields/"+prop.getString());
        }

        session.getWorkspace().copy("/hippo:configuration/hippo:derivatives/date", "/hippo:configuration/hippo:derivatives/test2");
        node = session.getRootNode().getNode("hippo:configuration/hippo:derivatives/test2");
        node.setProperty("hipposys:nodetype", "hippo:datedocument2");
        node.getNode("hipposys:accessed/date").setProperty("hipposys:relPath", "hippo:d2");
        for(NodeIterator iter = node.getNode("hipposys:derived").getNodes(); iter.hasNext(); ) {
            Node derivedDef = iter.nextNode();
            Property prop = derivedDef.getProperty("hipposys:relPath");
            prop.setValue("hippo:d2fields/"+prop.getString());
        }

        session.save();

        date = Calendar.getInstance();
        date.setTime(new Date());
    }

    private void check(Calendar date, Node info) throws Exception {
        assertEquals((long)date.get(Calendar.DAY_OF_YEAR), info.getProperty("hippostd:dayofyear").getLong());
        assertEquals((long)date.get(Calendar.SECOND), info.getProperty("hippostd:second").getLong());
        assertEquals((long)date.get(Calendar.DAY_OF_WEEK), info.getProperty("hippostd:dayofweek").getLong());
        assertEquals((long)date.get(Calendar.MONTH), info.getProperty("hippostd:month").getLong());
        assertEquals((long)date.get(Calendar.MINUTE), info.getProperty("hippostd:minute").getLong());
        assertEquals((long)date.get(Calendar.DAY_OF_MONTH), info.getProperty("hippostd:dayofmonth").getLong());
        assertEquals((long)date.get(Calendar.HOUR_OF_DAY), info.getProperty("hippostd:hourofday").getLong());
        assertEquals((long)date.get(Calendar.WEEK_OF_YEAR), info.getProperty("hippostd:weekofyear").getLong());
        assertEquals((long)date.get(Calendar.YEAR), info.getProperty("hippostd:year").getLong());
    }

    @Test
    public void testFacetedDateNode() throws Exception {
        Node handle = root.addNode("doc", "hippo:handle");
        Node node = handle.addNode("doc", "hippo:datedocument1");
        node.addMixin("hippo:harddocument");
        node = node.addNode("hippo:d");
        node.addMixin("mix:referenceable");
        node.setProperty("hippostd:date", date);
        session.save();
        check(date, root.getNode("doc/doc/hippo:d"));
    }

    @Test
    public void testFacetedDateCustom() throws Exception {
        Node handle = root.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        Node node = handle.addNode("doc", "hippo:datedocument2");
        node.addMixin("hippo:harddocument");
        node.setProperty("hippo:d1", date);
        node.setProperty("hippo:d2", date);
        session.save();
        check(date, root.getNode("doc/doc/hippo:d1fields"));
        check(date, root.getNode("doc/doc/hippo:d2fields"));
    }

    @Test
    public void testFacetedDateOptional() throws Exception {
        Node handle = root.addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        Node node = handle.addNode("doc", "hippo:datedocument2");
        node.addMixin("hippo:harddocument");
        node.setProperty("hippo:d1", date);
        session.save();
        check(date, root.getNode("doc/doc/hippo:d1fields"));
        assertFalse(root.hasNode("doc/doc/hippo:d2fields"));
    }

    @Test
    public void testSearch() throws Exception {
        root.addNode("docs","nt:unstructured").addMixin("mix:referenceable");

        Node handle = root.getNode("docs").addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        Node doc = handle.addNode("doc", "hippo:datedocument1");
        doc.addMixin("hippo:harddocument");
        doc = doc.addNode("hippo:d");
        doc.addMixin("mix:referenceable");
        doc.setProperty("hippostd:date", date);
        session.save();

        Node search = root.addNode("search", "hippo:facetsearch");
        search.setProperty("hippo:queryname", "test");
        search.setProperty("hippo:docbase", root.getNode("docs").getIdentifier());
        search.setProperty("hippo:facets", new String[] { "hippo:d/hippostd:dayofmonth" });
        session.save();
        search = root.getNode("search");
        assertNotNull(traverse(session, "/test/search/" + date.get(Calendar.DAY_OF_MONTH) + "/hippo:resultset/doc"));
    }


    @Test
    public void testSearchMultiLevel() throws Exception {
        root.addNode("docs","nt:unstructured").addMixin("mix:referenceable");
        Node handle = root.getNode("docs").addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        Node doc = handle.addNode("doc", "hippo:datedocument1");
        doc.addMixin("hippo:harddocument");
        doc = doc.addNode("hippo:d");
        doc.addMixin("mix:referenceable");
        doc.setProperty("hippostd:date", date);
        session.save();

        Node search = root.addNode("search", "hippo:facetsearch");
        search.setProperty("hippo:queryname", "test");
        search.setProperty("hippo:docbase", root.getNode("docs").getIdentifier());
        search.setProperty("hippo:facets", new String[] { "hippo:d/hippostd:dayofmonth", "hippo:d/hippostd:year" });
        session.save();

        search = root.getNode("search");

        assertNotNull(traverse(session, "/test/search/" + date.get(Calendar.DAY_OF_MONTH) + "/" + date.get(Calendar.YEAR) + "/hippo:resultset/doc"));
    }

    @Test
    public void testQuery() throws Exception {
        int level1 = 3;
        int level2 = 4;
        int level3 = 3;
        int level4 = 2;
        Node node = root.addNode("documents", "nt:unstructured");
        node.addMixin("mix:referenceable");
        for (int i1 = 0; i1 < level1; i1++) {
            Node child1 = node.addNode("folder" + i1, "hippostd:folder");
            child1.addMixin("hippo:harddocument");
            for (int i2 = 0; i2 < level2; i2++) {
                Node child2 = child1.addNode("folder" + i2, "hippostd:folder");
                child2.addMixin("hippo:harddocument");
                for (int i3 = 0; i3 < level3; i3++) {
                    Node child3 = child2.addNode("folder" + i3, "hippostd:folder");
                    child3.addMixin("hippo:harddocument");
                    for (int i4 = 0; i4 < level4; i4++) {
                        Node handle = child3.addNode("document" + i4, "hippo:handle");
                        handle.addMixin("hippo:hardhandle");
                        Node document = handle.addNode("document" + i4, "hippo:datedocument1");
                        document.addMixin("hippo:harddocument");
                        Node date = document.addNode("hippo:d");
                        date.addMixin("mix:referenceable");
                        Calendar cal = Calendar.getInstance();
                        cal.set(2000 + i1, i2, i3 + 1);
                        date.setProperty("hippostd:date", cal);
                    }
                }
            }
        }
        session.save();

        Node search = root.addNode("search", "hippo:facetsearch");
        search.setProperty("hippo:queryname", "test");
        search.setProperty("hippo:docbase", root.getNode("documents").getIdentifier());
        search.setProperty("hippo:facets", new String[] {"hippo:d/hippostd:month", "hippo:d/hippostd:year", "hippo:d/hippostd:dayofmonth"});
        session.save();

        search = root.getNode("search");
        search.accept(new TraversingItemVisitor.Default() {
            public void entering(Node node, int level) {

            }
            public void visit(Node node) throws RepositoryException {
                if(node.getName().equals("hippo:resultset"))
                    return;
                super.visit(node);
            }
        });
        QueryManager qmgr = session.getWorkspace().getQueryManager();
        Query query = qmgr.createQuery("//*[hippo:d/@hippostd:year=2002] order by @hippostd:month asc", Query.XPATH);
        QueryResult result = query.execute();
        int count = 0;
        for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
            Node n = iter.nextNode();
            ++count;
        }
        // there happen to be 1595 created docs in the year 2002 with the above for loop
        assertTrue("Count should be 1595", 24 == count);
    }
}
