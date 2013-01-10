/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.jackrabbit.value.BinaryImpl;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;

public class PdfExtractionAndIndexingTest extends RepositoryTestCase {
   
    public static final String NT_SEARCHDOCUMENT = "hippo:testsearchdocument";
    public static final String NT_COMPOUNDSTRUCTURE = "hippo:testcompoundstructure";
    private static final String TEST_PATH = "test";
    private static final String UNIQUE_WORD_IN_PLAIN_TEXT = "uniquestringabcd";
    private static final String UNIQUE_WORD_IN_UNNITTEST_PDF = "foobarlux";
    private static final String UNITTEST_PDF_FILE_NAME = "unittest.pdf";
    
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
        
        FreeTextSearchTest.flushIndex(testPath.getSession().getRepository());
        
        // we search on 'UNIQUE_WORD_IN_UNNITTEST_PDF', which is only contained by docWithHippoText
        String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+UNIQUE_WORD_IN_UNNITTEST_PDF+"')] order by @jcr:score descending";
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
        
        xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+UNIQUE_WORD_IN_PLAIN_TEXT+"')] order by @jcr:score descending";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        nodes = queryResult.getNodes();
        assertTrue(nodes.getSize() == 1L);
        assertTrue(nodes.nextNode().getName().equals("docWithoutHippoText"));
        
    }


    @Test
    public void testEmptyHippoTextBinary() throws Exception {
        // We first create a real PDF document, containing some words. We should be able to find the pdf
        // document with those words.
        createDocumentWithPdfAndHippoTextBinary("docWithHippoTextBinary", false);

        FreeTextSearchTest.flushIndex(testPath.getSession().getRepository());

        // we search on 'UNIQUE_WORD_IN_UNNITTEST_PDF',
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+UNIQUE_WORD_IN_UNNITTEST_PDF+"')] order by @jcr:score descending";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();

            assertTrue(nodes.getSize() == 1L);
            assertTrue(nodes.nextNode().getName().equals("docWithHippoTextBinary"));
        }
        // since the hippo:text is really used, we should find the unique text in the pdf

        // NOW, we store the same pdf again, this time with an EMPTY hippo text binary as well. Then,
        // we should *not* find the document

        createDocumentWithPdfAndHippoTextBinary("docWithHippoEMPTYTextBinary", true);

        FreeTextSearchTest.flushIndex(testPath.getSession().getRepository());

        // since the hippo:text is set to empty binary, we should NOT find the unique text in the pdf. Thus, we should
        // still only find "docWithHippoTextBinary"

        // we search on 'UNIQUE_WORD_IN_UNNITTEST_PDF',
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+UNIQUE_WORD_IN_UNNITTEST_PDF+"')] order by @jcr:score descending";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();

            assertTrue(nodes.getSize() == 1L);
            assertTrue(nodes.nextNode().getName().equals("docWithHippoTextBinary"));
        }
    }


    /*
     * We store a dummy text 'The quick brown fox jumps over the lazy +UNIQUE_WORD' as default binary. We also index an extracted pdf text version
     * in hippo:text. When includeHippoText = true, we expect that not 'The quick brown fox jumps over the lazy' is indexed, but
     * the extracted pdf text, and thus. When includeHippoText = false, you can find the document for example by searching on
     * 
     * 'UNIQUE_WORD'. 
     * 
     * When it is true, you can find the doc on 
     * 
     * 'UNIQUE_WORD_IN_UNNITTEST_PDF' because this word exists in the PDF_FILE_NAME
     */
    private void createDocumentWithPdf(String name, boolean includeHippoText) throws Exception {
        
        Node handle = testPath.addNode(name, HippoNodeType.NT_HANDLE);
        Node document = handle.addNode(name, NT_SEARCHDOCUMENT);
        
        Node compound =  document.addNode("substructure", NT_COMPOUNDSTRUCTURE);
        Node resource = compound.addNode("hippo:testresource", "hippo:resource");
        
        {
            resource.setProperty("jcr:encoding", "UTF-8");
            resource.setProperty("jcr:mimeType", "text/plain");
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(data, "UTF-8");
            writer.write("The quick brown fox jumps over the lazy "+UNIQUE_WORD_IN_PLAIN_TEXT);
            writer.close();
            resource.setProperty("jcr:data", new BinaryImpl(new ByteArrayInputStream(data.toByteArray())));
            resource.setProperty("jcr:lastModified", Calendar.getInstance());
        }
        
        if(includeHippoText) {
            InputStream pdf = this.getClass().getResourceAsStream(UNITTEST_PDF_FILE_NAME);
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
                    resource.setProperty("hippo:text", resource.getSession().getValueFactory().createBinary(extractedStream));
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
                
                // we set empty text:
                final ByteArrayInputStream emptyByteArrayInputStream = new ByteArrayInputStream(new byte[0]);
                resource.setProperty("hippo:text", resource.getSession().getValueFactory().createBinary(emptyByteArrayInputStream));
                
            } finally {
                pdf.close();
            }
        }
        testPath.save();
    }

    private void createDocumentWithPdfAndHippoTextBinary(String name, boolean setHippoBinaryEmpty) throws Exception {

        Node handle = testPath.addNode(name, HippoNodeType.NT_HANDLE);
        Node document = handle.addNode(name, NT_SEARCHDOCUMENT);

        Node compound =  document.addNode("substructure", NT_COMPOUNDSTRUCTURE);
        Node resource = compound.addNode("hippo:testresource", "hippo:resource");

        {
            resource.setProperty("jcr:encoding", "UTF-8");
            resource.setProperty("jcr:mimeType", "text/plain");
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(data, "UTF-8");
            writer.write("The quick brown fox jumps over the lazy "+UNIQUE_WORD_IN_PLAIN_TEXT);
            writer.close();
            resource.setProperty("jcr:data", new BinaryImpl(new ByteArrayInputStream(data.toByteArray())));
            resource.setProperty("jcr:lastModified", Calendar.getInstance());
        }

        if(setHippoBinaryEmpty) {
           final ByteArrayInputStream emptyByteArrayInputStream = new ByteArrayInputStream(new byte[0]);
           resource.setProperty("hippo:text", resource.getSession().getValueFactory().createBinary(emptyByteArrayInputStream));
        } else  {
            InputStream pdf = this.getClass().getResourceAsStream(UNITTEST_PDF_FILE_NAME);
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
                    resource.setProperty("hippo:text", resource.getSession().getValueFactory().createBinary(extractedStream));
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

                // we set empty text:
                final ByteArrayInputStream emptyByteArrayInputStream = new ByteArrayInputStream(new byte[0]);
                resource.setProperty("hippo:text", resource.getSession().getValueFactory().createBinary(emptyByteArrayInputStream));

            } finally {
                pdf.close();
            }
        }
        testPath.getSession().save();
    }
    
}
