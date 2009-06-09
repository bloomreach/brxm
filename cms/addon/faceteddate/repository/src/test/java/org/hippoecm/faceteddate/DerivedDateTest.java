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
package org.hippoecm.faceteddate;

import java.util.Calendar;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.Utilities;

public class DerivedDateTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Calendar date;
    private Node root;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(true);

        root = session.getRootNode();
        if(root.hasNode("test"))
            root.getNode("test").remove();
        root = root.addNode("test");
        session.save();

        session.getWorkspace().copy("/hippo:configuration/hippo:derivatives/date", "/hippo:configuration/hippo:derivatives/test1");
        Node node = session.getRootNode().getNode("hippo:configuration/hippo:derivatives/test1");
        node.setProperty("hippo:nodetype", "hippo:datedocument2");
        node.getNode("hippo:accessed/date").setProperty("hippo:relPath", "hippo:d1");
        for(NodeIterator iter = node.getNode("hippo:derived").getNodes(); iter.hasNext(); ) {
            Node derivedDef = iter.nextNode();
            Property prop = derivedDef.getProperty("hippo:relPath");
            prop.setValue("hippo:d1fields/"+prop.getString());
        }

        session.getWorkspace().copy("/hippo:configuration/hippo:derivatives/date", "/hippo:configuration/hippo:derivatives/test2");
        node = session.getRootNode().getNode("hippo:configuration/hippo:derivatives/test2");
        node.setProperty("hippo:nodetype", "hippo:datedocument2");
        node.getNode("hippo:accessed/date").setProperty("hippo:relPath", "hippo:d2");
        for(NodeIterator iter = node.getNode("hippo:derived").getNodes(); iter.hasNext(); ) {
            Node derivedDef = iter.nextNode();
            Property prop = derivedDef.getProperty("hippo:relPath");
            prop.setValue("hippo:d2fields/"+prop.getString());
        }

        session.save();

        date = Calendar.getInstance();
        date.setTime(new Date());
    }

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
        Node node = root.addNode("doc", "hippo:datedocument1");
        node.addMixin("hippo:harddocument");
        node = node.addNode("hippo:d");
        node.addMixin("mix:referenceable");
        node.setProperty("hippostd:date", date);
        session.save();
        check(date, root.getNode("doc/hippo:d"));
    }

    @Test
    public void testFacetedDateCustom() throws Exception {
        Node node = root.addNode("doc", "hippo:datedocument2");
        node.addMixin("hippo:harddocument");
        node.setProperty("hippo:d1", date);
        node.setProperty("hippo:d2", date);
        session.save();
        check(date, root.getNode("doc/hippo:d1fields"));
        check(date, root.getNode("doc/hippo:d2fields"));
    }

    @Test
    public void testFacetedDateOptional() throws Exception {
        Node node = root.addNode("doc", "hippo:datedocument2");
        node.addMixin("hippo:harddocument");
        node.setProperty("hippo:d1", date);
        session.save();
        check(date, root.getNode("doc/hippo:d1fields"));
        assertFalse(root.hasNode("doc/hippo:d2fields"));
    }

    @Test
    public void testSearch() throws Exception {
        root.addNode("docs","nt:unstructured").addMixin("mix:referenceable");
        Node doc = root.getNode("docs").addNode("doc", "hippo:datedocument1");
        doc.addMixin("hippo:harddocument");
        doc = doc.addNode("hippo:d");
        doc.addMixin("mix:referenceable");
        doc.setProperty("hippostd:date", date);
        session.save();

        Node search = root.addNode("search", "hippo:facetsearch");
        search.setProperty("hippo:queryname", "test");
        search.setProperty("hippo:docbase", root.getNode("docs").getUUID());
        search.setProperty("hippo:facets", new String[] { "hippo:d/hippostd:dayofmonth" });
        session.save();

        search = root.getNode("search");
        Utilities.dump(System.err, search);

        assertNotNull(traverse(session, "/test/search/" + date.get(Calendar.DAY_OF_MONTH) + "/hippo:resultset/doc"));
    }

    @Test
    public void testSearchMultiLevel() throws Exception {
        root.addNode("docs","nt:unstructured").addMixin("mix:referenceable");
        Node doc = root.getNode("docs").addNode("doc", "hippo:datedocument1");
        doc.addMixin("hippo:harddocument");
        doc = doc.addNode("hippo:d");
        doc.addMixin("mix:referenceable");
        doc.setProperty("hippostd:date", date);
        session.save();

        Node search = root.addNode("search", "hippo:facetsearch");
        search.setProperty("hippo:queryname", "test");
        search.setProperty("hippo:docbase", root.getNode("docs").getUUID());
        search.setProperty("hippo:facets", new String[] { "hippo:d/hippostd:dayofmonth", "hippo:d/hippostd:year" });
        session.save();

        search = root.getNode("search");
        Utilities.dump(System.err, search);

        assertNotNull(traverse(session, "/test/search/" + date.get(Calendar.DAY_OF_MONTH) + "/" + date.get(Calendar.YEAR) + "/hippo:resultset/doc"));
    }
}
