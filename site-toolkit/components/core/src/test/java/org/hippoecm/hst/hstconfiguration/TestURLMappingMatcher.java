package org.hippoecm.hst.hstconfiguration;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSiteService;
import org.hippoecm.hst.configuration.sitemap.HstMatchingSiteMapItem;
import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepository;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.test.AbstractSpringTestCase;

public class TestURLMappingMatcher extends AbstractSpringTestCase {

    public static final String PREVIEW_NODEPATH = "preview";
    public static final String CONFIGURATION_NODEPATH = "hst:configuration/hst:configuration";
    public static final String CONTENT_NODEPATH = "content";

    public void testURLMapping() {
//        TODO: Commented out because of compilation error
//
//        try {
//            HstSiteNodes hstSiteNodes = getHstSiteNodes();
//            HstSite hstSiteService = new HstSiteService("test", hstSiteNodes.getConfigNode(), hstSiteNodes.getContentNode());
//            HstMatchingSiteMapItem m = hstSiteService.getSiteMap().match("news/inland");
//            if (m != null) {
//                StringBuffer buf = new StringBuffer();
//                m.dump(buf, "");
//
//                System.out.println(buf.toString());
//
//            }
//
//        } catch (ServiceException e) {
//            e.printStackTrace();
//        } catch (RepositoryException e) {
//            e.printStackTrace();
//        }
//
    }

    protected String[] getConfigurations() {
        return new String[] { "org/hippoecm/hst/core/jcr/pool/GeneralBasicPoolingRepository.xml" };
    }

    protected HstSiteNodes getHstSiteNodes() {
        
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
            Node hstConfNode  = session.getRootNode().getNode(PREVIEW_NODEPATH + "/" + CONFIGURATION_NODEPATH);
            Node contentNode  = session.getRootNode().getNode(PREVIEW_NODEPATH + "/" + CONTENT_NODEPATH);
            return new HstSiteNodes(hstConfNode, contentNode);
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        return null;
    }

    class HstSiteNodes {
        
        Node configNode;
        Node contentNode;
        
        HstSiteNodes(Node configNode, Node contentNode){
            this.configNode = configNode;
            this.contentNode = contentNode;
        }

        public Node getConfigNode() {
            return configNode;
        }

        public Node getContentNode() {
            return contentNode;
        }
        
        
    }
}
