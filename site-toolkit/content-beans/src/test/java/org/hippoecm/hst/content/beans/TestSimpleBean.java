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
package org.hippoecm.hst.content.beans;

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.hst.AbstractBeanTestCase;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.repository.util.DateTools;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSimpleBean extends AbstractBeanTestCase {


    @Test
    public void testSimpleObjectGetting() throws Exception {
             
        ObjectConverter objectConverter = getObjectConverter();

        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);

        HippoFolder folder = (HippoFolder) obm.getObject("/unittestcontent/documents/unittestproject/common");
        
     
        Object o = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        assertNotNull("The object is not retrieved from the path.", o);
        assertTrue(" Object should be an instance of PersistableTextPage and not PersistableTextPageCopy, because PersistableTextPage is added first. The object is " + o, 
                o instanceof PersistableTextPage);
        
        PersistableTextPage homePage =  (PersistableTextPage)obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        PersistableTextPage homePageAsWell = (PersistableTextPage) obm.getObject("/unittestcontent/documents/unittestproject/common/homepage/homepage");

        assertTrue("Handle and Document should return true for equalCompare ", homePage.equalCompare(homePageAsWell));
        assertFalse("Folder and Document should return false for equalCompare ",folder.equalCompare(homePageAsWell));
        
        assertNotNull(homePage);
        assertNotNull(homePage.getNode());
    }
    
    @Test
    public void testSimpleObjectQuery() throws Exception {
        ObjectConverter objectConverter = getObjectConverter();

        ObjectBeanManager obm = new ObjectBeanManagerImpl(session, objectConverter);
        
        HippoFolder folder = (HippoFolder) obm.getObject("/unittestcontent/documents/unittestproject/common");
        
        HstQueryManager queryManager = new HstQueryManagerImpl(session, objectConverter, null);
        
        HstQuery hstQuery = queryManager.createQuery(folder);

        String query = "homepage";
        Filter filter = new FilterImpl(session, DateTools.Resolution.DAY);
        filter.addContains(".", query);
        hstQuery.setFilter(filter);
        
        List<HippoBean> resultBeans = doQuery(hstQuery);
        assertFalse("The query cannot find any result with '" + query + "'.", resultBeans.isEmpty());
        
        query = "is";
        filter = new FilterImpl(session, DateTools.Resolution.DAY);
        filter.addContains(".", query);
        hstQuery.setFilter(filter);
        
        resultBeans = doQuery(hstQuery);
        assertTrue("The query should not find any result with stopwords '" + query + "'.", resultBeans.isEmpty());
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
