package org.hippoecm.hst.ocm.query.impl;

import javax.jcr.Node;

import org.hippoecm.hst.core.request.HstRequestContext;

public class HstCtxWhereFilter {
    
    private String jcrExpression = "";
    
    public HstCtxWhereFilter(HstRequestContext requestContext, Node node) {
       this.jcrExpression = requestContext.getHstCtxWhereClauseComputer().getCtxWhereClause(node, requestContext);
    }

    public String getJcrExpression(){
        return this.jcrExpression;
    }
    
}
