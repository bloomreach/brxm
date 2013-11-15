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
package org.hippoecm.hst.jackrabbit.ocm;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdDocument;
import org.hippoecm.hst.jackrabbit.ocm.hippo.HippoStdFolder;
import org.hippoecm.hst.jackrabbit.ocm.util.OCMUtils;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * TestOCM
 * 
 * @version $Id$
 */
public class TestOCM extends RepositoryTestCase {
    
    private String [] fallbackHippoBeans = { "hippo:document" };
    private Class [] annotatedBeans = { TextPage.class };
    
    @Test
    public void testTextPage() throws Exception {
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        assertNotNull(productsPage.getHtml());
        assertNotNull(productsPage.getHtml().getContent());
        
        assertEquals("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct", productsPage.getPath());
        assertEquals("Products", productsPage.getTitle());
        assertEquals("new", productsPage.getStateSummary());
        assertEquals("unpublished", productsPage.getState());

        ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, null);
        
        HippoStdDocument productsPageDoc = (HippoStdDocument) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPageDoc);
        assertNotNull(productsPageDoc.getNode());

        assertEquals("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct", productsPageDoc.getPath());
        assertEquals("new", productsPageDoc.getStateSummary());
        assertEquals("unpublished", productsPageDoc.getState());

        HippoStdFolder parentCollection = productsPageDoc.getFolder();
        assertNotNull(parentCollection);
        assertEquals("/testcontent/documents/testproject/Products", parentCollection.getPath());
        
        List<HippoStdDocument> childDocs = parentCollection.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        for (HippoStdDocument childDoc : childDocs) {
            assertEquals("/testcontent/documents/testproject/Products", childDoc.getParentFolder().getPath());
        }
    }
    
    @Test
    public void testCollection() throws Exception {
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());

        assertEquals("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct", productsPage.getPath());
        assertEquals("Products", productsPage.getTitle());
        assertEquals("new", productsPage.getStateSummary());
        assertEquals("unpublished", productsPage.getState());
        
        // Normal JCR Node path
        HippoStdFolder coll = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject");
        assertNotNull(coll);
        assertNotNull(coll.getNode());
        assertEquals("/testcontent/documents/testproject", coll.getPath());

        List<HippoStdFolder> childColl = coll.getFolders();
        assertNotNull(childColl);
        assertFalse(childColl.isEmpty());

        for (HippoStdFolder childCollItem : childColl) {
            assertEquals("/testcontent/documents/testproject", childCollItem.getParentFolder().getPath());
        }
        
        HippoStdFolder productsColl = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject/Products");
        
        List<HippoStdDocument> childDocs = productsColl.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());

        for (HippoStdDocument childDoc : childDocs) {
            assertEquals("/testcontent/documents/testproject/Products", childDoc.getParentFolder().getPath());
        }
    }
    
    @Test
    public void testCollectionWithDigesterMapper() throws Exception {
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, new InputStream [] { getClass().getResourceAsStream("jackrabbit-ocm-descriptor.xml") } );
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());

        assertEquals("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct", productsPage.getPath());
        assertEquals("Products", productsPage.getTitle());
        assertEquals("new", productsPage.getStateSummary());
        assertEquals("unpublished", productsPage.getState());


        // Normal JCR Node path
        HippoStdFolder coll = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject");
        assertNotNull(coll);
        assertNotNull(coll.getNode());

        assertEquals("/testcontent/documents/testproject", coll.getPath());

        List<HippoStdFolder> childColl = coll.getFolders();
        assertNotNull(childColl);
        assertFalse(childColl.isEmpty());

        for (HippoStdFolder childCollItem : childColl) {
            assertEquals("/testcontent/documents/testproject", childCollItem.getParentFolder().getPath());
        }
        
        HippoStdFolder productsColl = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject/Products");
        
        List<HippoStdDocument> childDocs = productsColl.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());

        for (HippoStdDocument childDoc : childDocs) {
            assertEquals("/testcontent/documents/testproject/Products", childDoc.getParentFolder().getPath());
        }
    }
    
    @Test
    public void testQueryManager() throws Exception {
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        // search collection
        QueryManager qm = ocm.getQueryManager();
        Filter filter = qm.createFilter(HippoStdDocument.class);
        filter.setScope("/testcontent/documents/testproject//");
        Query query = qm.createQuery(filter);
        Collection result = ocm.getObjects(query);

        assertFalse(result.isEmpty());

        // search document by HippoStdDocument filter
        filter = qm.createFilter(HippoStdDocument.class);
        filter.setScope("/testcontent/documents/testproject/Products/SomeProduct//");
        query = qm.createQuery(filter);
        HippoStdDocument doc = (HippoStdDocument) ocm.getObject(query);
        assertNotNull(doc);
        assertTrue(doc instanceof TextPage);
        
        // search document by TextPage class filter with title filter.
        // because the class is TextPage, we can use title filter here.
        filter = qm.createFilter(TextPage.class);
        filter.setScope("/testcontent/documents/testproject/Products/SomeProduct//");
        filter.addEqualTo("title", "Products");
        query = qm.createQuery(filter);
        TextPage page = (TextPage) ocm.getObject(query);
        assertNotNull(page);
        assertTrue(page instanceof TextPage);
        
        // search document by TextPage class filter with 'contains'.
        filter = qm.createFilter(TextPage.class);
        filter.setScope("/testcontent/documents/testproject/Products//");
        filter.addContains(".", "CMS");
        query = qm.createQuery(filter);
        Collection<TextPage> textPages = (Collection<TextPage>) ocm.getObjects(query);
        assertNotNull(textPages);
        assertFalse(textPages.isEmpty());
        for (TextPage textPage : textPages) {
            assertTrue(textPage.getPath().startsWith("/testcontent/documents/testproject/Products"));
        }
    }
    
    @Test
    public void testTextPageUpdate() throws Exception {
        
        ObjectContentManager ocm = OCMUtils.createObjectContentManager(session, fallbackHippoBeans, annotatedBeans);
        
        TextPage productsPage = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());

        assertEquals("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct", productsPage.getPath());
        assertEquals("Products", productsPage.getTitle());
        assertEquals("new", productsPage.getStateSummary());
        assertEquals("unpublished", productsPage.getState());

        String oldTitle = productsPage.getTitle();
        
        // Now updating...
        productsPage.setTitle("Hey, Dude!");
        ocm.update(productsPage);
        ocm.save();
        
        // Now validating the changes from the repository...
        TextPage productsPageUpdated = (TextPage) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPageUpdated);
        assertNotNull(productsPageUpdated.getNode());
        assertEquals("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct", productsPageUpdated.getPath());
        assertEquals("Hey, Dude!", productsPageUpdated.getTitle());
        assertEquals("new", productsPageUpdated.getStateSummary());
        assertEquals("unpublished", productsPageUpdated.getState());

        
        // Restores the changes back...
        productsPageUpdated.setTitle(oldTitle);
        ocm.update(productsPageUpdated);
        ocm.save();
    }
}
