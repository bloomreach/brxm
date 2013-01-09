/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.service.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.SerializationUtils;
import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.hst.test.basic.BasicHstTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestJCRServiceBean extends AbstractBeanTestCase {
    
    private static final String TESTPROJECT_EXISTING_NODE = BasicHstTestCase.TEST_SITE_CONTENT_PATH + "/common/homepage/homepage";


    @Before
    public void setUp() throws Exception {
        super.setUp();
        
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
    }
    
    @Test
    public void testServiceBeanProxy() throws Exception {
        Session session = this.getSession();
        Node node = (Node)session.getItem(TESTPROJECT_EXISTING_NODE);
        TextPage t = ServiceFactory.create(node, TextPage.class);
        
        assertNotNull("title property is null!", t.getTitle());
        
        assertNotNull("summary property is null!", t.getSummary());
        
        byte [] bytes = SerializationUtils.serialize((Serializable) t);
        TextPage t2 = (TextPage) SerializationUtils.deserialize(bytes);
        
        assertEquals("The title property of the deserialized one is different from the original.", 
                t.getTitle(), t2.getTitle());
        assertEquals("The summary property of the deserialized one is different from the original.", 
                t.getSummary(), t2.getSummary());
        
        Service underlyingService = t.getUnderlyingService();
        assertNotNull("The underlying service is null!", underlyingService);
    }
        
    @Test
    public void testServiceBeanProxyWithClass() throws Exception {
        
        Session session = this.getSession();
        
        Node node = (Node)session.getItem(TESTPROJECT_EXISTING_NODE);
        TextPage t = ServiceFactory.create(node, TextPageImpl.class);
        
        assertTrue("the returned class is not implementation delegatee class.", t instanceof TextPageImpl);
        assertNotNull("title property is null!", t.getTitle());
        
        assertNotNull("summary property is null!", t.getSummary());
        
        byte [] bytes = SerializationUtils.serialize((Serializable) t);
        TextPage t2 = (TextPage) SerializationUtils.deserialize(bytes);
        
        assertEquals("The title property of the deserialized one is different from the original.", 
                t.getTitle(), t2.getTitle());
        assertEquals("The summary property of the deserialized one is different from the original.", 
                t.getSummary(), t2.getSummary());
        
        Service underlyingService = t.getUnderlyingService();
        assertNotNull("The underlying service is null!", underlyingService);
    }
}
