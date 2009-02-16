package org.hippoecm.hst.core.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.ConfigurationViewUtilities;
import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.HstSitesService;
import org.hippoecm.hst.configuration.SimpleHstSiteMapMatcher;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher.MatchResult;
import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepository;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

public class TestConfiguration extends AbstractSpringTestCase {

    public static final String TESTPREVIEW_NODEPATH = "testpreview";
    
    
    public void testConfiguration() {
 
         try {
            HstSites hstSites = new HstSitesService(getHstSitesNode()) ;
            HstSite s =  hstSites.getSite("testproject");
            HstSite s2 =  hstSites.getSite("nonexistingproject");
            assertNull(s2);
            
            HstSiteMapItem sItem =  s.getSiteMap().getSiteMapItem("news");
            HstComponentConfiguration c = s.getComponentsConfiguration().getComponentConfiguration(sItem.getComponentConfigurationId()); 
            assertNotNull(c);
            
        } catch (ServiceException e) {
            e.printStackTrace();
        }

    }
    
    @Test
    public void testPathMatcher(){
        
        HstSites hstSites;
        try {
            
            hstSites = new HstSitesService(getHstSitesNode());
            
            StringBuffer buf = new StringBuffer();
            
            
            HstSite hstSite =  hstSites.getSite("testproject");
            
            HstSiteMapMatcher hstSiteMapMatcher = new SimpleHstSiteMapMatcher();

            MatchResult matchNoResult = hstSiteMapMatcher.match("/non/exist/ing", hstSite);
            assertNull(matchNoResult.getSiteMapItem());
            assertNull(matchNoResult.getCompontentConfiguration());
            assertEquals(matchNoResult.getRemainder(), "non/exist/ing");
            
       
            
            MatchResult matchResult = hstSiteMapMatcher.match("/news/foo/bar", hstSite);
            assertEquals(matchResult.getRemainder(), "foo/bar");
            assertEquals(matchResult.getSiteMapItem().getId(), "news");

            ConfigurationViewUtilities.view(buf, matchResult);
            assertTrue("Buffer should not be empty", buf.length() > 0);
            
            assertEquals(matchResult.getSiteMapItem().getChild("inland").getId(), "news/inland");
            assertEquals(matchResult.getCompontentConfiguration().getId(), "pages/newsoverview");
            
            matchResult = hstSiteMapMatcher.match("/news/foo/bar/", hstSite);
            assertEquals(matchResult.getRemainder(), "foo/bar");
            
            matchResult = hstSiteMapMatcher.match("/news", hstSite);
            assertEquals(matchResult.getRemainder(), "");
            
            
            
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }


    protected Node getHstSitesNode() {
        
        BasicPoolingRepository poolingRepository = (BasicPoolingRepository) getComponent(PoolingRepository.class.getName());
        Repository repository = poolingRepository;

        Session session = null;
        try {
            session = repository.login();
        } catch (LoginException e1) {
            e1.printStackTrace();
        } catch (RepositoryException e1) {
            e1.printStackTrace();
        }

        try {
            Node previewSites  = session.getRootNode().getNode(TESTPREVIEW_NODEPATH);
            return previewSites;
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        return null;
    }

}
