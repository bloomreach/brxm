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
package org.hippoecm.hst.ocm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.impl.DefaultAtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.cache.impl.RequestObjectCacheImpl;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.ObjectConverter;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ProxyManagerImpl;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.ocm.query.QueryManager;
import org.apache.jackrabbit.ocm.query.impl.QueryManagerImpl;
import org.hippoecm.hst.ocm.HippoStdCollection;
import org.hippoecm.hst.ocm.HippoStdDocument;
import org.hippoecm.hst.ocm.manager.impl.HstObjectConverterImpl;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOCM {
    
    protected HippoRepository repository;
    protected SimpleCredentials defaultCredentials;

    @Before
    public void setUp() throws Exception {
        this.repository = HippoRepositoryFactory.getHippoRepository("rmi://127.0.0.1:1099/hipporepository");
        this.defaultCredentials = new SimpleCredentials("admin", "admin".toCharArray());
    }
    
    @After
    public void tearDown() throws Exception {
        if (this.repository != null) {
            this.repository.close();
        }
    }

    @Test
    public void testComponentConfiguration() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(ComponentConfiguration.class);
        Mapper mapper = new AnnotationMapperImpl(classes);
        
        Session session = this.repository.login(this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        // Normal JCR Node path
        ComponentConfiguration compConfig = (ComponentConfiguration) ocm.getObject("/hst:testconfiguration/hst:configuration/hst:pages/newsoverview");
        assertNotNull(compConfig);
        
        System.out.println("path: " + compConfig.getPath());
        System.out.println("reference name: " + compConfig.getReferenceName());
        System.out.println("content base path: " + compConfig.getComponentContentBasePath());
        System.out.println("class name: " + compConfig.getComponentClassName());
        System.out.println("render path: " + compConfig.getRenderPath());

        // hippo:facetselect JCR Node path
        compConfig = (ComponentConfiguration) ocm.getObject("/testpreview/testproject/hst:configuration/hst:configuration/hst:pages/newsoverview");
        assertNotNull(compConfig);
        
        System.out.println("path: " + compConfig.getPath());
        System.out.println("reference name: " + compConfig.getReferenceName());
        System.out.println("content base path: " + compConfig.getComponentContentBasePath());
        System.out.println("class name: " + compConfig.getComponentClassName());
        System.out.println("render path: " + compConfig.getRenderPath());
        
        session.logout();
    }
    
    @Test
    public void testTextPage() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(TextPage.class);
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdCollection.class);
        Mapper mapper = new AnnotationMapperImpl(classes);
        
        Session session = this.repository.login(this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        // Normal JCR Node path
        TextPage productsPage = (TextPage) ocm.getObject("/content/gettingstarted/pagecontent/Products/ProductsPage");
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("stateSummary: " + productsPage.getStateSummary());
        System.out.println("state: " + productsPage.getState());

        session.logout();
    }

    @Test
    public void testCollection() throws Exception {
        List<Class> classes = new ArrayList<Class>();
        classes.add(TextPage.class);
        classes.add(HippoStdDocument.class);
        classes.add(HippoStdCollection.class);
        Mapper mapper = new AnnotationMapperImpl(classes);
        
        Session session = this.repository.login(this.defaultCredentials);
        
        ObjectContentManager ocm = createObjectContentManager(session, mapper);
        
        // Normal JCR Node path
        HippoStdCollection coll = (HippoStdCollection) ocm.getObject("/content/gettingstarted/pagecontent");
        assertNotNull(coll);
        assertNotNull(coll.getNode());
        
        System.out.println("node: " + coll.getNode());
        System.out.println("path: " + coll.getPath());
        
        List<HippoStdCollection> childColl = coll.getCollections();
        assertNotNull(childColl);
        assertFalse(childColl.isEmpty());
        
        System.out.println("childColl: " + childColl);
        
        for (HippoStdCollection childCollItem : childColl) {
            System.out.println("childCollItem: " + childCollItem.getName() + ", " + childCollItem.getPath());
        }
        
        HippoStdCollection productsColl = (HippoStdCollection) ocm.getObject("/content/gettingstarted/pagecontent/Products");
        
        List<HippoStdDocument> childDocs = productsColl.getDocuments();
        assertNotNull(childDocs);
        assertFalse(childDocs.isEmpty());
        
        System.out.println("childDocs: " + childDocs);
        
        for (HippoStdDocument childDoc : childDocs) {
            System.out.println("childDoc: " + childDoc.getName() + ", " + childDoc.getPath() + ", " + childDoc.getState() + ", " + childDoc.getStateSummary());
        }

        session.logout();
    }
    
    private ObjectContentManager createObjectContentManager(Session session, Mapper mapper) throws UnsupportedRepositoryOperationException, RepositoryException {
        ObjectContentManager ocm = null;
        
        DefaultAtomicTypeConverterProvider converterProvider = new DefaultAtomicTypeConverterProvider();
        Map atomicTypeConverters = converterProvider.getAtomicTypeConverters();
        QueryManager queryManager = new QueryManagerImpl(mapper, atomicTypeConverters, session.getValueFactory());
        ProxyManager proxyManager = new ProxyManagerImpl();
        ObjectCache requestObjectCache = new RequestObjectCacheImpl();
        ObjectConverter objectConverter = new HstObjectConverterImpl(mapper, converterProvider, proxyManager, requestObjectCache);
        ocm = new ObjectContentManagerImpl(mapper, objectConverter, queryManager, requestObjectCache, session);
        
        return ocm;
    }
}
