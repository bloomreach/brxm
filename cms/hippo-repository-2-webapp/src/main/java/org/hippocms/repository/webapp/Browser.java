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
import org.apache.wicket.markup.html.form.Form;

public class Browser extends WebPage {
    private static final long serialVersionUID = 1L;

    private transient Repository repository;

    public Browser() throws LoginException, RepositoryException, MalformedURLException, ClassCastException,
            RemoteException, NotBoundException {

        ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory();
        repository = repositoryFactory.getRepository("rmi://localhost:1099/jackrabbit.repository");

        BrowserForm form = new BrowserForm("form");
        add(form);
    }


    private class BrowserForm extends Form {
        private static final long serialVersionUID = 1L;

        private PropertiesPanel properties;
        private TreePanel tree;

        public BrowserForm(String id) throws LoginException, RepositoryException {
            super(id);

            Session session = repository.login(new SimpleCredentials("username", "password".toCharArray()));
            
            properties = new PropertiesPanel("propertiesPanel");
            add(properties);
            
            tree = new TreePanel("treePanel", (NodeEditor)properties, session.getRootNode());
            add(tree);       
        }

    }

}
