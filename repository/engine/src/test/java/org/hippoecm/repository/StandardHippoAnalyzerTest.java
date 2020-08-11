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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StandardHippoAnalyzerTest extends RepositoryTestCase {
    
    public static final String NT_SEARCHDOCUMENT = "hippo:testsearchdocument";
    public static final String NT_COMPOUNDSTRUCTURE = "hippo:testcompoundstructure";
    public static final String NT_HTML = "hippo:testhtml";
    

    private static final String TEST_PATH = "test";
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
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while (nodes.hasNext());
        }
        
        // search for a stopword 'over': as it is a stopword, we should not find a hit for it:
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'over')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 0L);
            assertFalse(nodes.hasNext());
        }
        
    }
    
    @Test
    public void testDiacritics() throws Exception {
       /*
        * ë = \u00EB
        * ç = \u00E7
        * é = \u00E9
        * ß = \u00DF
        */
        
        
        String sentence = "Hygi\u00EBne is for fran\u00E7ois in his priv\u00E9leven as important as Fu\u00DFball";
        createCompoundStructure(sentence);
        
     // search with diacritics Hygiëne
       {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'hygi\u00EBne')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while (nodes.hasNext());
        }
        
        // search without diacritics hygiene
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'hygiene')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while(nodes.hasNext());
        }
        
     // search with diacritics françois
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'fran\u00E7ois')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while(nodes.hasNext());
        }
        
        // search without diacritics francois
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'francois')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while(nodes.hasNext());

        }
        
     // search with diacritics privéleven
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'priv\u00E9leven')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while(nodes.hasNext());
        }
        
        // search without diacritics priveleven
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'priveleven')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while(nodes.hasNext());
        }
        
     // search with diacritics Fußball
        {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'Fu\u00DFball')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while(nodes.hasNext());
        }
        
        // search without diacritics fussball (ß --> ss )
         {
            String xpath = "//element(*,"+NT_SEARCHDOCUMENT+")[jcr:contains(.,'fussball')]";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertTrue(nodes.getSize() == -1 || nodes.getSize() == 1L);
            assertTrue(nodes.hasNext());
            do {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            } while(nodes.hasNext());
        }
        
    }
}
