/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.io.File;
import java.net.URL;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.InitializationProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import static org.apache.jackrabbit.JcrConstants.JCR_MIMETYPE;
import static org.apache.jackrabbit.JcrConstants.NT_FILE;
import static org.apache.jackrabbit.JcrConstants.NT_FOLDER;
import static org.apache.jackrabbit.JcrConstants.NT_RESOURCE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENT;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTDELETE;
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

public class InitializationProcessorTest extends RepositoryTestCase {

    private Node test;
    private Node item;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        test = session.getRootNode().addNode("test");
        item = session.getRootNode().addNode("hippo:configuration/hippo:initialize/testnode", "hipposys:initializeitem");
        item.setProperty(HIPPO_STATUS, "pending");
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        removeNode("/hippo:configuration/hippo:initialize/testnode");
        super.tearDown();
    }

    private void checkPropNode(String expected) throws RepositoryException {
        Node node = session.getNode("/test/propnode");
        assertFalse(node.hasProperty("hippo:multi"));
        assertTrue(node.hasProperty("hippo:single"));
        assertEquals(expected, node.getProperty("hippo:single").getString());
    }

    private void checkPropNode(String[] expected) throws RepositoryException {
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

    private void process(Logger logger) {
        InitializationProcessor processor = new InitializationProcessorImpl(logger);
        processor.processInitializeItems(session);
    }

    @Test
    public void testAddContentFromNode() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        item.setProperty(HIPPO_CONTENT, "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" sv:name=\"testnode\"><sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\"><sv:value>nt:unstructured</sv:value></sv:property></sv:node>");
        session.save();
        process(null);
        assertTrue(session.nodeExists("/test/testnode"));
        assertEquals("done", item.getProperty(HIPPO_STATUS).getString());
    }

    @Test
    public void testContentDelete() throws Exception {
        final Node delete = test.addNode("delete");
        item.setProperty(HIPPO_CONTENTDELETE, delete.getPath());
        session.save();
        process(null);
        assertFalse(session.nodeExists("/test/delete"));
    }

    @Test
    public void testContentDeleteFailsOnSNS() throws Exception {
        final Node sns = test.addNode("sns");
        test.addNode("sns");
        item.setProperty(HIPPO_CONTENTDELETE, sns.getPath());
        session.save();
        process(new NOPLogger() {});
        assertEquals(2, test.getNodes("sns").getSize());
    }

    @Test
    public void testPropertyDelete() throws Exception {
        final Property delete = test.setProperty("delete", "");
        item.setProperty(HippoNodeType.HIPPO_CONTENTPROPDELETE, delete.getPath());
        session.save();
        process(null);
        assertFalse(session.propertyExists("/test/delete"));
    }

    @Test
    public void testPropSetNoParent() throws Exception {
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"a"});
        session.save();
        process(new NOPLogger() {});
        assertFalse(session.propertyExists("/test/propnode/hippo:single"));
    }

    @Test
    public void testPropSetSingle() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:single");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"b"});
        session.save();
        process(null);
        checkPropNode("b");
    }

    @Test
    public void testPropSetMulti() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit");
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        item.setProperty(HIPPO_CONTENTPROPSET, new String[]{"m", "n"});
        session.save();
        process(null);
        checkPropNode(new String[]{"m", "n"});
    }

    @Test
    public void testPropAdd() throws Exception {
        session.getRootNode().getNode("test").addNode("propnode", "hippo:testproplvlinit").setProperty("hippo:multi", new String[] { "m", "n" });
        item.setProperty(HIPPO_CONTENTROOT, "/test/propnode/hippo:multi");
        item.setProperty(HIPPO_CONTENTPROPADD, new String[] { "o", "p" });
        session.save();
        process(null);
        checkPropNode(new String[]{"m", "n", "o", "p"});
    }

    @Test
    public void testContentFolderInitialization() throws Exception {
        item.setProperty(HIPPO_CONTENTFOLDER, "resources");
        item.setProperty(HIPPO_CONTENTROOT, "/test");
        item.setProperty(HIPPO_EXTENSIONSOURCE, getClass().getResource("/hippo.jar").toString() + "!/hippoecm-extension.xml");
        session.save();
        process(null);
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
        process(null);

        assertTrue(test.hasNode("resources"));
        resources = test.getNode("resources");
        assertFalse(created.equals(resources.getProperty(JCR_CREATED).getDate()));
    }


    /*
     * REPO-969: It works fine when the file: URL is on non-Windows system,
     *           but it throws "IllegalArgumentException: URI is not hierarchical"
     *           if the file: URL denotes a Windows URL like 'file:C:/a/b/c/...'.
     */
    @Test
    public void testGetBaseZipFileFromURL() throws Exception {
        URL url = new URL("file:/a/b/c.jar!/d/e/f.xml");
        assertEquals("/a/b/c.jar!/d/e/f.xml", url.getFile());
        File baseFile = new InitializationProcessorImpl().getBaseZipFileFromURL(url);
        assertTrue(baseFile.getPath().endsWith("c.jar"));

        url = new URL("file:C:/a/b/c.jar!/d/e/f.xml");
        assertEquals("C:/a/b/c.jar!/d/e/f.xml", url.getFile());
        baseFile = new InitializationProcessorImpl().getBaseZipFileFromURL(url);
        assertTrue(baseFile.getPath().endsWith("c.jar"));
    }

}
