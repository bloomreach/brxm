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

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.request.BasicHstSiteMapMatcher;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

/**
 * This test tests whether the linkcreation of items (jcr Nodes) which involve SiteMap wildcard matching are being created correctly
 *
 */
public class WildCardLinks extends AbstractSpringTestCase{
    
        private static final String TESTPROJECT_NAME = "testproject";

        private static final String TESTPROJECT_FOLDER1 = "/testpreview/testproject/hst:content/News";
        private static final String TESTPROJECT_DOCUMENT1 = "/testpreview/testproject/hst:content/News/News1";
        private static final String TESTPROJECT_DOCUMENT2 = "/testpreview/testproject/hst:content/News/News1/News1";
        private static final String TESTPROJECT_FOLDER2 = "/testpreview/testproject/hst:content/News/2009";
        private static final String TESTPROJECT_FOLDER3 = "/testpreview/testproject/hst:content/News/2009/April";
        private static final String TESTPROJECT_FOLDER4 = "/testpreview/testproject/hst:content/News/2009/April/Day5";
        private static final String TESTPROJECT_DOCUMENT3 = "/testpreview/testproject/hst:content/News/2009/April/AprilNewsArticle";
        private static final String TESTPROJECT_DOCUMENT4 = "/testpreview/testproject/hst:content/News/2009/April/Day5/Day5Article";
        
        
        
         
        private HstSitesManager hstSitesManager;
        private HstLinkCreator hstLinkCreator;
        private HstSiteMapMatcher hstSiteMapMatcher;
        private HstSite hstSite;
        private ResolvedSiteMapItem res;
        private Session session;
        
        @Override
        public void setUp() throws Exception{
            super.setUp();
            this.hstSitesManager = getComponent(HstSitesManager.class.getName());
            this.hstLinkCreator = getComponent(HstLinkCreator.class.getName());
            this.hstSiteMapMatcher = new BasicHstSiteMapMatcher();
            this.hstSite = this.hstSitesManager.getSites().getSite(TESTPROJECT_NAME);
            this.res = hstSiteMapMatcher.match("news/2009", hstSite);

            Repository repository = (Repository) getComponent(Repository.class.getName());
            this.session = repository.login();
        }
        
       /**
        * Test *WITH* sitemap items involved having a wildcard, or thus *WITH* 'hst:relativecontentpath' containing ${1} kind of
        * parameters
        */
       
       @Test 
       public void testWithWildCardSiteMapItem() throws RepositoryException{
           Node node1 = (Node)session.getItem(TESTPROJECT_FOLDER1);
           HstLink hstLink = hstLinkCreator.create(node1, res);
           assertEquals("The getPath of the HstLink must be equal to 'news' but was '"+hstLink.getPath()+"' ","news", hstLink.getPath());
           
           Node node2 = (Node)session.getItem(TESTPROJECT_DOCUMENT1);
           hstLink = hstLinkCreator.create(node2, res);
           assertEquals("The getPath of the HstLink must be equal to 'news/News1.html' but was '"+hstLink.getPath()+"' ","news/News1.html", hstLink.getPath());
           
           Node node3 = (Node)session.getItem(TESTPROJECT_DOCUMENT2);
           hstLink = hstLinkCreator.create(node3, res);
           assertEquals("The getPath of the HstLink must be equal to 'news/News1.html' but was '"+hstLink.getPath()+"' ","news/News1.html", hstLink.getPath());
           
           Node node4 = (Node)session.getItem(TESTPROJECT_FOLDER2);
           hstLink = hstLinkCreator.create(node4, res);
           assertEquals("The getPath of the HstLink must be equal to 'news/2009' but was '"+hstLink.getPath()+"' ","news/2009", hstLink.getPath());
           
           
           Node node5 = (Node)session.getItem(TESTPROJECT_FOLDER3);
           hstLink = hstLinkCreator.create(node5, res);
           assertEquals("The getPath of the HstLink must be equal to 'news/2009/April' but was '"+hstLink.getPath()+"' ","news/2009/April", hstLink.getPath());
           
           Node node6 = (Node)session.getItem(TESTPROJECT_FOLDER4);
           hstLink = hstLinkCreator.create(node6, res);
           assertEquals("The getPath of the HstLink must be equal to 'news/2009/April/Day5' but was '"+hstLink.getPath()+"' ","news/2009/April/Day5", hstLink.getPath());
           
           Node node7 = (Node)session.getItem(TESTPROJECT_DOCUMENT3);
           hstLink = hstLinkCreator.create(node7, res);
           assertEquals("The getPath of the HstLink must be equal to 'news/2009/April/AprilNewsArticle.html' but was '"+hstLink.getPath()+"' ","news/2009/April/AprilNewsArticle.html", hstLink.getPath());
           
           Node node8 = (Node)session.getItem(TESTPROJECT_DOCUMENT4);
           hstLink = hstLinkCreator.create(node8, res);
           assertEquals("The getPath of the HstLink must be equal to 'news/2009/April/Day5/Day5Article.html' but was '"+hstLink.getPath()+"' ","news/2009/April/Day5/Day5Article.html", hstLink.getPath());
        
       }
       
        
}
