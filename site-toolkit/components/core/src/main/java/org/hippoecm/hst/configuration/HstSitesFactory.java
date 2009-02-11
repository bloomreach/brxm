package org.hippoecm.hst.configuration;

import javax.jcr.Repository;
import javax.jcr.Session;

public class HstSitesFactory {

    public static HstSites create(Repository repository, String nodePath) throws Exception {
        HstSites sites = null;
        Session session = null;
        
        try {
            session = repository.login();
            sites = new HstSitesService(session.getRootNode().getNode(nodePath));
        } finally {
            if (session != null)
                session.logout();
        }
        
        return sites;
    }
}
