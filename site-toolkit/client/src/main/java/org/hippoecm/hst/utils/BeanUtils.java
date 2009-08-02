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
package org.hippoecm.hst.utils;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing utility methods for Beans
 * 
 */
public class BeanUtils {

    private final static Logger log = LoggerFactory.getLogger(BeanUtils.class);

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * You need to add a <code>linkPath</code>: this is that path, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with '/myproject:link/@hippo:docbase' or even 'wildcard/@hippo:docbase' or 
     * 'wildcard/wildcard/@hippo:docbase' where wildcard = *
     * 
     */

    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, 
            String linkPath, BaseHstComponent component,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        List<String> linkPaths = new ArrayList<String>();
        linkPaths.add(linkPath);
        return createIncomingBeansQuery(bean, scope, linkPaths, component, beanMappingClass, includeSubTypes);
    }

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * The depth indicates how many child nodes deep is searched for a link to the HippoDocumentBean {@code bean}. The depth is allowed to range from 0 (direct hippo:docbase) to 4 (at most 4 levels deep searching is done)
     */

    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, int depth,
            BaseHstComponent component, Class<? extends HippoBean> beanMappingClass,
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
        return createIncomingBeansQuery(bean, scope, linkPaths, component, beanMappingClass, includeSubTypes);
    }

    /**
     * Returns a HstQuery for incoming beans. You can add filters and ordering to the query before executing it 
     * 
     * List<String> linkPaths is the list of paths that are searched that might have a link to the HippoDocumentBean {@code bean}. For example {/myproject:link/@hippo:docbase, /myproject:body/hippostd:content/@hippo:docbase}
     */
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
            List<String> linkPaths, BaseHstComponent component,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        String canonicalHandleUUID = bean.getCanonicalHandleUUID();

        HstQuery query = component.getQueryManager().createQuery(scope, beanMappingClass, includeSubTypes);
        Filter filter = query.createFilter();
        for (String linkPath : linkPaths) {
            Filter orFilter = query.createFilter();
            orFilter.addEqualTo(linkPath, canonicalHandleUUID);
            filter.addOrFilter(orFilter);
        }
        query.setFilter(filter);
        return query;
    }

    
    /**
     * Returns a list of beans of type T (the same type as {@code beanMappingClass}) that have a (facet)link to the HippoDocumentBean {@code bean}. If no incoming beans are found, 
     * an <code>empty</code> list will be returned. 
     * 
     * List<String> linkPaths is the list of paths that are searched that might have a link to the HippoDocumentBean {@code bean}. For example {/myproject:link/@hippo:docbase, /myproject:body/hippostd:content/@hippo:docbase}
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
