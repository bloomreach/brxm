package org.hippoecm.hst.hstconfiguration;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.HstSitesService;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepository;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;


public class TestURLMappingMatcher extends AbstractSpringTestCase {

    public static final String PREVIEW_NODEPATH = "preview";
    
    @Test
    public void stURLMapping() {

         try {
            HstSites hstSites = new HstSitesService(getHstSitesNode()) ;
            
            HstSite s =  hstSites.getSite("myproject");
            
            HstSiteMapItem sItem =  s.getSiteMap().getSiteMapItem("news");
            
            HstComponentConfiguration c = s.getComponentsConfiguration().getComponentConfiguration(sItem.getComponentConfigurationId()); 
            
            
        } catch (ServiceException e) {
            
            e.printStackTrace();
        }

    }

    protected String[] getConfigurations() {
        return new String[] { "org/hippoecm/hst/core/jcr/pool/GeneralBasicPoolingRepository.xml" };
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
            Node previewSites  = session.getRootNode().getNode(PREVIEW_NODEPATH);
            return previewSites;
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        return null;
    }

}
