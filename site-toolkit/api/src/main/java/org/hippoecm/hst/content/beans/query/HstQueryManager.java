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

import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;

public interface HstQueryManager {

    /**
     * @param hstRequestContext
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope 
     */
    HstQuery createQuery(HstRequestContext hstRequestContext, Node scope) throws QueryException;
    

    /**
     * @param hstRequestContext
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope 
     */
    HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope) throws QueryException;
    
    /**
     * 
     * @param hstRequestContext
     * @param scope
     * @param varargs filterBean
     * @return a new <code>{@link HstQuery}</code> with scope and filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    public HstQuery createQuery(HstRequestContext hstRequestContext, HippoBean scope, Class<? extends HippoBean>... filterBean) throws QueryException;
    

}
