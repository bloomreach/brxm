package org.hippoecm.hst.hstconfiguration;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstConfigurationService;
import org.hippoecm.hst.configuration.JCRHstConfigurationService;
import org.hippoecm.hst.configuration.sitemap.MatchingSiteMapItem;
import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepository;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.test.AbstractSpringTestCase;

public class TestURLMappingMatcher extends AbstractSpringTestCase {

    public static final String PREVIEW_NODEPATH = "preview";
    public static final String CONFIGURATION_NODEPATH = "hst:configuration/hst:configuration";

    public void testURLMappingLoading() {

        try {
            Node hstConfNode = getConfigNode();
            HstConfigurationService hstConfigurationService = new JCRHstConfigurationService(hstConfNode);
            MatchingSiteMapItem m = hstConfigurationService.getSiteMapService().match("news/inland");
            if (m != null) {
                StringBuffer buf = new StringBuffer();
                m.dump(buf, "");

                System.out.println(buf.toString());

            }

        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

    }

    protected String[] getConfigurations() {
        return new String[] { "org/hippoecm/hst/jcr/pool/GeneralBasicPoolingRepository.xml" };
    }

    protected Node getConfigNode() {
        
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

        Node hstConfNode = null;
        try {
            hstConfNode = session.getRootNode().getNode(PREVIEW_NODEPATH + "/" + CONFIGURATION_NODEPATH);

        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        return hstConfNode;
    }

}
