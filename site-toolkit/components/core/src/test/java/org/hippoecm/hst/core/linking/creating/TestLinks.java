package org.hippoecm.hst.core.linking.creating;

import static org.junit.Assert.assertEquals;

import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.HstSitesService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Test;

public class TestLinks extends AbstractHstTestCase{

        
        @Test
        public void testLinkToSiteMapItemId() {
     
             try {
                HstSites hstSites = new HstSitesService(getHstSitesNode(TESTPREVIEW_NODEPATH)) ;

                HstSiteMapItem currentSiteMapItem = hstSites.getSite(TESTPROJECT_NAME).getSiteMap().getSiteMapItemById("news");
                
                HstLinkCreator hstLinkCreatorr = new BasicHstLinkCreator();
                HstLink hstLink = hstLinkCreatorr.create("news/inland", currentSiteMapItem);
                assertEquals("The path of the hstLink should be 'news/inland'", "news/inland", hstLink.getPath());
                assertEquals("The site name of the link should be '"+TESTPROJECT_NAME+"'",TESTPROJECT_NAME, hstLink.getHstSite().getName());
             }
             catch (ServiceException e) {
                 e.printStackTrace();
             }

        }
}
