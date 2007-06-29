package org.hippocms.repository.webapp;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.wicket.markup.html.WebPage;

public class Browser extends WebPage {
    private static final long serialVersionUID = 1L;
    
    private PropertiesPanel properties;
    private TreePanel tree;

    public Browser() throws LoginException, RepositoryException, MalformedURLException, ClassCastException,
            RemoteException, NotBoundException {

        ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory();
        Repository repository = repositoryFactory.getRepository("rmi://localhost:1099/jackrabbit.repository");
        Session jcrSession = repository.login(new SimpleCredentials("username", "password".toCharArray()));
        
        properties = new PropertiesPanel("propertiesPanel", jcrSession);
        add(properties);

        tree = new TreePanel("treePanel", properties.getNodeEditor(), jcrSession.getRootNode());
        add(tree);
    }


}
