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
package org.hippoecm.hst.component.support.ocm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.mock.MockHstRequest;
import org.hippoecm.hst.mock.MockHstRequestContext;
import org.hippoecm.hst.ocm.HippoStdCollection;
import org.hippoecm.hst.ocm.HippoStdNode;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class TestBaseHstComponent {

    protected ServletContext servletContext;
    protected ServletConfig servletConfig;
    protected ComponentConfiguration compConfig;
    protected MockHstRequestContext hstRequestContext;
    protected MockHstRequest hstRequest;
    
    protected HippoRepository repository;
    protected SimpleCredentials defaultCredentials;

    @Before
    public void setUp() throws Exception {
        this.repository = HippoRepositoryFactory.getHippoRepository("rmi://127.0.0.1:1099/hipporepository");
        this.defaultCredentials = new SimpleCredentials("admin", "admin".toCharArray());
        
        this.servletContext = new MockServletContext() {
            @Override
            public InputStream getResourceAsStream(String path) {
                if (BaseHstComponent.DEFAULT_OCM_ANNOTATED_CLASSES_CONF.equals(path)) {
                    return TestBaseHstComponent.class.getResourceAsStream("ocm-annotated-classes.xml");
                } else {
                    return super.getResourceAsStream(path);
                }
            }
        };
        
        this.servletConfig = new MockServletConfig(servletContext);
        
        this.compConfig = new ComponentConfiguration() {
            public Object getResolvedProperty(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
                return null;
            }
        };
        
        this.hstRequestContext = new MockHstRequestContext();
        this.hstRequest = new MockHstRequest();
        this.hstRequest.setRequestContext(this.hstRequestContext);
   }
    
    @Test
    public void testBaseHstComponent() throws LoginException, RepositoryException {
        Session session = this.repository.login(this.defaultCredentials);
        this.hstRequestContext.setSession(session);
        BaseHstComponent baseHstComponent = new BaseHstComponent();
        baseHstComponent.init(this.servletConfig, this.compConfig);
        
        ObjectContentManager ocm = baseHstComponent.getObjectContentManager(hstRequest);
        assertNotNull(ocm);
        
        ObjectContentManager ocm2 = baseHstComponent.getObjectContentManager(hstRequest);
        assertTrue(ocm == ocm2);
        
        HippoStdNode documentNode = (HippoStdNode) ocm.getObject("/content/gettingstarted/pagecontent/Products/ProductsPage");
        assertNotNull(documentNode);
        System.out.println("contentNode: " + documentNode);
        assertTrue(documentNode instanceof TextPage);
        
        HippoStdNode collectionNode = (HippoStdNode) ocm.getObject("/content/gettingstarted/pagecontent");
        assertNotNull(collectionNode);
        System.out.println("collectionNode: " + collectionNode);
        assertTrue(collectionNode instanceof HippoStdCollection);
    }
    
}
