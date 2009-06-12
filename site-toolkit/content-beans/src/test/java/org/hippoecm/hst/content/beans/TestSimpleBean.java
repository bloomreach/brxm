/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.content.beans;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.beanutils.MethodUtils;
import org.hippoecm.hst.AbstractBeanSpringTestCase;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSimpleBean extends AbstractBeanSpringTestCase {

    protected Object repository;
    protected Credentials defaultCredentials;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        this.repository = getComponent(Repository.class.getName());
        this.defaultCredentials = getComponent(Credentials.class.getName());
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
        if (this.repository != null) {
            MethodUtils.invokeMethod(this.repository, "close", null);
        }
    }

    @Test
    public void testSimpleObjectGetting() throws Exception {
             
        ObjectConverter objectConverter = getObjectConverter();
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);

        HippoFolder folder = (HippoFolder) obm.getObject("/testcontent/documents/testproject/Products");
        
     
        Object o = obm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull("The object is not retrieved from the path.", o);
        assertTrue(" Object should be an instance of SimpleTextPage and not SimpleTextPageCopy, because SimpleTextPage is added first", o instanceof SimpleTextPage);
        
        SimpleTextPage productsPage =  (SimpleTextPage)obm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        SimpleTextPage productsPage2 = (SimpleTextPage) obm.getObject("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct");

        assertTrue("Handle and Document should return true for equalCompare ", productsPage.equalCompare(productsPage2));
        assertFalse("Folder and Document should return false for equalCompare ",folder.equalCompare(productsPage2));
        
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        System.out.println("body: " + productsPage.getBody().getContent());
        
        session.logout();
    }
    
   
}
