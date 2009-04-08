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
package org.hippoecm.hst.hippo.ocm;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.beanutils.MethodUtils;
import org.hippoecm.hst.hippo.ocm.manager.impl.ObjectContentManagerImpl;
import org.hippoecm.hst.hippo.ocm.manager.impl.ObjectConverterImpl;
import org.hippoecm.hst.jackrabbit.ocm.TextPage1;
import org.hippoecm.hst.ocm.manager.ObjectContentManager;
import org.hippoecm.hst.ocm.manager.ObjectConverter;
import org.hippoecm.hst.test.AbstractOCMSpringTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSimpleOCM extends AbstractOCMSpringTestCase {

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
        Map<String, Class[]> jcrPrimaryNodeTypeClassMapping = new HashMap<String, Class[]>();
        jcrPrimaryNodeTypeClassMapping.put("testproject:textpage", new Class [] { SimpleTextPage1.class });
        
        ObjectConverter objectConverter = new ObjectConverterImpl(jcrPrimaryNodeTypeClassMapping);
        
        Session session = (Session) MethodUtils.invokeMethod(this.repository, "login", this.defaultCredentials);
        ObjectContentManager ocm = new ObjectContentManagerImpl(session, objectConverter);
        
        SimpleTextPage1 productsPage = (SimpleTextPage1) ocm.getObject("/testcontent/testproject/Products/SomeProduct");
        assertNotNull(productsPage);
        //assertNotNull(productsPage.getNode());
        
        System.out.println("node: " + productsPage.getNode());
        System.out.println("path: " + productsPage.getPath());
        System.out.println("title: " + productsPage.getTitle());
        
        session.logout();
    }
}
