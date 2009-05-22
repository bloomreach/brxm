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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdFolder;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNode;
import org.hippoecm.hst.mock.MockHstRequest;
import org.hippoecm.hst.mock.MockHstRequestContext;
import org.hippoecm.hst.test.AbstractClientSpringTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

@Ignore
public class TestBaseHstComponent extends AbstractClientSpringTestCase {

    protected ServletContext servletContext;
    protected ServletConfig servletConfig;
    protected ComponentConfiguration compConfig;
    protected MockHstRequestContext hstRequestContext;
    protected MockHstRequest hstRequest;
    
    protected Repository repository;
    protected Credentials defaultCredentials;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.repository = getComponent(Repository.class.getName());
        this.defaultCredentials = getComponent(Credentials.class.getName());
        
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
            public String getParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
                return null;
            }

            public Map<String, String> getParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
                return null;
            }
        };
        
        this.hstRequestContext = new MockHstRequestContext();
        this.hstRequest = new MockHstRequest();
        this.hstRequest.setRequestContext(this.hstRequestContext);
    }
    
    @Test
    public void testBaseHstComponent() throws Exception {
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
        
        Map<String, Object> properties = documentNode.getProperty();
        assertNotNull(properties);
        System.out.println("properties: " + properties);
        assertEquals("Products", properties.get("gettingstarted:title"));
        
        HippoStdNode collNode = (HippoStdNode) ocm.getObject("/content/gettingstarted/pagecontent");
        assertNotNull(collNode);
        System.out.println("collectionNode: " + collNode);
        assertTrue(collNode instanceof HippoStdFolder);
        
        HippoStdFolder folderNode = (HippoStdFolder) collNode;
        List<HippoStdFolder> childFolders = folderNode.getFolders();
        assertNotNull(childFolders);
        System.out.println("childFolders: " + childFolders);
    }
    
}
