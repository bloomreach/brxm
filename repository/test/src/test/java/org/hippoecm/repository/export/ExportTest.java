/*
 *  Copyright 2011 Hippo (www.onehippo.com).
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
 *  under the License.
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

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.ElementQualifier;
import org.hippoecm.repository.TestCase;
import org.junit.After;
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
 * Test for org.hippoecm.repository.export.ExportModule
 */
@Ignore
public class ExportTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.export");
    private final static String BUILD_HOME;
    private final static String EXTENSION_HOME;
    private final static String CONFIG_HOME;
    private final static String CONTENT_HOME;

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
            for (File file : configHome.listFiles()) {
                file.delete();
            }
            configHome.delete();
        }
        System.setProperty("hippoecm.export.dir", CONFIG_HOME);
        // startup the repository
        super.setUp();
        // remove imported nodes
        Node root = super.session.getNode("/");
        for (NodeIterator iter = root.getNode("export-test").getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            log.debug("removing node " + node.getPath());
            node.remove();
        }
        for (NodeIterator iter = root.getNode("hippo:configuration/hippo:initialize").getNodes("et:*"); iter.hasNext();) {
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
        NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
        // unregistering namespace is not supported by jackrabbit...
//            try { registry.unregisterNamespace("etx"); } catch (NamespaceException e) {}
        super.session.save();
        m_testRoot = session.getNode("/export-test");
    }

//    @Test
//    public void testAddNode() throws Exception {
//            m_testRoot.addNode("et:simple", "et:node");
//            super.session.save();
//        compare("simple");
//    }
//  @Test
//  public void testAddAndRemoveNode() throws Exception {
//          // case: add and remove a child node
//          Node node0 = m_testRoot.addNode("et:simple", "et:node");
//          Node node1 = node0.addNode("et:simple", "et:node");
//          super.session.save();
//          node1.remove();
//          super.session.save();
//    compare("simple");
//    // case: add and remove a context node
//    // (should add and remove content resource instruction)
//    Node node2 = m_testRoot.addNode("et:simple2", "et:node");
//    super.session.save();
//    Thread.sleep(2*1000);
//    node2.remove();
//    super.session.save();
//    // result should be the same
//    compare("simple");
//  }
//  @Test
//  public void testMoveNode() throws Exception {
//          // create /export-test/et:tobemoved and persist 
//          m_testRoot.addNode("et:tobemoved", "et:node");
//          super.session.save();
//          Thread.sleep(2*1000); // allow export to finish
//          // move
//          super.session.move("/export-test/et:tobemoved", "/export-test/et:simple");
//          super.session.save();
//          compare("simple");
//  }
//    /*
//     * Because unregistering namespaces is not supported by jackrabbit
//     * we cannot clean up and this test only works on a clean repository
//     */
//    @Test
//    public void testAddNamespace() throws Exception {
//            NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
//            registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.1");
//            // we need to add and remove a node here in order for the jcr event listener to be called
//            Node node = m_testRoot.addNode("et:simple", "et:node");
//            super.session.save();
//            node.remove();
//            super.session.save();
//            compare("namespace");
//    }
//    /*
//     * Because unregistering namespaces is not supported by jackrabbit
//     * we cannot clean up and this test only works on a clean repository
//     */
//    @Test
//    public void testAddandUpdateNamespace() throws Exception {
//            NamespaceRegistry registry = super.session.getWorkspace().getNamespaceRegistry();
//            registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.0");
//            // we need to add and remove a node here in order for the jcr event listener to be called
//            Node node = m_testRoot.addNode("et:simple", "et:node");
//            super.session.save();
//    
//            // if we register a namespace that is an updated version of
//            // a previously registered namespace then the instruction
//            // for that previously registered namespace must be removed
//            // and an updated namespace instruction must be added
//            
//            // get the old one out of the way
//      ((NamespaceRegistryImpl)registry).externalRemap("etx", "etx_old", "http://hippoecm.org/etx/nt/1.0");
//            
//            // register the new one
//            registry.registerNamespace("etx", "http://hippoecm.org/etx/nt/1.1");
//            // just to trigger an event
//            node.remove();
//            super.session.save();
//            compare("namespace");
//    }
//    @Test
//    public void testAddNodetype() throws Exception {
//            NodeTypeManager ntm = super.session.getWorkspace().getNodeTypeManager();
//            NodeTypeTemplate template = ntm.createNodeTypeTemplate();
//            template.setName("et:example");
//            ntm.registerNodeType(template, false);
//            super.session.save();
//            compare("nodetype");
//    }
//    @Test
//    public void testAddNodetypes() throws Exception {
//            NodeTypeManager ntm = super.session.getWorkspace().getNodeTypeManager();
//            NodeTypeTemplate template = ntm.createNodeTypeTemplate();
//            template.setName("et:example");
//            ntm.registerNodeType(template, false);
//            super.session.save();
//            Thread.sleep(2*1000);
//            // adding a nodetype in the same namespace should result
//            // in the cnd for this namespace to be updated
//            NodeTypeTemplate template2 = ntm.createNodeTypeTemplate();
//            template2.setName("et:example2");
//            ntm.registerNodeType(template2, false);
//            super.session.save();
//            compare("nodetypes");
//    }
//    @Test public void testFilterContentHandler() throws Exception {
//            File contentFile = new File(CONTENT_HOME, "exporttest-content.xml");
//            FileReader contentFileReader = new FileReader(contentFile);
//            InputSource contentFileSource = new InputSource(contentFileReader);
//            
//            File resultFile = new File(BUILD_HOME, "exporttest-content-result.xml");
//            FileWriter resultFileWriter = new FileWriter(resultFile);
//            
//        SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
//        TransformerHandler handler = stf.newTransformerHandler();
//        Transformer transformer = handler.getTransformer();
//        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
//        handler.setResult(new StreamResult(resultFileWriter));
//            
//        List<String> excluded = Arrays.asList("/test/basedocument");
//            ContentResourceInstruction.FilterContentHandler filter = new ContentResourceInstruction.FilterContentHandler(handler, excluded);
//            
//            XMLReader reader = XMLReaderFactory.createXMLReader();
//            reader.setContentHandler(filter);
//            reader.parse(contentFileSource);
//    }
//    @Test public void testFilterContentHandler2() throws Exception {
//            File contentFile = new File(CONTENT_HOME, "nested-content.xml");
//            FileReader contentFileReader = new FileReader(contentFile);
//            InputSource contentFileSource = new InputSource(contentFileReader);
//            
//            File resultFile = new File(BUILD_HOME, "nested-content-result.xml");
//            FileWriter resultFileWriter = new FileWriter(resultFile);
//            
//        SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
//        TransformerHandler handler = stf.newTransformerHandler();
//        Transformer transformer = handler.getTransformer();
//        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
//        handler.setResult(new StreamResult(resultFileWriter));
//                    
//        List<String> excluded = Arrays.asList("/0/0.1");
//            ContentResourceInstruction.FilterContentHandler filter = new ContentResourceInstruction.FilterContentHandler(handler, excluded);
//            
//            XMLReader reader = XMLReaderFactory.createXMLReader();
//            reader.setContentHandler(filter);
//            reader.parse(contentFileSource);
//    }
    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void compare(String testCase) throws Exception {
        Thread.sleep(2 * 1000); // allow export to do its work
        Map<String, Reader> changes = loadFiles(new File(CONFIG_HOME));
        Map<String, Reader> expected = loadFiles(new File(EXTENSION_HOME, testCase));
        assertEquals(changes, expected);
    }

    private Map<String, Reader> loadFiles(File directory) throws IOException {
        Map<String, Reader> result = new HashMap<String, Reader>();
        File[] files = directory.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                Reader r = new FileReader(file);
                String name = file.getName();
                result.put(name, r);
            }
        }
        return result;
    }

    private void assertEquals(Map<String, Reader> changes, Map<String, Reader> expected) throws IOException, SAXException {
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
}
