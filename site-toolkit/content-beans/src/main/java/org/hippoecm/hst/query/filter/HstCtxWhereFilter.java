package org.hippoecm.hst.query.filter;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.query.FilterException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.search.HstContextWhereClauseException;

public class HstCtxWhereFilter {
    
    private String jcrExpression;
    
    public HstCtxWhereFilter(HstRequestContext requestContext, Node node) throws FilterException {
        
       this.jcrExpression = null;
       
       try {
           jcrExpression = requestContext.getHstCtxWhereClauseComputer().getCtxWhereClause(node, requestContext);
       } catch (HstContextWhereClauseException e) {
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

    public String getJcrExpression(){
        return this.jcrExpression;
    }
    
}
