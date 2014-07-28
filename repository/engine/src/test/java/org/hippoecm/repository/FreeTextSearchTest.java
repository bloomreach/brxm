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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FreeTextSearchTest extends RepositoryTestCase {

    public static final String DOCUMENT_TITLE_PART = "foo";
    public static final String COMPOUNDDOCUMENT_TITLE_PART = "bar";
    public static final String  HTML_CONTENT_PART = "lux";
    public static final String  BINARY_CONTENT_PART = "dog";
    private static final String TRANSLATED_DOCUMENT_NAME = "xyzyx";

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

        // set translation node
        Node handle = session.getNode("/test/Document1");
        handle.addMixin("hippo:translated");
        final Node translation = handle.addNode("hippo:translation", "hippo:translation");
        translation.setProperty("hippo:language", "en");
        translation.setProperty("hippo:message", TRANSLATED_DOCUMENT_NAME);

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
     * All hippo:message properties of hippo:translation nodes under a handle are included
     * in the fulltext index of the document.
     */
    @Test
    public void testSearchOnTranslatedDocumentName() throws Exception {
        createContent(defaultContent);

        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+TRANSLATED_DOCUMENT_NAME+"')] order by @jcr:score descending";
        final QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        final NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        final Node node = nodes.nextNode();
        assertEquals("/test/Document1/Document1", node.getPath());
    }

    /**
     * When the hippo:translation node under a hippo:handle is removed the document's index is updated
     */
    @Test
    public void testRemoveTranslationUpdatesDocumentIndex() throws Exception {
        createContent(defaultContent);
        session.getNode("/test/Document1/hippo:translation").remove();
        session.save();
        flushIndex(session.getRepository());

        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+TRANSLATED_DOCUMENT_NAME+"')] order by @jcr:score descending";
        final QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        final NodeIterator nodes = queryResult.getNodes();
        assertEquals(0L, nodes.getSize());
    }

    @Test
    public void testSkipNodeForIndexing() throws Exception {
        String[] extraSkipIndexDocument = new String[] {
                "/test/Document2",             "hippo:handle",
                "jcr:mixinTypes",             "mix:referenceable",
                    "/test/Document2/Document2",        "hippo:testsearchdocument",
                    "jcr:mixinTypes",                   "mix:referenceable,hippo:skipindex",
                    "title",                            "This is the title of document 2 containing " + DOCUMENT_TITLE_PART,
                        "/test/Document2/Document2/compoundchild",    "hippo:testcompoundstructure",
                        "compoundtitle", "This is the compoundtitle containing " + COMPOUNDDOCUMENT_TITLE_PART,
                            "/test/Document2/Document2/compoundchild/hippo:testhtml",    "hippo:testhtml",
                            "hippo:testcontent", "The content property of testhtml node containing " + HTML_CONTENT_PART
        };
        createContent(defaultContent, extraSkipIndexDocument);

        // since the Document2 has mixin hippo:skipindex we should not find that document and only find Document1
        String xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+DOCUMENT_TITLE_PART+"')] order by @jcr:score descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();

        NodeIterator nodes = queryResult.getNodes();
        assertEquals(1L, nodes.getSize());
        Node doc = nodes.nextNode();
        assertTrue(doc.getName().equals("Document1"));


        // and we should not find nodes ***BELOW*** Document2 either:
        xpath = "/jcr:root/test/Document2/Document2//* order by @jcr:score descending";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertEquals(0L, queryResult.getNodes().getSize());

        // after removing the mixin 'hippo:skipindex', the node should be found again.
        session.getNode("/test/Document2/Document2").removeMixin("hippo:skipindex");
        session.save();

        xpath = "//element(*,hippo:testsearchdocument)[jcr:contains(.,'"+DOCUMENT_TITLE_PART+"')] order by @jcr:score descending";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertEquals(2L, queryResult.getNodes().getSize());

        // below is tricky : since the child nodes below Document2 are not changed *AFTER* the skipindex mixin was removed,
        // they are still not indexed!!
        xpath = "/jcr:root/test/Document2/Document2//* order by @jcr:score descending";
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertEquals(0L, queryResult.getNodes().getSize());

        // after touching and saving the child nodes below Document2, they get indexed
        session.getNode("/test/Document2/Document2/compoundchild").setProperty("compoundtitle", "foo");
        session.getNode("/test/Document2/Document2/compoundchild/hippo:testhtml").setProperty("hippo:testcontent", "foo");
        session.save();
        queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        assertEquals(2L, queryResult.getNodes().getSize());

    }


    public static void flushIndex(Repository repository) {
        try {
            repository = org.hippoecm.repository.decorating.RepositoryDecorator.unwrap(repository);
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
