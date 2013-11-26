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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class DerivedDateTest extends RepositoryTestCase {

    private Calendar date;
    private Node root;

    @After
    @Override
    public void tearDown() throws Exception {
        if(session.nodeExists("/test")) {
            session.getNode("/test").remove();
        }
        if(session.nodeExists("/hippo:configuration/hippo:derivatives/test1")) {
            session.getNode("/hippo:configuration/hippo:derivatives/test1").remove();
        }
        if(session.nodeExists("/hippo:configuration/hippo:derivatives/test2")) {
            session.getNode("/hippo:configuration/hippo:derivatives/test2").remove();
        }
        session.save();
        super.tearDown();
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        root = session.getRootNode();
        if(session.nodeExists("/test")) {
            session.getNode("/test").remove();
        }
        root = root.addNode("test");
        session.save();

        session.getWorkspace().copy("/hippo:configuration/hippo:derivatives/date", "/hippo:configuration/hippo:derivatives/test1");
        Node test1 = session.getNode("/hippo:configuration/hippo:derivatives/test1");
        test1.setProperty("hipposys:nodetype", "hippo:datedocument2");
        test1.getNode("hipposys:accessed/date").setProperty("hipposys:relPath", "hippo:d1");
        for(NodeIterator iter = test1.getNode("hipposys:derived").getNodes(); iter.hasNext(); ) {
            Node derivedDef = iter.nextNode();
            Property prop = derivedDef.getProperty("hipposys:relPath");
            prop.setValue("hippo:d1fields/"+prop.getString());
        }

        session.getWorkspace().copy("/hippo:configuration/hippo:derivatives/date", "/hippo:configuration/hippo:derivatives/test2");
        Node test2 = session.getRootNode().getNode("hippo:configuration/hippo:derivatives/test2");
        test2.setProperty("hipposys:nodetype", "hippo:datedocument2");
        test2.getNode("hipposys:accessed/date").setProperty("hipposys:relPath", "hippo:d2");
        for(NodeIterator iter = test2.getNode("hipposys:derived").getNodes(); iter.hasNext(); ) {
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
        Node handle = session.getNode("/test").addNode("doc", "hippo:handle");
        Node node = handle.addNode("doc", "hippo:datedocument1");
        node.addMixin("mix:versionable");
        node = node.addNode("hippo:d");
        node.addMixin("mix:referenceable");
        node.setProperty("hippostd:date", date);
        session.save();
        check(date, session.getNode("/test/doc/doc/hippo:d"));
    }

    @Test
    public void testFacetedDateCustom() throws Exception {
        Node handle = session.getNode("/test").addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        Node node = handle.addNode("doc", "hippo:datedocument2");
        node.addMixin("mix:versionable");
        node.setProperty("hippo:d1", date);
        node.setProperty("hippo:d2", date);
        session.save();
        check(date, session.getNode("/test/doc/doc/hippo:d1fields"));
        check(date, session.getNode("/test/doc/doc/hippo:d2fields"));
    }

    @Test
    public void testFacetedDateOptional() throws Exception {
        Node handle = session.getNode("/test").addNode("doc", "hippo:handle");
        handle.addMixin("hippo:hardhandle");
        Node node = handle.addNode("doc", "hippo:datedocument2");
        node.addMixin("mix:versionable");
        node.setProperty("hippo:d1", date);
        session.save();
        check(date, session.getNode("/test/doc/doc/hippo:d1fields"));
        assertFalse(session.nodeExists("/test/doc/doc/hippo:d2fields"));
    }

}
