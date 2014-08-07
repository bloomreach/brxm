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

import java.util.Arrays;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.impl.InitializationProcessorImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.helpers.NOPLogger;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import static org.apache.jackrabbit.JcrConstants.JCR_MIMETYPE;
import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.jackrabbit.JcrConstants.NT_FOLDER;
import static org.apache.jackrabbit.JcrConstants.NT_RESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTFOLDER;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPADD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPSET;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTROOT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_EXTENSIONSOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELOADONSTARTUP;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_STATUS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest extends RepositoryTestCase {

    private Node test;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        test = session.getRootNode().addNode("test");
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:initialize/testnode");
        super.tearDown();
    }

    private void check(String expected) throws RepositoryException {
        Node node = session.getNode("/test/propnode");
        assertFalse(node.hasProperty("hippo:multi"));
        assertTrue(node.hasProperty("hippo:single"));
        assertEquals(expected, node.getProperty("hippo:single").getString());
    }

    private void check(String[] expected) throws RepositoryException {
        Node node = session.getNode("/test/propnode");
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
        node.setProperty(HIPPO_CONTENTROOT, "/test");
        node.setProperty("hippo:content", "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" sv:name=\"testnode\"><sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property></sv:node>");
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        assertTrue(session.getRootNode().getNode("test").hasNode("testnode"));
        assertEquals("done", node.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testPropertyInitializationNoParent() throws Exception {
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"a"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        assertEquals("pending", node.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testPropertyInitializationNewSingleSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"b"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check("b");
    }

    @Test
    public void testPropertyInitializationNewSingleSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"c", "d"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        assertEquals("pending", node.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testPropertyInitializationNewSingleSetNone() throws Exception {
        Node target = session.getNode("/test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getNode("/hippo:configuration/hippo:initialize").addNode("testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session, Arrays.asList(node));
        assertFalse(target.hasProperty("hippo:single"));
    }

    @Test
    public void testPropertyInitializationNewSingleAddSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"e"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        assertEquals("pending", node.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testPropertyInitializationNewSingleAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"f", "g"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        assertEquals("pending", node.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testPropertyInitializationNewSingleSetAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"h", "i"});
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"j", "k"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        assertEquals("pending", node.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testPropertyInitializationNewMultiSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"l"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        check(new String[] {"l"});
    }

    @Test
    public void testPropertyInitializationNewMultiSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"m", "n"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[] {"m", "n"});
    }

    @Test
    public void testPropertyInitializationNewMultiSetAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"r", "s"});
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"t", "u"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[] {"r", "s", "t", "u"});
    }

    // Test for managing a existing single value property
    @Test
    public void testPropertyInitializationExistingSingleSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"B"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check("B");
    }

    @Test
    public void testPropertyInitializationExistingSingleSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"C", "D"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        check("z");
    }

    @Test
    public void testPropertyInitializationExistingSingleSetNone() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        assertFalse(session.getRootNode().getNode("test/propnode").hasProperty("hippo:single"));
        assertFalse(session.getRootNode().getNode("test/propnode").hasProperty("hippo:multi"));
    }

    @Test
    public void testPropertyInitializationExistingSingleAddSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"E"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        check("z");
    }

    @Test
    public void testPropertyInitializationExistingSingleAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"F", "G"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        check("z");
    }

    @Test
    public void testPropertyInitializationExistingSingleSetAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:single", "z");
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"H", "I"});
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"J", "K"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        // expecting error output: set noplogger
        InitializationProcessor processor = new InitializationProcessorImpl(new NOPLogger() {});
        processor.processInitializeItems(session);
        check("z");
    }

    // Test for managing a existing multi value property
    @Test
    public void testPropertyInitializationExistingMultiSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"L"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[] {"L"});
    }

    @Test
    public void testPropertyInitializationExistingMultiSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"M", "N"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[] {"M", "N"});
    }

    @Test
    public void testPropertyInitializationExistingMultiSetNone() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[] {});
    }

    @Test
    public void testPropertyInitializationExistingMultiAddSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"O"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[] {"x", "y", "O"});
    }

    @Test
    public void testPropertyInitializationExistingMultiAddMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"P", "Q"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[] {"x", "y", "P", "Q"});
    }

    @Test
    public void testPropertyInitializationExistingMultiSetAddMulti() throws Exception {
        session.getNode("/test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] {"x", "y"});
        Node node = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        node.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        node.setProperty(HIPPO_CONTENTPROPSET, new String[] {"R", "S"});
        node.setProperty(HIPPO_CONTENTPROPADD, new String[] {"T", "U"});
        node.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);
        check(new String[]{"R", "S", "T", "U"});
    }

    @Test
    public void testContentFolderInitialization() throws Exception {
        Node item = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        item.setProperty(HIPPO_CONTENTFOLDER, "resources");
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        item.setProperty(HIPPO_EXTENSIONSOURCE, getClass().getResource("/hippo.jar").toString() + "!/hippoecm-extension.xml");
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();
        InitializationProcessor processor = new InitializationProcessorImpl();
        processor.processInitializeItems(session);

//        session.exportSystemView("/test", System.out, false, false);

        assertTrue(test.hasNode("resources"));
        Node resources = test.getNode("resources");
        Calendar created = resources.getProperty(JCR_CREATED).getDate();
        assertTrue(resources.isNodeType(NT_FOLDER));
        assertTrue(resources.hasNode("images"));
        Node images = resources.getNode("images");
        assertTrue(images.isNodeType(NT_FOLDER));
        Node javascript = resources.getNode("javascript");
        assertTrue(javascript.isNodeType(NT_FOLDER));
        assertTrue(images.hasNode("hippo.png"));
        Node png = images.getNode("hippo.png");
        assertTrue(png.isNodeType(NT_FILE));
        Node content = png.getNode(JCR_CONTENT);
        assertTrue(content.isNodeType(NT_RESOURCE));
        assertTrue(content.hasProperty(JCR_MIMETYPE));
        assertTrue(content.hasProperty(JCR_DATA));
        assertEquals("image/png", content.getProperty(JCR_MIMETYPE).getString());

        // test reload
        item.setProperty(HIPPO_RELOADONSTARTUP, true);
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();
        processor.processInitializeItems(session);

        assertTrue(test.hasNode("resources"));
        resources = test.getNode("resources");
        assertFalse(created.equals(resources.getProperty(JCR_CREATED).getDate()));
    }

}
