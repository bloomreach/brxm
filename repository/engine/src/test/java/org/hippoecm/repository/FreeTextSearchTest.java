/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.io.OutputStreamWriter;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_NAMED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FreeTextSearchTest extends RepositoryTestCase {

    public static final String DOCUMENT_TITLE_PART = "foo";
    public static final String COMPOUNDDOCUMENT_TITLE_PART = "bar";
    public static final String  HTML_CONTENT_PART = "lux";
    public static final String  BINARY_CONTENT_PART = "dog";
    private static final String DOCUMENT_DISPLAYNAME = "xyzyx";

    private String[] defaultContent = new String[] {
            "/test", "nt:unstructured",
                "/test/Document1", "hippo:handle",
                "jcr:mixinTypes", "mix:referenceable",
                    "/test/Document1/Document1", "hippo:testsearchdocument",
                    "jcr:mixinTypes", "mix:referenceable",
                    "title", "This is the title of document 1 containing " + DOCUMENT_TITLE_PART,
                        "/test/Document1/Document1/compoundchild",    "hippo:testcompoundstructure",
                        "compoundtitle", "This is the compoundtitle containing " + COMPOUNDDOCUMENT_TITLE_PART,
                            "/test/Document1/Document1/compoundchild/hippo:testhtml",    "hippo:testhtml",
                            "hippo:testcontent", "The content property of testhtml node containing " + HTML_CONTENT_PART
    };


    
    private void createContent(String[]... contents) throws Exception {
        if (contents == null) {
            throw new IllegalArgumentException("no bootstrap content");
        }

        for (String[] content : contents) {
            build(content, session);
        }

        // extra set a binary property
        Node compound = session.getNode("/test/Document1/Document1/compoundchild");
        Node resource = compound.addNode("hippo:testresource", "hippo:resource");
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(data, "UTF-8");
        writer.write("The quick brown fox jumps over the lazy " + BINARY_CONTENT_PART);
        writer.close();
        resource.setProperty("jcr:data", new ByteArrayInputStream(data.toByteArray()));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        // set display name
        Node handle = session.getNode("/test/Document1");
        handle.addMixin(NT_NAMED);
        handle.setProperty(HIPPO_NAME, DOCUMENT_DISPLAYNAME);

        session.save();
        flushIndex(session.getRepository());
    }

    @Test
    public void testSimpleFreeTextSearch() throws Exception {

        createContent(defaultContent);
        
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+DOCUMENT_TITLE_PART+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
 
    /**
     * test where we search on a String property in a child node and we expect the parent node, Document1 
     * to find, which is a parent of the node containing the property
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testFirstLevelChildNodeFreeTextSearch() throws Exception {

        createContent(defaultContent);
        
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+COMPOUNDDOCUMENT_TITLE_PART+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
    
    /**
     * test where we search on a String property in a child node and we expect the parent's parent node, Document1 
     * to find, which is a parent of the node containing the property
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testSecondLevelChildNodeFreeTextSearch() throws Exception {

        createContent(defaultContent);
        
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+HTML_CONTENT_PART+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
    
    /**
     * test where we search on a binary property in a child node and we expect the parent's parent node, Document1 
     * to find, which is a parent of the node containing the property
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testSecondChildNodeBinaryFreeTextSearch() throws Exception {

        createContent(defaultContent);
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+BINARY_CONTENT_PART+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
        
    }
    
    /**
     * This test is to prove we require the FieldNames.AGGREGATED_NODE_UUID logic in ServicingSearchIndex
     * When we search on the text property of a direct child node below the hippo:document, we should
     * not find the hippo:document anymore when the child node is removed. This test is to assure that, 
     * when a direct child node is removed, the hippo:document is reindexed in the repository. 
     * 
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testDeleteFirstLevelChildNode() throws Exception {

        createContent(defaultContent);
        
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+COMPOUNDDOCUMENT_TITLE_PART+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        assertTrue(nodes.nextNode().getName().equals("Document1"));
        
        Node n = session.getNode("/test/Document1/Document1");
        n.getNode("compoundchild").remove();
        n.getSession().save();
        
        flushIndex(session.getRepository());
        
        xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+COMPOUNDDOCUMENT_TITLE_PART+"')] order by @jcr:score descending";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        nodes = queryResult.getNodes();
        // we have removed the deeper child node that had the 'HTML_CONTENT_PART' term, hence, we should not
        // get a hit anymore
        assertEquals(0L, nodes.getSize());
        
    }
    
    /**
     * This test is to prove we require the FieldNames.AGGREGATED_NODE_UUID logic in ServicingSearchIndex
     * When we search on the text property of some child node at some level below the hippo:document, we should
     * not find the hippo:document anymore when the child node is removed. This test is to assure that, 
     * when a deeper located child node is removed, the hippo:document must is in the repository. 
     * 
     * @throws RepositoryException
     * @throws IOException 
     */
    @Test
    public void testDeleteSecondLevelChildNode() throws Exception {

        createContent(defaultContent);
        
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+HTML_CONTENT_PART+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        Node doc = nodes.nextNode();
        assertTrue(doc.getName().equals("Document1"));
        
        Node n = session.getNode("/test/Document1/Document1/compoundchild");
        n.getNode("hippo:testhtml").remove();

        n.getSession().save();
        
        flushIndex(session.getRepository());
        
        xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+HTML_CONTENT_PART+"')] order by @jcr:score descending";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        nodes = queryResult.getNodes();
        // we have removed the deeper child node that had the 'HTML_CONTENT_PART' term, hence, we should not
        // get a hit anymore
        assertEquals(0L, nodes.getSize());
        
    }
   
    
    @Test
    public void testAddFirstLevelChildNode() throws Exception {
        String word = "addedcompound";
        String[] extraChildNodeContent = new String[] {
                "/test/Document1/Document1/compoundchild",  "hippo:testcompoundstructure",
                "compoundtitle", "This is the compoundtitle containing " + word
        };

        createContent(defaultContent, extraChildNodeContent);

        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+word+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }

    @Test
    public void testAddSecondLevelChildNode() throws Exception {
        String word = "addedhtmlnode";

        String[] extraSecondLevelChildNodeContent = new String[] {
                "/test/Document1/Document1/compoundchild/hippo:html2",  "hippo:testhtml",
                "hippo:testcontent", "The content property of testhtml node containing " + word
        };

        createContent(defaultContent, extraSecondLevelChildNodeContent);

        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+word+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }

    /**
     * This test is to prove {@code }org.hippoecm.repository.query.lucene.ServicingSearchIndex#augmentDocumentsToUpdate} is
     * really required. Namely a newly added node won't trigger a document reindex through org.apache.jackrabbit.core.query.lucene.FieldNames.AGGREGATED_NODE_UUID
     */
    @Test
    public void testAddSecondLevelChildNode_to_existing_document() throws Exception {
        String word = "addedhtmlnode";
        createContent(defaultContent);

        session.save();
        flushIndex(session.getRepository());
        {
            String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'" + word + "')] order by @jcr:score descending";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertEquals(0L, nodes.getSize());
        }
        final Node html = session.getNode("/test/Document1/Document1/compoundchild").addNode("hippo:html2", "hippo:testhtml");
        html.setProperty("hippo:testcontent", "The content property of testhtml node containing " + word);
        session.save();
        flushIndex(session.getRepository());

        {
            String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'" + word + "')] order by @jcr:score descending";
            QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            NodeIterator nodes = queryResult.getNodes();
            assertEquals(1L, nodes.getSize());
            while (nodes.hasNext()) {
                Node doc = nodes.nextNode();
                assertTrue(doc.getName().equals("Document1"));
            }
        }
    }
    
    @Test
    public void testModifyFirstLevelChildNode() throws Exception {
        createContent(defaultContent);
        Node n = session.getNode("/test/Document1/Document1");
       
        Node compound = session.getNode("/test/Document1/Document1/compoundchild");
        String word = "changedcompound";
        compound.setProperty("compoundtitle", "This is now the new compoundtitle containing " + word);
        n.getSession().save();
        
        flushIndex(session.getRepository());
        
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+word+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }

    @Test
    public void testModifySecondLevelChildNode() throws Exception {
        createContent(defaultContent);
        Node n = session.getNode("/test/Document1/Document1");
       
        Node html = session.getNode("/test/Document1/Document1/compoundchild/hippo:testhtml");
        String word = "changedtesthtml";
        html.setProperty("hippo:testcontent", "The content property of testhtml node now containing " + word);

        n.getSession().save();
        
        flushIndex(session.getRepository());
        
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+word+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }

    /**
     * hippo:name on handle must be indexed on document level
     */
    @Test
    public void test_displayname_SearchOnDocumentName() throws Exception {
        createContent(defaultContent);

        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+ DOCUMENT_DISPLAYNAME +"')] order by @jcr:score descending";
        final QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        final NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        final Node node = nodes.nextNode();
        assertEquals("/test/Document1/Document1", node.getPath());
    }

    @Test
    public void test_displayname_RemoveName_andAddName_UpdatesDocumentIndex() throws Exception {
        createContent(defaultContent);
        final Node handle = session.getNode("/test/Document1");
        handle.removeMixin(NT_NAMED);
        session.save();
        flushIndex(session.getRepository());

        {
            String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'" + DOCUMENT_DISPLAYNAME + "')] order by @jcr:score descending";
            final QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            final NodeIterator nodes = queryResult.getNodes();
            assertEquals(0L, nodes.getSize());
        }

        // again adding the display name should result in the document being found by DOCUMENT_DISPLAYNAME
        handle.addMixin(NT_NAMED);
        handle.setProperty(HIPPO_NAME, DOCUMENT_DISPLAYNAME);
        session.save();
        flushIndex(session.getRepository());
        {
            String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+ DOCUMENT_DISPLAYNAME +"')] order by @jcr:score descending";
            final QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
            final NodeIterator nodes = queryResult.getNodes();
            assertEquals(1L, nodes.getSize());
        }
    }

    @Test
    public void testCharacterReferenceRemoval() throws Exception {
        String word = "abc&nbsp;xyz&#x32;AT&T&gt;klm&agrave;";

        String[] extraSecondLevelChildNodeContent = new String[] {
                "/test/Document1/Document1/compoundchild/hippo:html2",  "hippo:testhtml",
                "hippo:testcontent", word
        };

        createContent(defaultContent, extraSecondLevelChildNodeContent);

        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'abc AND xyz AND AT&T AND klm')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        while(nodes.hasNext()) {
            Node doc = nodes.nextNode();
            assertTrue(doc.getName().equals("Document1"));
        }
    }

    public static void flushIndex(Repository repository) {
        try {
            repository = org.hippoecm.repository.impl.RepositoryDecorator.unwrap(repository);
            QueryHandler queryHandler = ((RepositoryImpl)repository).getSearchManager("default").getQueryHandler();
            ((SearchIndex)queryHandler).flush();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RepositoryException ex) {
        }
    }
}
