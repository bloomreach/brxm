/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.export;

import static org.custommonkey.xmlunit.DifferenceConstants.ATTR_SEQUENCE_ID;
import static org.custommonkey.xmlunit.DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID;
import static org.custommonkey.xmlunit.DifferenceConstants.COMMENT_VALUE_ID;
import static org.custommonkey.xmlunit.DifferenceConstants.TEXT_VALUE_ID;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import junit.framework.Assert;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.ElementQualifier;
import org.hippoecm.repository.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Test for {@link ExportModule}
 */
public class ExportTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: ";

    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export.test");
    private static final long SLEEP_AFTER_SAVE = 2*1000 + 500;
    
    private static final String BUILD_HOME;
    private static final String EXTENSION_HOME;
    private static final String CONFIG_HOME;
    private static final String CONTENT_HOME;

    static {
        // Where are we?
        File basedir = new File(System.getProperty("user.dir"));
        if (basedir.getName().equals("target")) {
            BUILD_HOME = basedir.getPath();
            EXTENSION_HOME = basedir.getParent() + "/src/test/resources/export/extensions";
            CONFIG_HOME = basedir.getPath() + "/config";
            CONTENT_HOME = basedir.getParent() + "/src/test/resources/export/content";
        } else {
            BUILD_HOME = basedir.getPath() + "/target";
            EXTENSION_HOME = basedir.getPath() + "/src/test/resources/export/extensions";
            CONFIG_HOME = basedir.getPath() + "/target/config";
            CONTENT_HOME = basedir.getPath() + "/src/test/resources/export/content";
        }

    }
    // /export-test
    private Node m_testRoot;

    @Before
    @Override
    public void setUp() throws Exception {
        // remove results from previous invocation
        // if we do this on teardown we can't inspect
        // results manually
        File configHome = new File(CONFIG_HOME);
        if (configHome.exists()) {
            log.debug("deleting config home: " + configHome.getPath());
            delete(configHome);
        }
        System.setProperty("hippoecm.export.dir", CONFIG_HOME);
        // startup the repository
        super.setUp(true);
        // remove imported nodes
        Node root = super.session.getNode("/");
        for (NodeIterator iter = root.getNodes("et:*"); iter.hasNext();) {
            Node node = iter.nextNode();
            log.debug("removing node " + node.getPath());
            node.remove();
        }
        for (NodeIterator iter = root.getNode("hippo:configuration/hippo:initialize").getNodes("et-*"); iter.hasNext();) {
            Node node = iter.nextNode();
            log.debug("removing node " + node.getPath());
            node.remove();
        }
        NodeTypeManager manager = super.session.getWorkspace().getNodeTypeManager();
        try {
            manager.unregisterNodeType("et:example");
        } catch (NoSuchNodeTypeException e) {
        } 
        try {
            manager.unregisterNodeType("et:example2");
        } catch (NoSuchNodeTypeException e) {
        }
//        NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
        // unregistering namespace is not supported by jackrabbit...
//            try { registry.unregisterNamespace("etx"); } catch (NamespaceException e) {}
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        m_testRoot = session.getRootNode();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        TestCase.tearDownClass(true);
        System.clearProperty("hippoecm.export.dir");
    }

    @Test
    public void testAddNode() throws Exception {
        m_testRoot.addNode("et:simple", "et:node");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("simple");
    }
    
    @Test
    public void testAddDeepNode() throws Exception {
        Node node = m_testRoot.addNode("foo", "et:node");
        Node sub = node.addNode("foo", "et:node");
        sub.addNode("foo", "et:node");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("deep");
    }
    
    @Test
    public void testAddAndRemoveNode() throws Exception {
        // case: add and remove a child node
        Node node0 = m_testRoot.addNode("et:simple", "et:node");
        Node node1 = node0.addNode("et:simple", "et:node");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        node1.remove();
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("simple");
        // case: add and remove a context node
        // (should add and remove content resource instruction)
        Node node2 = m_testRoot.addNode("et:simple2", "et:node");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        node2.remove();
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        // result should be the same
        compare("simple");
    }
    
    @Test
    public void testMoveNode() throws Exception {
        // create /export-test/et:tobemoved and persist 
        m_testRoot.addNode("et:tobemoved", "et:node");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        // move
        super.session.move("/et:tobemoved", "/et:simple");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("simple");
    }
    
    @Test
    public void testMoveDeepNode() throws Exception {
        Node node = m_testRoot.addNode("bar", "et:node");
        Node sub = node.addNode("foo", "et:node");
        sub.addNode("foo", "et:node");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        // move
        super.session.move("/bar", "/foo");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("deep");
    }
    
    /*
     * Because unregistering namespaces is not supported by jackrabbit
     * we cannot clean up and this test only works on a clean repository
     */
    @Test
    @Ignore
    public void testAddNamespace() throws Exception {
        NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
        registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.1");
        // we need to add and remove a node here in order for the jcr event listener to be called
        Node node = m_testRoot.addNode("et:simple", "et:node");
        super.session.save();
        node.remove();
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("namespace");
    }
    
    /*
     * Because unregistering namespaces is not supported by jackrabbit
     * we cannot clean up and this test only works on a clean repository
     */
    @Test
    @Ignore
    public void testAddandUpdateNamespace() throws Exception {
        NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
        registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.0");
        // we need to add and remove a node here in order for the jcr event listener to be called
        Node node = m_testRoot.addNode("et:simple", "et:node");
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        
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
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("namespace");
    }
    
    @Test
    public void testAddNodetype() throws Exception {
        NodeTypeManager ntm = super.session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate template = ntm.createNodeTypeTemplate();
        template.setName("et:example");
        ntm.registerNodeType(template, false);
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("nodetype");
    }
    
    @Test
    public void testAddNodetypes() throws Exception {
        NodeTypeManager ntm = super.session.getWorkspace().getNodeTypeManager();
        NodeTypeTemplate template = ntm.createNodeTypeTemplate();
        template.setName("et:example");
        ntm.registerNodeType(template, false);
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        // adding a nodetype in the same namespace should result
        // in the cnd for this namespace to be updated
        NodeTypeTemplate template2 = ntm.createNodeTypeTemplate();
        template2.setName("et:example2");
        ntm.registerNodeType(template2, false);
        super.session.save();
        Thread.sleep(SLEEP_AFTER_SAVE);
        compare("nodetypes");
    }

    @Test
    public void testFilterContentHandler() throws Exception {
        File contentFile = new File(CONTENT_HOME, "nested-content.xml");
        FileReader contentFileReader = new FileReader(contentFile);
        InputSource contentFileSource = new InputSource(contentFileReader);
        
        File resultFile = new File(BUILD_HOME, "nested-content-result.xml");
        FileWriter resultFileWriter = new FileWriter(resultFile);
            
        SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler handler = stf.newTransformerHandler();
        Transformer transformer = handler.getTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
        handler.setResult(new StreamResult(resultFileWriter));
                    
        List<String> excluded = Arrays.asList("/0/0.1");
        ContentResourceInstruction.FilterContentHandler filter = new ContentResourceInstruction.FilterContentHandler(handler, excluded);
        
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(filter);
        reader.parse(contentFileSource);
    }

    private void compare(String testCase) throws Exception {
        Thread.sleep(2 * 1000); // allow export to do its work
        Map<String, Reader> changes = new HashMap<String, Reader>();
        loadFiles(new File(CONFIG_HOME), new File(CONFIG_HOME), changes);
        Map<String, Reader> expected = new HashMap<String, Reader>();
        loadFiles(new File(EXTENSION_HOME, testCase), new File(EXTENSION_HOME, testCase), expected);
        assertEquals(changes, expected);
    }

    private void loadFiles(File basedir, File file, Map<String, Reader> readers) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                loadFiles(basedir, child, readers);
            }
        }
        else if (file.getName().endsWith(".xml") || file.getName().endsWith(".cnd")) {
            Reader reader = new FileReader(file);
            String path = file.getPath().substring(basedir.getPath().length()+1);
            readers.put(path, reader);
        }
    }

    private void assertEquals(Map<String, Reader> changes, Map<String, Reader> expected) throws IOException, SAXException {
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
                assertTrue("Comparing " + file, d.similar());
            } else if (file.endsWith(".cnd")) {
                Reader er = expected.get(file);
                try {
                    int changeChar, expectedChar = 0;
                    do {
                        changeChar = change.read();
                        expectedChar = er.read();
                        assertTrue(changeChar == expectedChar);
                    } while (changeChar != -1 && expectedChar != -1);
                } catch (IOException e) {
                    log.error("Error comparing cnd files", e);
                } finally {
                    try {
                        change.close();
                    } catch (IOException e) {
                    }
                    try {
                        er.close();
                    } catch (IOException e) {
                    }

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
            // TODO: sequence is only irrelevant for property nodes
            // in hippoecm-extension.xml files
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
//                        log.debug("compare " + control.getNodeName() + " with " + test.getNodeName() + "?");
            if (control.getNodeName().equals("sv:property") && test.getNodeName().equals("sv:property")) {
                return control.getAttribute("sv:name").equals(test.getAttribute("sv:name"));
            }
            if (control.getNodeName().equals("sv:node") && test.getNodeName().equals("sv:node")) {
                return control.getAttribute("sv:name").equals(test.getAttribute("sv:name"));
            }
            return (control.getNodeName().equals("sv:value") && test.getNodeName().equals("sv:value"));
        }
    }
    
    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        file.delete();
    }
}
