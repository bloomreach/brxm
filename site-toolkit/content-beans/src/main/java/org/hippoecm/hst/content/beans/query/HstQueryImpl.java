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

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.LoggerFactory;

public class HstQueryImpl implements HstQuery {
   
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstQueryImpl.class);
   
    private HstRequestContext hstRequestContext;
    private ObjectConverter objectConverter;
    
    
    public HstQueryImpl(HstRequestContext hstRequestContext, ObjectConverter objectConverter) {
        this.hstRequestContext = hstRequestContext;
        this.objectConverter = objectConverter;
    }

    public void setScope(Node node) {
        
    }

    public void addOrderByAscending(String fieldNameAttribute) {
        
    }


    public void addOrderByDescending(String fieldNameAttribute) {
       
    }
    
    public void setLimit(int limit) {
        
    }


    public void setOffset(int offset) {
        
    }

    public BaseFilter getFilter() {
        return null;
    }


    public void setFilter(BaseFilter filter) {
        
    }


    public HstQueryResult execute() throws QueryException{
        String query = "//element(*,hippo:document)";
        try {
            QueryManager jcrQueryManager = this.hstRequestContext.getSession().getWorkspace().getQueryManager();
            Query jcrQuery = jcrQueryManager.createQuery(query, "xpath");
            return new HstQueryResultImpl(this.objectConverter, jcrQuery.execute());
        } catch (InvalidQueryException e) {
            throw new QueryException(e.getMessage(), e);
        } catch (LoginException e) {
           log.warn("LoginException. Return null : {}", e);
        } catch (RepositoryException e) {
            log.warn("RepositoryException. Return null :  {}", e);
        }
        return null;
    }


    
    
}
