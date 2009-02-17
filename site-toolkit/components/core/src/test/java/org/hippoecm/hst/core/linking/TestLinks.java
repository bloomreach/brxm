package org.hippoecm.hst.core.linking;

import static org.junit.Assert.assertEquals;

import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.HstSitesService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.creating.BasicHstLinkCreator;
import org.hippoecm.hst.core.linking.creating.HstLinkCreator;
import org.hippoecm.hst.core.linking.rewriting.HstLinkRewriter;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

public class TestLinks extends AbstractSpringTestCase{
    
        private static final String TESTPROJECT_NAME = "testproject";
        
        private HstSites hstSites;
        private HstLinkCreator hstLinkCreator;
        private HstLinkRewriter hstLinkRewriter;
    
        @Override
        public void setUp() throws Exception{
            super.setUp();
            this.hstSites = (HstSites) getComponent(HstSites.class.getName());
            this.hstLinkCreator = (HstLinkCreator) getComponent(HstLinkCreator.class.getName());
            this.hstLinkRewriter = (HstLinkRewriter) getComponent(HstLinkRewriter.class.getName());
        }
    
        @Test
        public void testLinkToSiteMapItemId() {
            
            HstSiteMapItem currentSiteMapItem = hstSites.getSite(TESTPROJECT_NAME).getSiteMap().getSiteMapItemById("news");
            
            HstLinkCreator hstLinkCreatorr = new BasicHstLinkCreator();
            HstLink hstLink = hstLinkCreatorr.create("news/inland", currentSiteMapItem);
            assertEquals("The path of the hstLink should be 'news/inland'", "news/inland", hstLink.getPath());
            assertEquals("The site name of the link should be '"+TESTPROJECT_NAME+"'",TESTPROJECT_NAME, hstLink.getHstSite().getName());
            

        }
}
