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

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Session;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstCtxWhereClauseComputerImpl;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSimpleBean extends AbstractBeanTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
    }

    @Test
    public void testSimpleObjectGetting() throws Exception {
             
        ObjectConverter objectConverter = getObjectConverter();
        
        Session session = this.getSession();
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);

        HippoFolder folder = (HippoFolder) obm.getObject("/testcontent/documents/testproject/Products");
        
     
        Object o = obm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        assertNotNull("The object is not retrieved from the path.", o);
        assertTrue(" Object should be an instance of PersistableTextPage and not PersistableTextPageCopy, because PersistableTextPage is added first", o instanceof PersistableTextPage);
        
        PersistableTextPage productsPage =  (PersistableTextPage)obm.getObject("/testcontent/documents/testproject/Products/SomeProduct");
        PersistableTextPage productsPage2 = (PersistableTextPage) obm.getObject("/testcontent/documents/testproject/Products/SomeProduct/SomeProduct");

        assertTrue("Handle and Document should return true for equalCompare ", productsPage.equalCompare(productsPage2));
        assertFalse("Folder and Document should return false for equalCompare ",folder.equalCompare(productsPage2));
        
        assertNotNull(productsPage);
        assertNotNull(productsPage.getNode());
        
        session.logout();
    }
    
    @Test
    public void testSimpleObjectQuery() throws Exception {
        ObjectConverter objectConverter = getObjectConverter();
        
        Session session = this.getSession();
      
        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        
        HippoFolder folder = (HippoFolder) obm.getObject("/testcontent/documents/testproject/Products");
        
        HstQueryManager queryManager = new HstQueryManagerImpl(objectConverter, new HstCtxWhereClauseComputerImpl());
        
        HstQuery hstQuery = queryManager.createQuery(folder);

        String query = "CMS";
        Filter filter = new FilterImpl();
        filter.addContains(".", query);
        hstQuery.setFilter(filter);
        
        List<HippoBean> resultBeans = doQuery(hstQuery);
        assertFalse("The query cannot find any result with '" + query + "'.", resultBeans.isEmpty());
        
        query = "is";
        filter = new FilterImpl();
        filter.addContains(".", query);
        hstQuery.setFilter(filter);
        
        resultBeans = doQuery(hstQuery);
        assertTrue("The query should not find any result with common English word like '" + query + "'.", resultBeans.isEmpty());
        
        session.logout();
    }
    
    private static List<HippoBean> doQuery(HstQuery hstQuery) throws Exception {
        List<HippoBean> resultBeans = new LinkedList<HippoBean>();
        final HstQueryResult result = hstQuery.execute();

        for (HippoBeanIterator it = result.getHippoBeans(); it.hasNext(); ) {
            HippoBean bean = it.nextHippoBean();
            if (bean != null) {
                resultBeans.add(bean);
            }
        }
        
        return resultBeans;
    }
    
}
