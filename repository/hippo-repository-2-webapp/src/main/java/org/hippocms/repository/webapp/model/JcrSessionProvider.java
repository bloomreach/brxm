package org.hippocms.repository.webapp.model;

import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.protocol.http.WebSession;

public class JcrSessionProvider extends WebSession {
    private static final long serialVersionUID = 1L;
        
    private String address;
    private javax.jcr.Session jcrSession;

    public JcrSessionProvider(Application application, Request request, String address) {
        super(application, request);
        this.address = address;
    }

    public javax.jcr.Session getSession()  {
        if (jcrSession == null || !jcrSession.isLive()) {
            try {
                ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory();
                Repository repository = repositoryFactory.getRepository(address);
                jcrSession = repository.login(new SimpleCredentials("username", "password".toCharArray()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jcrSession;
    }

 
}
