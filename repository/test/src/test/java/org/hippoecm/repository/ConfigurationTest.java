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
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class ConfigurationTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.getRootNode().addNode("test", "nt:unstructured");
        session.save();
        session.refresh(false);
    }

    @After
    public void tearDown() throws Exception {
        while (session.getRootNode().hasNode("hippo:configuration/hippo:initialize/testnode")) {
            session.getRootNode().getNode("hippo:configuration/hippo:initialize/testnode").remove();
            session.save();
        }
        super.tearDown();
    }
    private volatile boolean updateDone;

    private boolean saveAndWaitForUpdate(Node node) throws RepositoryException {
        ObservationManager observation = node.getSession().getWorkspace().getObservationManager();
        final String path = node.getPath() + "/" + "hippo:process";
        updateDone = false;
        EventListener eventListener = new EventListener() {
            public void onEvent(EventIterator events) {
                while (events.hasNext()) {
                    try {
                        Event event = events.nextEvent();
                        if (event.getType() == Event.PROPERTY_REMOVED && path.equals(event.getPath()))
                            updateDone = true;
                    } catch (RepositoryException ex) {
                    }
                }
            }
        };
        observation.addEventListener(eventListener, Event.PROPERTY_REMOVED, node.getPath(), true, null, null, true);
        node.getSession().save();
        // small initialize items like these ought to be executed within 5 seconds
        for (int i = 0; i < 50 && !updateDone; i++) {
            try {
                Thread.sleep(100);
                session.refresh(false);
            } catch (InterruptedException ex) {
            }
        }
        observation.removeEventListener(eventListener);
        return updateDone;
    }

    private void check(String expected) throws RepositoryException {
        Node node = session.getRootNode().getNode("test/propnode");
        assertFalse(node.hasProperty("hippo:multi"));
        assertTrue(node.hasProperty("hippo:single"));
        assertEquals(expected, node.getProperty("hippo:single").getString());
    }

    private void check(String[] expected) throws RepositoryException {
        Node node = session.getRootNode().getNode("test/propnode");
        assertTrue(node.hasProperty("hippo:multi"));
        assertFalse(node.hasProperty("hippo:single"));
        Value[] values = node.getProperty("hippo:multi").getValues();
        assertEquals(expected.length, values.length);
        int count = 0;
        for (Value value : values) {
            assertEquals(expected[count++], value.getString());
        }
    }

    @Test
    public void testConfiguration() throws Exception {
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test");
        node.setProperty("hippo:content", "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" sv:name=\"testnode\"><sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property></sv:node>");
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        assertTrue(session.getRootNode().getNode("test").hasNode("testnode"));
    }

    @Test
    public void testPropertyInitializationNoParent() throws Exception {
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {"a"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
    }

    // Test for managing a non-existing single value property
    @Test
    public void testPropertyInitializationNewSingleSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {"b"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check("b");
    }

    @Test
    public void testPropertyInitializationNewSingleSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {"c", "d"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
    }

    @Test
    public void testPropertyInitializationNewSingleSetNone() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        assertFalse(session.getRootNode().getNode("test/propnode").hasProperty("hippo:single"));
        assertFalse(session.getRootNode().getNode("test/propnode").hasProperty("hippo:multi"));
    }

    @Test
    public void testPropertyInitializationNewSingleAddSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropadd", new String[] {"e"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
    }

    @Test
    public void testPropertyInitializationNewSingleAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropadd", new String[] {"f", "g"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
    }

    @Test
    public void testPropertyInitializationNewSingleSetAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {"h", "i"});
        node.setProperty("hippo:contentpropadd", new String[] {"j", "k"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
    }

    // Test for managing a non-existing multi value property
    @Test
    public void testPropertyInitializationNewMultiSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {"l"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"l"});
    }

    @Test
    public void testPropertyInitializationNewMultiSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {"m", "n"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"m", "n"});
    }

    @Test
    public void testPropertyInitializationNewMultiSetNone() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {});
    }

    @Test
    public void testPropertyInitializationNewMultiAddSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropadd", new String[] {"o"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"o"});
    }

    @Test
    public void testPropertyInitializationNewMultiAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropadd", new String[] {"p", "q"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"p", "q"});
    }

    @Test
    public void testPropertyInitializationNewMultiSetAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {"r", "s"});
        node.setProperty("hippo:contentpropadd", new String[] {"t", "u"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"r", "s", "t", "u"});
    }

    // Test for managing a existing single value property
    @Test
    public void testPropertyInitializationExistingSingleSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {"B"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check("B");
    }

    @Test
    public void testPropertyInitializationExistingSingleSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {"C", "D"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
        check("z");
    }

    @Test
    public void testPropertyInitializationExistingSingleSetNone() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        assertFalse(session.getRootNode().getNode("test/propnode").hasProperty("hippo:single"));
        assertFalse(session.getRootNode().getNode("test/propnode").hasProperty("hippo:multi"));
    }

    @Test
    public void testPropertyInitializationExistingSingleAddSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropadd", new String[] {"E"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
        check("z");
    }

    @Test
    public void testPropertyInitializationExistingSingleAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropadd", new String[] {"F", "G"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
        check("z");
    }

    @Test
    public void testPropertyInitializationExistingSingleSetAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:single");
        node.setProperty("hippo:contentpropset", new String[] {"H", "I"});
        node.setProperty("hippo:contentpropadd", new String[] {"J", "K"});
        node.setProperty("hippo:process", true);
        assertFalse(saveAndWaitForUpdate(node));
        check("z");
    }

    // Test for managing a existing multi value property
    @Test
    public void testPropertyInitializationExistingMultiSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {"L"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"L"});
    }

    @Test
    public void testPropertyInitializationExistingMultiSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {"M", "N"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"M", "N"});
    }

    @Test
    public void testPropertyInitializationExistingMultiSetNone() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {});
    }

    @Test
    public void testPropertyInitializationExistingMultiAddSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropadd", new String[] {"O"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"x", "y", "O"});
    }

    @Test
    public void testPropertyInitializationExistingMultiAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropadd", new String[] {"P", "Q"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"x", "y", "P", "Q"});
    }

    @Test
    public void testPropertyInitializationExistingMultiSetAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty("hippo:contentroot", "/test/propnode/hippo:multi");
        node.setProperty("hippo:contentpropset", new String[] {"R", "S"});
        node.setProperty("hippo:contentpropadd", new String[] {"T", "U"});
        node.setProperty("hippo:process", true);
        assertTrue(saveAndWaitForUpdate(node));
        check(new String[] {"R", "S", "T", "U"});
    }
}
