package org.hippoecm.hst.service.jcr;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

public class TestJCRServiceBean extends AbstractSpringTestCase{
    
    private static final String TESTPROJECT_EXISTING_VIRTUALNODE = "/testpreview/testproject/hst:content/Products/ProductsPage/ProductsPage";
    private Repository repository;
    
    @Override 
    public void setUp() throws Exception{
        super.setUp();
        repository  = (Repository) getComponent(Repository.class.getName());
    }
    
    @Test
    public void testServiceBeanProxy() throws IllegalAccessException, NoSuchFieldException {
        
        try {
            Session session = repository.login();
            Node node = (Node)session.getItem(TESTPROJECT_EXISTING_VIRTUALNODE);
            TextPage t = ServiceFactory.create(node, TextPage.class);
            
            System.out.println(t.getTitle());
            System.out.println(t.getSummary());
            
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        
    }
        
}
