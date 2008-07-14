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
package org.hippoecm.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DereferencedExportImportTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
        
        // references between doc1 and doc2
        Value uuidDoc1 = session.getValueFactory().createValue(testExport.getNode("doc1").getUUID(), PropertyType.REFERENCE);
        Value uuidDoc2 = session.getValueFactory().createValue(testExport.getNode("doc2").getUUID(), PropertyType.REFERENCE);
        Value uuidDoc3 = session.getValueFactory().createValue(testExport.getNode("doc3").getUUID(), PropertyType.REFERENCE);
        Value uuidRef1 = session.getValueFactory().createValue(testData.getNode("ref1").getUUID(), PropertyType.REFERENCE);

        // one single
        testExport.getNode("doc1").setProperty("ref-to-2", uuidDoc2);
        
        // two single
        testExport.getNode("doc2").setProperty("ref-to-1", uuidDoc1);
        testExport.getNode("doc2").setProperty("ref-to-3", uuidDoc3);
        
        // one multi
        testExport.getNode("doc3").setProperty("ref-to-12", new Value[]{uuidDoc1, uuidDoc2});

        // one external
        testExport.getNode("doc4").setProperty("ref-to-extern", uuidRef1);

        // create import node
        testImport = testData.addNode(TEST_IMPORT_NODE,  "hippo:ntunstructured");
        
        // expose data to user session
        session.save();
    }

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
        int mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_OVERWRITE;
        ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
        session.save();
        Node node = testImport.getNode(TEST_EXPORT_NODE);
        assertNotNull(node);
    }

    @Test
    public void testReferenceFailImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        
        // remove referenced node
        testData.getNode("ref1").remove();
        
        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW;
        int mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_OVERWRITE;
        try {
            ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
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
        int mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_OVERWRITE;
        ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
        
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
        int mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_OVERWRITE;
        ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
        
        Node node = testImport.getNode(TEST_EXPORT_NODE);
        assertNotNull(node);
        assertEquals(session.getRootNode().getUUID(), node.getNode("doc4").getProperty("ref-to-extern").getString());
    }

    
    @Test
    public void testMergeFailImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        
        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        int mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_OVERWRITE;
        ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
        session.save();
        
        try {
            referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
            mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_THROW;
            ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
            fail("Import should fail.");
        } catch (RepositoryException e) {
            // expected
        }
    }
    
    @Test
    public void testMergeAddImport() throws RepositoryException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        
        int referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        int mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_OVERWRITE;
        ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
        session.save();
        
        out = new ByteArrayOutputStream();
        ((HippoSession) session).exportDereferencedView(testExport.getPath(), out, false, false);
        in = new ByteArrayInputStream(out.toByteArray());
        referenceBehavior = ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE;
        mergeBehavior = ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP;
        ((HippoSession) session).importDereferencedXML(testImport.getPath(), in, uuidBehavior, referenceBehavior, mergeBehavior);
        
        assertTrue(testImport.hasNode(TEST_EXPORT_NODE));
        assertEquals(8L, testImport.getNode(TEST_EXPORT_NODE).getNodes().getSize());
        
    }
    
    private void prettyPrint(byte[] bytes, OutputStream out) throws Exception {
        Source source = new StreamSource(new ByteArrayInputStream(bytes));
        DOMResult result = new DOMResult();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer identityTransformer = transformerFactory.newTransformer();
        identityTransformer.transform(source, result);
        org.w3c.dom.Document doc = (org.w3c.dom.Document) result.getNode();

        OutputFormat format = new OutputFormat(doc);
        format.setEncoding("UTF-8");
        format.setIndenting(true);
        format.setIndent(2);
        format.setLineWidth(80);

        XMLSerializer xmlSerializer = new XMLSerializer(out, format);
        xmlSerializer.serialize(doc);
    }
}
