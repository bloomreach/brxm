/*
 *  Copyright 2010 Hippo.
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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;

public class StandardHippoAnalyzerTest extends TestCase {
    
    public final static String NT_SEARCHDOCUMENT = "hippo:testsearchdocument";
    public static final String NT_COMPOUNDSTRUCTURE = "hippo:testcompoundstructure";
    public static final String NT_HTML = "hippo:testhtml";
    

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

    private void createCompoundStructure(String sentence) throws Exception {
        Node handle = testPath.addNode("Document1", HippoNodeType.NT_HANDLE);
        Node document = handle.addNode("Document1", NT_SEARCHDOCUMENT);
        document.setProperty("title", sentence);
        
        testPath.save();
    }
    
 
    @Test
    public void testStopWords() throws Exception {
        
        createCompoundStructure("The quick brown fox jumps over the lazy dog");
        
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'quick')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
        // search for a stopword 'over': as it is a stopword, we should not find a hit for it:
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'over')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 0L); 
        }
        
    }
    
    @Test
    public void testDiacritics() throws Exception {
        String sentence = "Hygiëne is for françois in his privéleven as important as Fußball";
        createCompoundStructure(sentence);
        
     // search with diacritics Hygiëne
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'hygiëne')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
        // search without diacritics hygiene
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'hygiene')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
     // search with diacritics françois
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'françois')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
        // search without diacritics francois
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'francois')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
     // search with diacritics privéleven
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'privéleven')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
        // search without diacritics priveleven
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'priveleven')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
     // search with diacritics Fußball
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'Fußball')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
        // search without diacritics fussball (ß --> ss )
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'fussball')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == 1L); 
            while(nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
        
    }
}
