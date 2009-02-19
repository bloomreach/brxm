package org.hippoecm.hst.service.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.lang.SerializationUtils;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.Test;

public class TestJCRServiceBean extends AbstractSpringTestCase{
    
    private static final String TESTPROJECT_EXISTING_VIRTUALNODE = "/testpreview/testproject/hst:content/Products/SomeProduct/SomeProduct";
    private Repository repository;
    
    @Override 
    public void setUp() throws Exception{
        super.setUp();
        repository  = (Repository) getComponent(Repository.class.getName());
    }
    
    @Test
    public void testServiceBeanProxy() throws Exception {
        Session session = repository.login();
        Node node = (Node)session.getItem(TESTPROJECT_EXISTING_VIRTUALNODE);
        TextPage t = ServiceFactory.create(node, TextPage.class);
        
        assertNotNull("title property is null!", t.getTitle());
        System.out.println(t.getTitle());
        
        assertNotNull("summary property is null!", t.getSummary());
        System.out.println(t.getSummary());
        
        byte [] bytes = SerializationUtils.serialize((Serializable) t);
        TextPage t2 = (TextPage) SerializationUtils.deserialize(bytes);
        
        assertEquals("The title property of the deserialized one is different from the original.", 
                t.getTitle(), t2.getTitle());
        assertEquals("The summary property of the deserialized one is different from the original.", 
                t.getSummary(), t2.getSummary());
        
        Service underlyingService = t.getUnderlyingService();
        assertNotNull("The underlying service is null!", underlyingService);
        System.out.println("The underlying service: " + underlyingService);
    }
        
}
