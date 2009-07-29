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
package org.hippoecm.hst.content.beans.query;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;

public interface HstQueryManager {

    /**
     * Creates a empty query, with scope
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope {@code scope}
     */
    HstQuery createQuery(Node scope) throws QueryException;
    
    /**
     * Creates a query, with scope  and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     * @param scope
     * @param filterBean
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope & filter
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;

    /**
     * Creates a empty query, with scope
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope {@code scope}
     */ 
    HstQuery createQuery(HippoBean scope) throws QueryException;
    
    /**
     * Creates a query, with scope HippoBean and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     * @param scope
     * @param filterBean
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope & filter
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;
    
    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied filterBeans. If needed, use 
     * {@link #createQuery(HstRequestContext, HippoBean, Class, boolean)} instead.
     * 
     * @param scope
     * @param filterBeans
     * @return a new <code>{@link HstQuery}</code> with scope & filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, Class<? extends HippoBean>... filterBeans) throws QueryException;
    
    /**
     * Creates a query, with a scope and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments. It is not possible to retrieve subtypes of the applied filterBeans with this method. If needed, use 
     * {@link #createQuery(HstRequestContext, Node, Class, boolean)} instead.
     * 
     * @param scope
     * @param varargs filterBean
     * @return a new <code>{@link HstQuery}</code> with scope and filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, Class<? extends HippoBean>... filterBean) throws QueryException;
    
    /**
     * @Deprecated Use {@link #createQuery(Node)}
     */
    @Deprecated
    HstQuery createQuery(HstRequestContext hstRequestContext, Node scope) throws QueryException;
    
    /**
     * @deprecated  Use {@link #createQuery(Node,HippoBean ,boolean)}
     */
    @Deprecated
    HstQuery createQuery(HstRequestContext hstRequestContext, Node scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;

    /**
     * @deprecated Use {@link #createQuery(HippoBean)}
     */ 
    @Deprecated 
    HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope) throws QueryException;
    
    /**
     * @Deprecated Use {@link #createQuery(HippoBean, HippoBean , boolean)}
     */
    @Deprecated
    HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;
    
    /**
     * @Deprecated Use {@link #createQuery(Node, HippoBean... )}
     */
    @Deprecated
    HstQuery createQuery(HstRequestContext hstRequestContext, Node scope, Class<? extends HippoBean>... filterBeans) throws QueryException;
    
    /**
     * @Deprecated Use {@link #createQuery(HippoBean, HippoBean... )}
     */
    @Deprecated
    HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope, Class<? extends HippoBean>... filterBean) throws QueryException;
    

}
