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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EnhancedExportImportTest extends RepositoryTestCase {

    Node testData;
    Node testExport;
    Node testImport;

    int uuidBehavior = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

    // nodes that have to be cleaned up
    private static final String TEST_DATA_NODE = "testdata";
    private static final String TEST_EXPORT_NODE = "testexport";
    private static final String TEST_IMPORT_NODE = "testimport";

    public void cleanup() throws RepositoryException  {
        session.refresh(false);
        if (session.getRootNode().hasNode(TEST_DATA_NODE)) {
            session.getRootNode().getNode(TEST_DATA_NODE).remove();
        }
        if (session.getRootNode().hasNode(TEST_IMPORT_NODE)) {
            session.getRootNode().getNode(TEST_IMPORT_NODE).remove();
        }
        session.save();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // create test data
        testData = session.getRootNode().addNode(TEST_DATA_NODE);
        testData.addNode("ref1",  "hippo:ntunstructured").addMixin("mix:referenceable");

        testExport = testData.addNode(TEST_EXPORT_NODE,  "hippo:ntunstructured");
        testExport.addNode("doc1",  "hippo:ntunstructured").addMixin("mix:referenceable");
        testExport.addNode("doc2",  "hippo:ntunstructured").addMixin("mix:referenceable");
        testExport.addNode("doc3",  "hippo:ntunstructured").addMixin("mix:referenceable");
        testExport.addNode("doc4",  "hippo:ntunstructured").addMixin("mix:referenceable");
        testExport.addNode("doc5",  "hippo:document").addMixin("hippo:derived");

        // references between doc1 and doc2
        Value uuidDoc1 = session.getValueFactory().createValue(testExport.getNode("doc1").getIdentifier(), PropertyType.REFERENCE);
        Value uuidDoc2 = session.getValueFactory().createValue(testExport.getNode("doc2").getIdentifier(), PropertyType.REFERENCE);
        Value uuidDoc3 = session.getValueFactory().createValue(testExport.getNode("doc3").getIdentifier(), PropertyType.REFERENCE);
        Value uuidRef1 = session.getValueFactory().createValue(testData.getNode("ref1").getIdentifier(), PropertyType.REFERENCE);

        // one single
        testExport.getNode("doc1").setProperty("ref-to-2", uuidDoc2);

        // two single
        testExport.getNode("doc2").setProperty("ref-to-1", uuidDoc1);
        testExport.getNode("doc2").setProperty("ref-to-3", uuidDoc3);

        // one multi
        testExport.getNode("doc3").setProperty("ref-to-12", new Value[]{uuidDoc1, uuidDoc2});

        // one external
        testExport.getNode("doc4").setProperty("ref-to-extern", uuidRef1);

        // empty multi
        testExport.getNode("doc5").setProperty("hippo:related", new Value[]{});

        // create import node
        testImport = testData.addNode(TEST_IMPORT_NODE,  "hippo:ntunstructured");

        // expose data to user session
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        cleanup();
        super.tearDown();
    }

    @Test
    public void testExport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);

        //prettyPrint(out.toByteArray(), System.out);
        // TODO check the contents of the export
    }

    @Test
    public void testNormalImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);
        session.save();
        assertTrue("Import node not found", testImport.hasNode(TEST_EXPORT_NODE));
    }

    @Test
    public void testNodeEncoding() throws RepositoryException, IOException {
        testExport.addNode("node with space",  "hippo:ntunstructured");
        testExport.addNode("node_x0020_with_x0020_encoded",  "hippo:ntunstructured");
        testExport.save();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);
        session.save();
        assertTrue("Import node not found", testImport.hasNode(TEST_EXPORT_NODE));

        Node node = testImport.getNode(TEST_EXPORT_NODE);
        assertTrue("Import of node with spaces failed", node.hasNode("node with space"));
        assertTrue("Import of node with encoded spaces failed", node.hasNode("node_x0020_with_x0020_encoded"));
    }

    @Test
    public void testPropertyEncoding() throws RepositoryException, IOException {
        Node encode = testExport.addNode("encode_propery_test",  "hippo:ntunstructured");
        encode.setProperty("value_with_space", "my value");
        encode.setProperty("value_with_xml", "</sv:value></sv:property></sv:node>");
        encode.setProperty("value_with_encoded_space", "my_x0020_value");
        encode.setProperty("property with space", "dummy");
        encode.setProperty("property_with_xml_<xml>", "dummy");
        encode.setProperty("property_x0020_with_x0020_encoded space", "dummy");

        testExport.save();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);
        session.save();
        assertTrue("Import node not found", testImport.hasNode(TEST_EXPORT_NODE));

        Node node = testImport.getNode(TEST_EXPORT_NODE);
        assertTrue("Import of property encoding test node failed", node.hasNode("encode_propery_test"));

        encode = node.getNode("encode_propery_test");
        assertTrue("Property value with space not found", encode.hasProperty("value_with_space"));
        assertTrue("Property value with encoded space not found", encode.hasProperty("value_with_encoded_space"));
        assertTrue("Property with space not found", encode.hasProperty("property with space"));
        assertTrue("Property with space not found", encode.hasProperty("property_with_xml_<xml>"));
        assertTrue("Property with encoded space not found", encode
                .hasProperty("property_x0020_with_x0020_encoded space"));

        assertEquals("my value", encode.getProperty("value_with_space").getString());
        assertEquals("my_x0020_value", encode.getProperty("value_with_encoded_space").getString());
        assertEquals("dummy", encode.getProperty("property with space").getString());
        assertEquals("dummy", encode.getProperty("property_with_xml_<xml>").getString());
        assertEquals("dummy", encode.getProperty("property_x0020_with_x0020_encoded space").getString());
    }


    @Test
    public void testReferenceFailImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        // remove referenced node
        testData.getNode("ref1").remove();

        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW;
        try {
            ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);
            fail("Import should fail.");
        } catch (RepositoryException e) {
            // expected
        }
        assertFalse(testImport.hasNode(TEST_EXPORT_NODE));
    }

    @Test
    public void testReferenceSkipImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        // remove referenced node
        testData.getNode("ref1").remove();

        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);

        Node node = testImport.getNode(TEST_EXPORT_NODE);
        assertNotNull(node);
        assertFalse(node.getNode("doc4").hasProperty("ref-to-extern"));
    }

    @Test
    public void testReferenceRootImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        // remove referenced node
        testData.getNode("ref1").remove();
        session.getRootNode().addMixin("mix:referenceable");

        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_TO_ROOT;
        ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);

        Node node = testImport.getNode(TEST_EXPORT_NODE);
        assertNotNull(node);
        assertEquals(session.getRootNode().getIdentifier(), node.getNode("doc4").getProperty("ref-to-extern").getString());
    }

    @Test
    public void testMergeAddImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());

        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);
        session.save();

        out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        in = new ByteArrayInputStream(out.toByteArray());
        referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        ((HippoSession) session).importEnhancedSystemViewXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, null);

        assertTrue(testImport.hasNode(TEST_EXPORT_NODE));
        assertEquals(2, testImport.getNodes(TEST_EXPORT_NODE).getSize());
        assertEquals(5L, testImport.getNode(TEST_EXPORT_NODE).getNodes().getSize());
    }
}
