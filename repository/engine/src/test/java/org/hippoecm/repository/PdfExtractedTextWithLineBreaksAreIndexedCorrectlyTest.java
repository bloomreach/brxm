/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

public class PdfExtractedTextWithLineBreaksAreIndexedCorrectlyTest extends RepositoryTestCase {
   
    public static final String NT_SEARCHDOCUMENT = "hippo:testsearchdocument";
    public static final String NT_COMPOUNDSTRUCTURE = "hippo:testcompoundstructure";
    private static final String TEST_PATH = "test";

    private static final String WORDS_ON_NEW_LINE_WITHOUT_SPACES = "words_on_new_line_without_spaces.pdf";
    private static final String SPACE_LINE_SEPARATOR = " ";
    private static final String SLASH_N_LINE_SEPARATOR = "\n";
    
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
    public void testLineBreaksPdfNoDefaultLineSeparator() throws Exception {

        // no line separator means system default is used
        createDocumentWithPdf("testpdfdoc1", null);

        FreeTextSearchTest.flushIndex(testPath.getSession().getRepository());

        String[] searchWords = {"banaan", "kers", "4711", "ziekenhuis"};
        for (String word : searchWords) {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+word+"')] order by @jcr:score descending";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();

            assertTrue(nodes.getSize() == 1L);
            assertTrue(nodes.nextNode().getName().equals("testpdfdoc1"));
        }
        
        
    }

    @Test
    public void testLineBreaksPdfBackSlashNLineSeparator() throws Exception {

        // no line separator means system default is used
        createDocumentWithPdf("testpdfdoc2", SLASH_N_LINE_SEPARATOR);

        FreeTextSearchTest.flushIndex(testPath.getSession().getRepository());

        String[] searchWords = {"banaan", "kers", "4711", "ziekenhuis"};
        for (String word : searchWords) {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+word+"')] order by @jcr:score descending";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();

            assertTrue(nodes.getSize() == 1L);
            assertTrue(nodes.nextNode().getName().equals("testpdfdoc2"));
        }
    }

    @Test
    public void testLineBreaksPdfSpaceLineSeparator() throws Exception {

        // no line separator means system default is used
        createDocumentWithPdf("testpdfdoc3", SPACE_LINE_SEPARATOR);

        FreeTextSearchTest.flushIndex(testPath.getSession().getRepository());

        String[] searchWords = {"banaan", "kers", "4711", "ziekenhuis"};
        for (String word : searchWords) {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'"+word+"')] order by @jcr:score descending";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();

            assertTrue(nodes.getSize() == 1L);
            assertTrue(nodes.nextNode().getName().equals("testpdfdoc3"));
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
    private void createDocumentWithPdf(String name, String lineSeparator) throws Exception {
        
        Node handle = testPath.addNode(name, HippoNodeType.NT_HANDLE);
        Node document = handle.addNode(name, NT_SEARCHDOCUMENT);
        
        Node compound =  document.addNode("substructure", NT_COMPOUNDSTRUCTURE);
        Node resource = compound.addNode("hippo:testresource", "hippo:resource");
        
        {
            resource.setProperty("jcr:encoding", "UTF-8");
            resource.setProperty("jcr:mimeType", "application/pdf");
            InputStream pdf = this.getClass().getResourceAsStream(WORDS_ON_NEW_LINE_WITHOUT_SPACES);
            resource.setProperty("jcr:data", new BinaryImpl(new BufferedInputStream(pdf)));
            resource.setProperty("jcr:lastModified", Calendar.getInstance());
        }
        {
            InputStream pdf = this.getClass().getResourceAsStream(WORDS_ON_NEW_LINE_WITHOUT_SPACES);
            try {
                PDFParser parser = new PDFParser(new BufferedInputStream(pdf));
                PDDocument pdDocument = null;
                try {
                    parser.parse();
                    pdDocument = parser.getPDDocument();
                    CharArrayWriter writer = new CharArrayWriter();
    
                    PDFTextStripper stripper = new PDFTextStripper();
                    if (lineSeparator != null) {
                       stripper.setLineSeparator(lineSeparator);
                    }
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
