package org.hippoecm.hst.test;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepository;

public class AbstractHstTestCase extends AbstractSpringTestCase {

    protected static final String TESTPREVIEW_NODEPATH = "testpreview";
    protected static final String TESTPROJECT_NAME = "testproject";
    
    protected Node getHstSitesNode(String rootSitesPath) {

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
            Node previewSites = session.getRootNode().getNode(rootSitesPath);
            return previewSites;
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        return null;
    }

}
