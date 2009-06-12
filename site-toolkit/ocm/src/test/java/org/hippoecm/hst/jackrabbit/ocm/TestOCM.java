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
package org.hippoecm.hst.jackrabbit.ocm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.digester.DigesterMapperImpl;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.hippoecm.hst.jackrabbit.ocm.manager.cache.NOOPObjectCache;
import org.hippoecm.hst.jackrabbit.ocm.manager.impl.HstAnnotationMapperImpl;
import org.hippoecm.hst.jackrabbit.ocm.manager.impl.HstObjectConverterImpl;
import org.hippoecm.hst.jackrabbit.ocm.query.impl.HstQueryManagerImpl;
import org.hippoecm.hst.test.AbstractOCMSpringTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOCM extends AbstractOCMSpringTestCase {
    
    protected Object repository;
    protected Credentials defaultCredentials;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.repository = getComponent(Repository.class.getName());
        this.defaultCredentials = getComponent(Credentials.class.getName());
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
        if (this.repository != null) {
            MethodUtils.invokeMethod(this.repository, "close", null);
        }
    }

    @Test
    public void testComponentConfiguration() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(ComponentConfiguration.class);
        Mapper mapper = new HstAnnotationMapperImpl(classes, "hippo:document");
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        // Normal JCR Node path
        ComponentConfiguration compConfig = (ComponentConfiguration) ocm.getObject("/hst:testconfiguration/hst:configuration/hst:pages/newsoverview");
        assertNotNull(compConfig);
        
        System.out.println("path: " + compConfig.getPath());
        System.out.println("content base path: " + compConfig.getComponentContentBasePath());
        System.out.println("class name: " + compConfig.getComponentClassName());
        System.out.println("render path: " + compConfig.getRenderPath());

        // hippo:facetselect JCR Node path
        compConfig = (ComponentConfiguration) ocm.getObject("/testpreview/testproject/hst:configuration/hst:configuration/hst:pages/newsoverview");
        assertNotNull(compConfig);
        
        System.out.println("path: " + compConfig.getPath());
        System.out.println("content base path: " + compConfig.getComponentContentBasePath());
        System.out.println("class name: " + compConfig.getComponentClassName());
        System.out.println("render path: " + compConfig.getRenderPath());
        
        session.logout();
    }
    
    @Test
    public void testTextPage1() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(TextPage1.class);
        classes.add(HippoStdHtml.class);
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdFolder.class);
        Mapper mapper = new HstAnnotationMapperImpl(classes, "hippo:document");
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        TextPage1 productsPage = (TextPage1) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        assertNotNull(productsPage.getHtml());
        assertNotNull(productsPage.getHtml().getContent());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        System.out.println("html: " + productsPage.getHtml().getContent());

        classes = new ArrayList<Class>();
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdFolder.class);
        mapper = new HstAnnotationMapperImpl(classes, "hippo:document");
        ocm = createObjectContentManager(session, mapper);
        
        HippoStdDocument productsPageDoc = (HippoStdDocument) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPageDoc);
        assertNotNull(productsPageDoc.getNode());

        System.out.println("productsPageDoc's node: " + productsPageDoc.getNode());
        System.out.println("productsPageDoc's path: " + productsPageDoc.getPath());
        System.out.println("productsPageDoc's stateSummary: " + productsPageDoc.getStateSummary());
        System.out.println("productsPageDoc's state: " + productsPageDoc.getState());
        
        HippoStdFolder parentCollection = productsPageDoc.getFolder();
        System.out.println("parentCollection: " + parentCollection);
        System.out.println("parentCollection.path: " + parentCollection.getPath());
        assertNotNull(parentCollection);
        
        List<HippoStdDocument> childDocs = parentCollection.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        System.out.println("childDocs: " + childDocs);
        for (HippoStdDocument childDoc : childDocs) {
            System.out.println("childDoc: " + childDoc);
        }
        
        session.logout();
    }
    
    @Test
    public void testTextPage2() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(TextPage2.class);
        classes.add(HippoStdHtml.class);
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdFolder.class);
        Mapper mapper = new HstAnnotationMapperImpl(classes, "hippo:document");
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        TextPage2 productsPage = (TextPage2) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        assertNotNull(productsPage.getUuid());
        //assertNotNull(productsPage.getComments());
        //assertFalse(productsPage.getComments().isEmpty());

        System.out.println("uuid: " + productsPage.getUuid());
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        
        List<TextPageComment> comments = productsPage.getComments();
        if (comments != null) {
            for (TextPageComment comment : comments) {
                System.out.println("comment: " + comment.getComment());
            }
        }
    }

    @Test
    public void testCollection() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(TextPage1.class);
        classes.add(HippoStdHtml.class);
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdFolder.class);
        Mapper mapper = new HstAnnotationMapperImpl(classes, "hippo:document");
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        TextPage1 productsPage = (TextPage1) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        
        // Normal JCR Node path
        HippoStdFolder coll = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject");
        assertNotNull(coll);
        assertNotNull(coll.getNode());
        
        System.out.println("node: " + coll.getNode());
        System.out.println("path: " + coll.getPath());
        
        List<HippoStdFolder> childColl = coll.getFolders();
        assertNotNull(childColl);
        assertFalse(childColl.isEmpty());
        
        System.out.println("childColl: " + childColl);
        
        for (HippoStdFolder childCollItem : childColl) {
            System.out.println("childCollItem: " + childCollItem.getName() + ", " + childCollItem.getPath());
        }
        
        HippoStdFolder productsColl = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject/Products");
        
        List<HippoStdDocument> childDocs = productsColl.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        
        System.out.println("childDocs: " + childDocs);
        
        for (HippoStdDocument childDoc : childDocs) {
            System.out.println("childDoc: " + childDoc.getName() + ", " + childDoc.getPath() + ", " + childDoc.getState() + ", " + childDoc.getStateSummary());
        }

        session.logout();
    }
    
    @Test
    public void testCollectionWithDigesterMapper() throws Exception {
        Mapper mapper = new DigesterMapperImpl(getClass().getResourceAsStream("jackrabbit-ocm-descriptor.xml"));
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        TextPage1 productsPage = (TextPage1) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        
        // Normal JCR Node path
        HippoStdFolder coll = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject");
        assertNotNull(coll);
        assertNotNull(coll.getNode());
        
        System.out.println("node: " + coll.getNode());
        System.out.println("path: " + coll.getPath());
        
        List<HippoStdFolder> childColl = coll.getFolders();
        assertNotNull(childColl);
        assertFalse(childColl.isEmpty());
        
        System.out.println("childColl: " + childColl);
        
        for (HippoStdFolder childCollItem : childColl) {
            System.out.println("childCollItem: " + childCollItem.getName() + ", " + childCollItem.getPath());
        }
        
        HippoStdFolder productsColl = (HippoStdFolder) ocm.getObject("/testcontent/documents/testproject/Products");
        
        List<HippoStdDocument> childDocs = productsColl.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        
        System.out.println("childDocs: " + childDocs);
        
        for (HippoStdDocument childDoc : childDocs) {
            System.out.println("childDoc: " + childDoc.getName() + ", " + childDoc.getPath() + ", " + childDoc.getState() + ", " + childDoc.getStateSummary());
        }

        session.logout();
    }
    
    @Test
    public void testQueryManager() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(TextPage1.class);
        classes.add(HippoStdHtml.class);
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdFolder.class);
        Mapper mapper = new HstAnnotationMapperImpl(classes, "hippo:document");
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        // search collection
        QueryManager qm = ocm.getQueryManager();
        Filter filter = qm.createFilter(HippoStdDocument.class);
        filter.setScope("/testcontent/documents/testproject//");
        Query query = qm.createQuery(filter);
        Collection result = ocm.getObjects(query);
        System.out.println("result: " + result);
        assertFalse(result.isEmpty());

        // search document by HippoStdDocument filter
        filter = qm.createFilter(HippoStdDocument.class);
        filter.setScope("/testcontent/documents/testproject/Products/SomeProduct//");
        query = qm.createQuery(filter);
        HippoStdDocument doc = (HippoStdDocument) ocm.getObject(query);
        System.out.println("doc: " + doc);
        assertNotNull(doc);
        assertTrue(doc instanceof TextPage1);
        
        // search document by TextPage class filter with title filter.
        // because the class is TextPage, we can use title filter here.
        filter = qm.createFilter(TextPage1.class);
        filter.setScope("/testcontent/documents/testproject/Products/SomeProduct//");
        filter.addEqualTo("title", "Products");
        query = qm.createQuery(filter);
        TextPage1 page = (TextPage1) ocm.getObject(query);
        System.out.println("page: " + page);
        assertNotNull(page);
        assertTrue(page instanceof TextPage1);
        
        session.logout();
    }
    
    @Test
    public void testTextPageUpdate() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(TextPage1.class);
        classes.add(HippoStdHtml.class);
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdFolder.class);
        Mapper mapper = new HstAnnotationMapperImpl(classes, "hippo:document");
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        TextPage1 productsPage = (TextPage1) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());
        
        String oldTitle = productsPage.getTitle();
        
        // Now updating...
        productsPage.setTitle("Hey, Dude!");
        ocm.update(productsPage);
        ocm.save();
        
        // Now validating the changes from the repository...
        TextPage1 productsPageUpdated = (TextPage1) ocm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull(productsPageUpdated);
        assertNotNull(productsPageUpdated.getNode());
        assertEquals("Hey, Dude!", productsPageUpdated.getTitle());
        
        System.out.println("node: " + productsPageUpdated.getNode());
        System.out.println("path: " + productsPageUpdated.getPath());
        System.out.println("title: " + productsPageUpdated.getTitle());
        System.out.println("stateSummary: " + productsPageUpdated.getStateSummary());
        System.out.println("state: " + productsPageUpdated.getState());
        
        // Restores the changes back...
        productsPageUpdated.setTitle(oldTitle);
        ocm.update(productsPageUpdated);
        ocm.save();
        
        session.logout();
    }
    
    private ObjectContentManager createObjectContentManager(Session session, Mapper mapper) throws UnsupportedRepositoryOperationException, RepositoryException {
        ObjectContentManager ocm = null;
        
        DefaultAtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
        Map atomicTypeConverters = converterProvider.getAtomicTypeConverters();
        QueryManager queryManager = new HstQueryManagerImpl(mapper, atomicTypeConverters, session.getValueFactory());
        ProxyManager proxyManager = new ProxyManagerImpl();
        ObjectCache requestObjectCache = new NOOPObjectCache();
        ObjectConverter objectConverter = new HstObjectConverterImpl(mapper, converterProvider, proxyManager, requestObjectCache);
        ocm = new ObjectContentManagerImpl(mapper, objectConverter, queryManager, requestObjectCache, session);
        
        return ocm;
    }
}
