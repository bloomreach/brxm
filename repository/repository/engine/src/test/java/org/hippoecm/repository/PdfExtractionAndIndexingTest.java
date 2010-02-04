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

import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.QueryResult;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

public class PdfExtractionAndIndexingTest extends TestCase {
   
    public final static String NT_SEARCHDOCUMENT = "hippo:testsearchdocument";
    public static final String NT_COMPOUNDSTRUCTURE = "hippo:testcompoundstructure";
    private final static String TEST_PATH = "test";
    private Node testPath;

    
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        testPath = session.getRootNode().addNode(TEST_PATH);
        session.save();
    }

    
    @Test
    public void testHippoTextBinary() throws Exception {
        
        createDocumentWithPdf("docWithHippoText", true);
        
        createDocumentWithPdf("docWithoutHippoText", false);
        
        // we search on QueryManager, which only contains docWithHippoText
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'QueryManager')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        /*
         * Note that the jcr:data contains a complete different binary, that does not contain QueryManager (is not even a pdf).
         * Because we also store a hippo:text binary, this is the binary that gets indexed.   
         */ 
        
        assertTrue(nodes.getSize() == 1L);
        assertTrue(nodes.nextNode().getName().equals("docWithHippoText"));
       
        // now we search on 'uniquestringabcd': both documents contain this string in their jcr:data, but, the docWithHippoText 
        // should index the hippo:text, hence, we don't expect to find the docWithHippoText now:
        
        xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'uniquestringabcd')]";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L);
        assertTrue(nodes.nextNode().getName().equals("docWithoutHippoText"));
        
    }
 
    /*
     * We store a dummy text 'The quick brown fox jumps over the lazy abuniquestringcd' as default binary. We also index an extracted pdf text version
     * in hippo:text. When includeHippoText = true, we expect that not 'The quick brown fox jumps over the lazy' is indexed, but
     * the extracted pdf text, and thus. When includeHippoText = false, you can find the document for example by searching on
     * 
     * 'uniquestringabcd'. 
     * 
     * When it is true, you can find the doc on 
     * 
     * 'QueryManager' because this word exists in the jsr170-1.0.pdf
     */
    private void createDocumentWithPdf(String name, boolean includeHippoText) throws Exception {
        
        Node handle = testPath.addNode(name, HippoNodeType.NT_HANDLE);
        Node document = handle.addNode(name, NT_SEARCHDOCUMENT);
        
        Node compound =  document.addNode("substructure", NT_COMPOUNDSTRUCTURE);
        Node resource = compound.addNode("hippo:testresource", "hippo:testtextresource");
        
        {
            resource.setProperty("jcr:encoding", "UTF-8");
            resource.setProperty("jcr:mimeType", "text/plain");
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(data, "UTF-8");
            writer.write("The quick brown fox jumps over the lazy uniquestringabcd");
            writer.close();
            resource.setProperty("jcr:data", new ByteArrayInputStream(data.toByteArray()));
            resource.setProperty("jcr:lastModified", Calendar.getInstance());
        }
        
        if(includeHippoText) {
            InputStream pdf = this.getClass().getResourceAsStream("jsr170-1.0.pdf");
            try {
                PDFParser parser = new PDFParser(new BufferedInputStream(pdf));
                PDDocument pdDocument = null;
                try {
                    parser.parse();
                    pdDocument = parser.getPDDocument();
                    CharArrayWriter writer = new CharArrayWriter();
    
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setLineSeparator("\n");
                    stripper.writeText(pdDocument, writer);
                     
                    StringBuilder extracted = new StringBuilder();
                    extracted.append(writer.toCharArray());
                    // make sure to store it as UTF-8
                    InputStream extractedStream = IOUtils.toInputStream(extracted.toString() , "UTF-8"); 
                    resource.setProperty("hippo:text", extractedStream);
                } finally {
                    try {
                        if (pdDocument != null) {
                            pdDocument.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                // it may happen that PDFParser throws a runtime
                // exception when parsing certain pdf documents
                
                // we set empty extracted text:
                InputStream extractedStream = IOUtils.toInputStream("" , "UTF-8"); 
                resource.setProperty("hippo:text", extractedStream);
                
            } finally {
                pdf.close();
            }
        }
        testPath.save();
    }
    
}
