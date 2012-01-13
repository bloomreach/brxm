/*
 *  Copyright 2011 - 2012 Hippo.
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
package org.hippoecm.hst.util;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentBeanUtils {

    private final static Logger log = LoggerFactory.getLogger(ContentBeanUtils.class);

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it
     *  You need to add a <code>linkPath</code>: this is that path, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with 'myproject:link/@hippo:docbase' or even 'wildcard/@hippo:docbase' or 
     * 'wildcard/wildcard/@hippo:docbase' where wildcard = *
     * 
     * @param bean The HippoDocumentBean that you have, and for which you want to find the other beans that have a link to it (incoming beans)
     * @param scope the scope (hierarchical location) to search below for 'incoming beans'
     * @param linkPath the path where the 'incoming beans' have there link (mirror) stored, for example at myns:links/@hippo:docbase
     * @param converter the ObjectConverter
     * @param beanMappingClass the type the 'incoming beans' should be of
     * @param includeSubTypes <code>true</code> when subtypes of beanMappingClass should be included in the result 
     * @return a HstQuery that contains the constraints for 'incoming beans' to your <code>bean</code>
     * @throws QueryException
     */
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, 
            String linkPath, ObjectConverter converter,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{
    
        List<String> linkPaths = new ArrayList<String>();
        linkPaths.add(linkPath);
        return createIncomingBeansQuery(bean, scope, linkPaths, converter, beanMappingClass, includeSubTypes);
    
    }
    
    
/**
 * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it
 *  You need to add a <code>depth</code>: this is the maximum depth, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with 'myproject:link/@hippo:docbase' is depth 1,
 *  'myproject:somecompound/myproject:link/@hippo:docbase' is depth 2
 * @param bean The HippoDocumentBean that you have, and for which you want to find the other beans that have a link to it (incoming beans)
 * @param scope the scope (hierarchical location) to search below for 'incoming beans'
 * @param depth the <code>depth</code> until which the links below the HippoDocuments you want to find can be.  Maximum depth is 4, when larger, a QueryException is thrown
 * @param converter the ObjectConverter
 * @param beanMappingClass the type the 'incoming beans' should be of
 * @param includeSubTypes <code>true</code> when subtypes of beanMappingClass should be included in the result 
 * @return a HstQuery that contains the constraints for 'incoming beans' to your <code>bean</code>
 * @throws QueryException when <code>depth</code> is larger than 4
 */
public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, int depth,
        ObjectConverter converter, Class<? extends HippoBean> beanMappingClass,
        boolean includeSubTypes) throws QueryException{
        if (depth < 0 || depth > 4) {
            throw new FilterException("Depth must be (including) between 0 and 4");
        }
        String path = "@hippo:docbase";
        List<String> linkPaths = new ArrayList<String>();
        linkPaths.add(path);
        for (int i = 1; i <= depth; i++) {
            path = "*/" + path;
            linkPaths.add(path);
        }
        return createIncomingBeansQuery(bean, scope, linkPaths, converter, beanMappingClass, includeSubTypes);
    }
    
    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * You need to add  <code>linkPaths</code>: these are the paths, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with 'myproject:link/@hippo:docbase' or even 'wildcard/@hippo:docbase' or 
     * 'wildcard/wildcard/@hippo:docbase' where wildcard = *
     * 
     * @param bean The HippoDocumentBean that you have, and for which you want to find the other beans that have a link to it (incoming beans)
     * @param scope the scope (hierarchical location) to search below for 'incoming beans'
     * @param linkPaths the paths where the 'incoming beans' have there link (mirror) stored, for example at {myns:links/@hippo:docbase, myns:alsolinks/@hippo:docbase }
     * @param converter the ObjectConverter
     * @param beanMappingClass the type the 'incoming beans' should be of
     * @param includeSubTypes <code>true</code> when subtypes of beanMappingClass should be included in the result 
     * @return a HstQuery that contains the constraints for 'incoming beans' to your <code>bean</code>
     * @throws QueryException
     */
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
            List<String> linkPaths, ObjectConverter converter,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        String canonicalHandleUUID = bean.getCanonicalHandleUUID();
        HstQuery query;
        try {
            ComponentManager compMngr = HstServices.getComponentManager();
            HstQueryManagerFactory hstQueryManagerFactory = (HstQueryManagerFactory)compMngr.getComponent(HstQueryManagerFactory.class.getName());
            query = hstQueryManagerFactory.createQueryManager(bean.getNode().getSession(), converter).createQuery(scope, beanMappingClass, includeSubTypes);
            Filter filter = query.createFilter();
            for (String linkPath : linkPaths) {
                Filter orFilter = query.createFilter();
                orFilter.addEqualTo(linkPath, canonicalHandleUUID);
                filter.addOrFilter(orFilter);
            }
            query.setFilter(filter);
            return query;
        } catch (RepositoryException e) {
            throw new QueryException("RepositoryException",e);
        }
        
    }

    /**
     * Returns a list of beans of type T (the same type as {@code beanMappingClass}) that have a (facet)link to the HippoDocumentBean {@code bean}. If no incoming beans are found, 
     * an <code>empty</code> list will be returned. 
     * 
     */
    public static <T extends HippoBean> List<T> getIncomingBeans(HstQuery query,
            Class<? extends HippoBean> beanMappingClass) throws QueryException {

        List<T> incomingBeans = new ArrayList<T>();

        HstQueryResult result = query.execute();
        HippoBeanIterator beans = result.getHippoBeans();
        while (beans.hasNext()) {
            T incomingBean = (T) beans.nextHippoBean();
            if(incomingBean == null) {
                continue;
            }
            if (!beanMappingClass.isAssignableFrom(incomingBean.getClass())) {
                // should not be possible
                log.warn("Found a bean not being of type or subtype of '{}'. Skip bean", beanMappingClass.getName());
                continue;
            }
            incomingBeans.add(incomingBean);
        }

        return incomingBeans;
    }
    
}
