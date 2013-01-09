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
package org.hippoecm.hst.content.beans.standard;

import static org.junit.Assert.assertEquals;

import javax.jcr.Session;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestHippoTranslatedBeans extends AbstractBeanTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
    }

    @Test
    public void testTranslatedBeans() throws Exception {
             
        ObjectConverter objectConverter = getObjectConverter();
        
        Session session = this.getSession();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        
        HippoFolder folder = (HippoFolder) obm.getObject("/unittestcontent/documents/unittestproject/common");
        assertEquals("en_US", folder.getLocaleString());
        assertEquals(LocaleUtils.toLocale("en_US"), folder.getLocale());
        
        HippoDocument homePage =  (HippoDocument) obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        assertEquals("en_US", homePage.getLocaleString());
        assertEquals(LocaleUtils.toLocale("en_US"), homePage.getLocale());
        
        session.logout();
    }
    
}
