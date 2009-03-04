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
package org.hippoecm.hst.core.linking;

import static org.junit.Assert.assertEquals;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.hst.service.ServiceNamespace;
import org.hippoecm.hst.service.UnderlyingServiceAware;
import org.hippoecm.hst.test.AbstractSpringTestCase;

public class TestLinks extends AbstractSpringTestCase{
    
        private static final String TESTPROJECT_NAME = "testproject";
        
        private static final String TESTPROJECT_EXISTING_VIRTUALHANDLE= "/testpreview/testproject/hst:content/Products/SomeProduct";
        private static final String TESTPROJECT_EXISTING_VIRTUALDOCUMENT = "/testpreview/testproject/hst:content/Products/SomeProduct/SomeProduct";
        private static final String TESTPROJECT_EXISTING_VIRTUALHANDLE_OTHER = "/testpreview/testproject/hst:content/Products/HippoCMS";
        
        private HstSitesManager hstSitesManager;
        private HstLinkCreator hstLinkCreator;
       
        @Override
        public void setUp() throws Exception{
            super.setUp();
            this.hstSitesManager = getComponent(HstSitesManager.class.getName());
            this.hstLinkCreator = getComponent(HstLinkCreator.class.getName());
           
        }
        
        public void testLinkToSiteMapItemId() {
            
            HstSiteMapItem currentSiteMapItem = this.hstSitesManager.getSites().getSite(TESTPROJECT_NAME).getSiteMap().getSiteMapItemById("products");
            HstLink hstLink = hstLinkCreator.create("products", currentSiteMapItem);
            assertEquals("The path of the hstLink should be 'products'", "products", hstLink.getPath());
            assertEquals("The site name of the link should be '"+TESTPROJECT_NAME+"'",TESTPROJECT_NAME, hstLink.getHstSite().getName());
            

        }
        
      
        public void testLinkCreateOfNode(){
            
            Repository repository = (Repository) getComponent(Repository.class.getName());
            
            try {
                Session session = repository.login();
                HstSiteMapItem currentSiteMapItem = this.hstSitesManager.getSites().getSite(TESTPROJECT_NAME).getSiteMap().getSiteMapItemById("products");
                
                Node someProductHandle = (Node)session.getItem(TESTPROJECT_EXISTING_VIRTUALHANDLE);
                Node someProductDocument = (Node)session.getItem(TESTPROJECT_EXISTING_VIRTUALDOCUMENT);
                Node hippoCMSHandle = (Node)session.getItem(TESTPROJECT_EXISTING_VIRTUALHANDLE_OTHER);
                
                // a link creation to a handle
                HstLink hstLink = hstLinkCreator.create(someProductHandle, currentSiteMapItem);
                assertEquals("","products/someproduct", hstLink.getPath());
                
                // a link creation to a document below the handle should result in the same link 
                hstLink = hstLinkCreator.create(someProductDocument, currentSiteMapItem);
                assertEquals("", "products/someproduct", hstLink.getPath());
                
                // The sitemap item that matches returns a path 'products'. HippoCMS is part of the nodepath
                // that is not represented within the sitemap relativeContentLocation
                hstLink = hstLinkCreator.create(hippoCMSHandle, currentSiteMapItem);
                assertEquals("", "products/HippoCMS", hstLink.getPath());
                
            } catch (LoginException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            
            
        }
       
   
       public void testLinkCreateOfService(){
           
        Repository repository = (Repository) getComponent(Repository.class.getName());

        try {
            Session session = repository.login();
            HstSiteMapItem currentSiteMapItem = this.hstSitesManager.getSites().getSite(TESTPROJECT_NAME).getSiteMap().getSiteMapItemById("products");
            Node someProductHandle = (Node) session.getItem(TESTPROJECT_EXISTING_VIRTUALHANDLE);
            
            TestPage s = ServiceFactory.create(someProductHandle, TestPage.class);
            
            
            HstLink hstLink = hstLinkCreator.create(s.getUnderlyingService(), currentSiteMapItem);
            
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
      }
       
       
       @ServiceNamespace(prefix = "testproject")
       public interface TestPage extends UnderlyingServiceAware{
           String getTitle();
       }
        
}
