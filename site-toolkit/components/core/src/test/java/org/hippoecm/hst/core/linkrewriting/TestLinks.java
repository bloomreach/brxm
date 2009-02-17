package org.hippoecm.hst.core.linkrewriting;

import static org.junit.Assert.assertEquals;

import org.hippoecm.hst.configuration.ConfigurationViewUtilities;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.HstSitesService;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.linkrewriting.BasicHstLinkRewriter;
import org.hippoecm.hst.linkrewriting.HstLink;
import org.hippoecm.hst.linkrewriting.HstLinkRewriter;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Test;

public class TestLinks extends AbstractHstTestCase{

        
        @Test
        public void testLinkToSiteMapItemId() {
     
             try {
                HstSites hstSites = new HstSitesService(getHstSitesNode(TESTPREVIEW_NODEPATH)) ;

                StringBuffer buf =  new StringBuffer();
                ConfigurationViewUtilities.view(buf,hstSites);
                System.out.println(buf.toString());
                
                HstSiteMapItem currentSiteMapItem = hstSites.getSite(TESTPROJECT_NAME).getSiteMap().getSiteMapItemById("news");
                
                HstLinkRewriter hstLinkRewriter = new BasicHstLinkRewriter();
                HstLink hstLink = hstLinkRewriter.rewrite("news/inland", currentSiteMapItem);
                assertEquals("The path of the hstLink should be 'news/inland'", "news/inland", hstLink.getPath());
                assertEquals("The site name of the link should be '"+TESTPROJECT_NAME+"'",TESTPROJECT_NAME, hstLink.getHstSite().getName());
             }
             catch (ServiceException e) {
                 e.printStackTrace();
             }

        }
}
