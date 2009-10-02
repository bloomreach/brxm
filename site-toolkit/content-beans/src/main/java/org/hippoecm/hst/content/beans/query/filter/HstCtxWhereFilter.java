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
package org.hippoecm.hst.content.beans.query.filter;

import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.core.search.HstContextualizeException;
import org.hippoecm.hst.core.search.HstCtxWhereClauseComputer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstCtxWhereFilter implements BaseFilter{
    
    private static final Logger log = LoggerFactory.getLogger(HstCtxWhereFilter.class);
    
    private String jcrExpression;
    
    public HstCtxWhereFilter(HstCtxWhereClauseComputer hstCtxWhereClauseComputer, Node scope) throws FilterException {
       
        try {
           jcrExpression = hstCtxWhereClauseComputer.getCtxWhereClause(scope);
       } catch (HstContextualizeException e) {
           throw new FilterException("Exception while computing the context where clause", e);
       }
       if(jcrExpression == null) {
           /*
            *  not allowed to search when the jcrExpression == null, because the creation of the ctx where clause failed.
            *  If you would continue the search, unwanted search results might be shown
            */
           throw new FilterException("context where clause from the HstCtxWhereClauseComputer is not allowed to be null at this point.");
       }
    }

    public HstCtxWhereFilter(HstCtxWhereClauseComputer hstCtxWhereClauseComputer, List<Node> scopes,
            boolean skipInvalidScopes) throws FilterException{
        
        try {
            jcrExpression = hstCtxWhereClauseComputer.getCtxWhereClause(scopes, skipInvalidScopes);
        } catch (HstContextualizeException e) {
            throw new FilterException("Cannot create context where filter: ", e);
        }
    }

    public String getJcrExpression(){
        return this.jcrExpression;
    }


    
}
