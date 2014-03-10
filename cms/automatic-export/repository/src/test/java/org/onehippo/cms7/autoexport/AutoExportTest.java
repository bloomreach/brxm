/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.ElementQualifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import junit.framework.Assert;
import static org.custommonkey.xmlunit.DifferenceConstants.ATTR_SEQUENCE_ID;
import static org.custommonkey.xmlunit.DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID;
import static org.custommonkey.xmlunit.DifferenceConstants.COMMENT_VALUE_ID;
import static org.custommonkey.xmlunit.DifferenceConstants.TEXT_VALUE_ID;
import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.api.HippoNodeType.INITIALIZE_PATH;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_ENABLED_PROPERTY_NAME;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_NODE_PATH;

/**
 * Test for {@link AutoExportModule}
 */
@Ignore
public class AutoExportTest extends RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger("org.onehippo.cms7.autoexport.test");
    private static final long TEN_SECONDS = 10*1000;
    
    private String testHome;
    private String projectBase;

    // /export-test
    private Node testRoot;

    @Before
    @Override
    public void setUp() throws Exception {
        // Where are we?
        final String resource = AutoExportTest.class.getResource("/autoexporttest/simple/hippoecm-extension.xml").getFile();
        int idx = resource.indexOf("/target/test-classes/autoexporttest/simple/hippoecm-extension.xml");
        String moduleHome = resource.substring(0, idx);

        testHome = URLDecoder.decode(moduleHome + "/target/test-classes/autoexporttest", "UTF-8");
        projectBase = URLDecoder.decode(moduleHome + "/target/autoexporttest", "UTF-8");

        assertTrue(new File(testHome).exists());

        // remove results from previous invocation
        // if we do this on teardown we can't inspect
        // results manually
        File projectBaseDir = new File(this.projectBase);
        if (projectBaseDir.exists()) {
            log.debug("deleting project base dir: " + projectBaseDir.getPath());
            FileUtils.deleteDirectory(projectBaseDir);
        }
        System.setProperty("project.basedir", this.projectBase);

        // startup the repository
        super.setUp(true);

        // remove imported nodes
        Node root = session.getNode("/");
        for (NodeIterator iter = root.getNodes("et:*"); iter.hasNext();) {
            Node node = iter.nextNode();
            log.debug("removing node " + node.getPath());
            node.remove();
        }
        for (NodeIterator iter = root.getNode(CONFIGURATION_PATH).getNode(INITIALIZE_PATH).getNodes("et-*"); iter.hasNext();) {
            Node node = iter.nextNode();
            log.debug("removing node " + node.getPath());
            node.remove();
        }
        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        try {
            manager.unregisterNodeType("et:example");
        } catch (NoSuchNodeTypeException e) {
        } 
        try {
            manager.unregisterNodeType("et:example2");
        } catch (NoSuchNodeTypeException e) {
        }

        session.save();
        testRoot = session.getRootNode();

    }

    @Override
    @After
    public void tearDown() throws Exception {
        System.clearProperty("project.basedir");
        super.tearDown();
    }

    @Test
    public void testAddNode() throws Exception {
        String[] content = { "/et:simple", "et:node" };
        build(session, content);
        session.save();
        waitForAutoExport();
        checkExportedFiles("simple");
        assertTrue(hasInitializeItemNode("et-simple"));
    }

    @Test
    public void testAddDeepNode() throws Exception {
        String[] content = {
                "/et:foo", "et:node",
                "/et:foo/et:foo", "et:node",
                "/et:foo/et:foo/et:foo", "et:node"
        };
        build(session, content);
        session.save();
        waitForAutoExport();
        checkExportedFiles("deep");
        assertTrue(hasInitializeItemNode("et-foo"));
        assertTrue(hasInitializeItemNode("et-foo-et-foo-et-foo"));
    }
    
    @Test
    public void testAddAndRemoveNode() throws Exception {
        // case: add and remove a child node
        Node node0 = testRoot.addNode("et:simple", "et:node");
        Node node1 = node0.addNode("et:simple", "et:node");
        session.save();
        waitForAutoExport();
        node1.remove();
        session.save();
        waitForAutoExport();
        checkExportedFiles("simple");
        // case: add and remove a context node
        // (should add and remove content resource instruction)
        Node node2 = testRoot.addNode("et:simple2", "et:node");
        session.save();
        waitForAutoExport();
        node2.remove();
        session.save();
        waitForAutoExport();
        // result should be the same
        checkExportedFiles("simple");
    }
    
    @Test
    public void testMoveNode() throws Exception {
        // create /export-test/et:tobemoved and persist 
        testRoot.addNode("et:tobemoved", "et:node");
        session.save();
        waitForAutoExport();
        // move
        session.move("/et:tobemoved", "/et:simple");
        session.save();
        waitForAutoExport();
        checkExportedFiles("simple");
        assertTrue(!hasInitializeItemNode("et-tobemoved"));
    }
    
    @Test
    public void testMoveDeepNode() throws Exception {
        Node node = testRoot.addNode("et:bar", "et:node");
        Node sub = node.addNode("et:foo", "et:node");
        sub.addNode("et:foo", "et:node");
        session.save();
        waitForAutoExport();
        // move
        session.move("/et:bar", "/et:foo");
        session.save();
        waitForAutoExport();
        checkExportedFiles("deep");
    }
    
//    @Test
//    public void testMoveDeepOverRemovedNode() throws Exception {
//        Node node = testRoot.addNode("et:foo", "et:node");
//        Node sub = node.addNode("et:foo", "et:node");
//        sub.addNode("et:foo", "et:node");
//        node = testRoot.addNode("et:bar", "et:node");
//        sub = node.addNode("et:foo", "et:node");
//        sub.addNode("et:foo", "et:node");
//        super.session.save();
//        Thread.sleep(TEN_SECONDS);
//        // move
//        super.session.removeItem("/et:foo");
//        super.session.move("/et:bar", "/et:foo");
//        super.session.save();
//        Thread.sleep(TEN_SECONDS);
//        checkExportedFiles("deep");
//    }

    @Test
    public void testAddNamespace() throws Exception {
        NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
        registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.1");
        // we need to add and remove a node here in order for the jcr event listener to be called
        Node node = testRoot.addNode("et:simple", "et:node");
        session.save();
        node.remove();
        session.save();
        waitForAutoExport();
        checkExportedFiles("namespace");
        assertTrue(hasInitializeItemNode("etx"));
    }

    @Test
    public void testAddandUpdateNamespace() throws Exception {
        NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
        registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.0");
        // we need to add and remove a node here in order for the jcr event listener to be called
        Node node = testRoot.addNode("et:simple", "et:node");
        session.save();
        waitForAutoExport();
        
        // if we register a namespace that is an updated version of
        // a previously registered namespace then the instruction
        // for that previously registered namespace must be removed
        // and an updated namespace instruction must be added
        
        // get the old one out of the way
        ((NamespaceRegistryImpl)registry).externalRemap("etx", "etx_old", "http://hippoecm.org/etx/nt/1.0");
            
        // register the new one
        registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.1");
        // just to trigger an event
        node.remove();
        super.session.save();
        waitForAutoExport();
        checkExportedFiles("namespace");
        assertTrue(hasInitializeItemNode("etx"));
    }
    
    @Test
    public void testAddNodetype() throws Exception {
        NodeTypeManager ntm = super.session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate template = ntm.createNodeTypeTemplate();
        template.setName("et:example");
        ntm.registerNodeType(template, false);
        super.session.save();
        waitForAutoExport();
        checkExportedFiles("nodetype");
    }
    
    @Test
    public void testAddNodetypes() throws Exception {
        NodeTypeManager ntm = super.session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate template = ntm.createNodeTypeTemplate();
        template.setName("et:example");
        ntm.registerNodeType(template, false);
        super.session.save();
        waitForAutoExport();
        // adding a nodetype in the same namespace should result
        // in the cnd for this namespace to be updated
        NodeTypeTemplate template2 = ntm.createNodeTypeTemplate();
        template2.setName("et:example2");
        ntm.registerNodeType(template2, false);
        super.session.save();
        waitForAutoExport();
        checkExportedFiles("nodetypes");
    }
    
    @Test
    public void testBasicDeltaXML() throws Exception {
        disableExport();
        Node node = testRoot.addNode("et:foo");
        node.setProperty("baz", "baz");
        session.save();
        enableExport();
        node.addNode("et:bar");
        node.setProperty("baz", "baz");
        node.setProperty("quz", "quz");
        session.save();
        waitForAutoExport();
        checkExportedFiles("basicdelta");
    }

    @Test
    public void testNestedDeltaXML() throws Exception {
        disableExport();
        Node node = testRoot.addNode("et:foo");
        session.save();
        enableExport();
        node.addNode("et:bar").addNode("et:baz");
        session.save();
        waitForAutoExport();
        checkExportedFiles("nesteddelta");
    }

    @Test
    public void testExcludeUuidPaths() throws Exception {
        String[] content = {
                "/et:foo", "et:node",
                "jcr:mixinTypes", "mix:referenceable",
                "/et:foo/et:foo", "et:node",
                "/et:foo/et:foo/et:foo", "et:node",
                "jcr:mixinTypes", "mix:referenceable",
        };
        build(session, content);
        session.save();
        waitForAutoExport();
        checkExportedFiles("filteruuids");
    }

    private void waitForAutoExport() throws RepositoryException {
        try {
            Thread.sleep(TEN_SECONDS);
        } catch (InterruptedException ignore) {}
    }

    private void checkExportedFiles(String testCase) throws Exception {
        Map<String, Reader> changes = new HashMap<String, Reader>();
        createReadersForExportedFiles(new File(projectBase + "/content/src/main/resources"), new File(projectBase + "/content/src/main/resources"), changes);
        Map<String, Reader> expected = new HashMap<String, Reader>();
        createReadersForExportedFiles(new File(testHome, testCase), new File(testHome, testCase), expected);
        assertEquals(expected, changes);
    }

    private void createReadersForExportedFiles(File basedir, File file, Map<String, Reader> readers) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                createReadersForExportedFiles(basedir, child, readers);
            }
        } else if (file.getName().endsWith(".xml") || file.getName().endsWith(".cnd")) {
            Reader reader = new FileReader(file);
            String path = file.getPath().substring(basedir.getPath().length()+1);
            readers.put(path, reader);
        }
    }

    private void assertEquals(Map<String, Reader> expected, Map<String, Reader> changes) throws IOException, SAXException {
        Assert.assertEquals(expected.size(), changes.size());
        for (String file : expected.keySet()) {
            log.debug("Comparing file " + file);
            Reader change = changes.get(file);
            assertNotNull(change);
            if (file.endsWith(".xml")) {
                // compare the xml
                Diff d = new Diff(expected.get(file), change);
                d.overrideDifferenceListener(new IgnoreTextDifferenceListener());
                d.overrideElementQualifier(new MyElementQualifier());
                assertTrue("File " + file + " has unexpected contents, diff:\n" + d.toString(), d.similar());
            } else if (file.endsWith(".cnd")) {
                Reader er = expected.get(file);
                try {
                    int changeChar, expectedChar = 0, index = 0;
                    do {
                        changeChar = change.read();
                        expectedChar = er.read();
                        Assert.assertEquals("File " + file + " contains unexpected character at index " + index, expectedChar, changeChar);
                    } while (changeChar != -1 && expectedChar != -1);
                } catch (IOException e) {
                    log.error("Error comparing cnd files", e);
                } finally {
                    IOUtils.closeQuietly(change);
                    IOUtils.closeQuietly(er);
                }
            }
        }
    }

    // not interested in differences in text node values
    private static class IgnoreTextDifferenceListener implements DifferenceListener {
        private boolean isIgnoredDifference(Difference difference) {
            if (difference.getId() == TEXT_VALUE_ID) {
                // ignore all differences in text nodes other than within element sv:value
                Element parentNode = (Element)difference.getTestNodeDetail().getNode().getParentNode();
                if (parentNode.getNodeName().equals("sv:value")) {
                    // also, when it is a value of a creation date property, ignore it as well
                    return ((Element)parentNode.getParentNode()).getAttribute("sv:name").equals("jcr:created");
                }
                return true;
            }
            if (difference.getId() == COMMENT_VALUE_ID) {
                return true;
            }
            if (difference.getId() == ATTR_SEQUENCE_ID) {
                return true;
            }
            if (difference.getId() == CHILD_NODELIST_SEQUENCE_ID) {
                return true;
            }
            return false;
        }

        @Override
        public int differenceFound(Difference difference) {
            if (isIgnoredDifference(difference)) {
                return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
            } else {
                log.debug("difference: " + difference);
                return RETURN_ACCEPT_DIFFERENCE;
            }
        }

        @Override
        public void skippedComparison(org.w3c.dom.Node node, org.w3c.dom.Node node1) {
        }
    }

    private static class MyElementQualifier implements ElementQualifier {
        @Override
        public boolean qualifyForComparison(Element control, Element test) {
            if (control.getNodeName().equals("sv:property") && test.getNodeName().equals("sv:property")) {
                return control.getAttribute("sv:name").equals(test.getAttribute("sv:name"));
            }
            if (control.getNodeName().equals("sv:node") && test.getNodeName().equals("sv:node")) {
                return control.getAttribute("sv:name").equals(test.getAttribute("sv:name"));
            }
            return (control.getNodeName().equals("sv:value") && test.getNodeName().equals("sv:value"));
        }
    }

    private void disableExport() {
        try {
            session.getNode(CONFIG_NODE_PATH).setProperty(CONFIG_ENABLED_PROPERTY_NAME, false);
        } catch (RepositoryException e) {
            log.error("Failed to disable export.", e);
        }
    }
    
    private void enableExport() {
        try {
            session.getNode(CONFIG_NODE_PATH).setProperty(CONFIG_ENABLED_PROPERTY_NAME, true);
        } catch (RepositoryException e) {
            log.error("Failed to disable export.", e);
        }
    }
    
    private boolean hasInitializeItemNode(String name) throws RepositoryException {
        try {
            return session.getRootNode().getNode(CONFIGURATION_PATH).getNode(INITIALIZE_PATH).hasNode(name);
        } catch (PathNotFoundException e) {
            return false;
        }
    }
}
