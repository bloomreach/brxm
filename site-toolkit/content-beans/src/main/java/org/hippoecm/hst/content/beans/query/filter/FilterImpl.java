package org.hippoecm.hst.content.beans.query.filter;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;

public class FilterImpl implements Filter{

    StringBuilder jcrExpressionBuilder;
    
    public FilterImpl(){
        
    }
    
    public void addContains(String scope, String fullTextSearch) throws FilterException{
        String jcrExpression = null;
        if (scope.equals(".")) {
            jcrExpression = "jcr:contains(., '" + fullTextSearch + "')";
        }
        else {
            jcrExpression = "jcr:contains(@" + scope + ", '" + fullTextSearch+ "')";
        }

        addExpression(jcrExpression);
    }
   

    public void addBetween(String fieldAttributeName, Object value1, Object value2) throws FilterException {
        String jcrExpression = "( @" + fieldAttributeName + " >= "
        + this.getStringValue(fieldAttributeName, value1)
        + " and @" + fieldAttributeName + " <= "
        + this.getStringValue(fieldAttributeName, value2) + ")";

       addExpression(jcrExpression);
    }

    public void addEqualTo(String fieldAttributeName, Object value) throws FilterException{
        String jcrExpression = "@" + fieldAttributeName + " = "
        + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);
    }

    public void addNotEqualTo(String fieldAttributeName, Object value) throws FilterException{
        String jcrExpression = "@" + fieldAttributeName + " != "
        + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);
    }
    
    public void addGreaterOrEqualThan(String fieldAttributeName, Object value) throws FilterException{
        String jcrExpression = "@" + fieldAttributeName + " >= "
        + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);
    }

    public void addGreaterThan(String fieldAttributeName, Object value) throws FilterException{
        String jcrExpression = "@" + fieldAttributeName + " > "
        + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);
    }
    
    public void addLessOrEqualThan(String fieldAttributeName, Object value) throws FilterException{
        String jcrExpression = "@" + fieldAttributeName + " <= "
        + this.getStringValue(fieldAttributeName, value);
        
        addExpression(jcrExpression);
    }

    public void addLessThan(String fieldAttributeName, Object value) throws FilterException{
        String jcrExpression = "@" + fieldAttributeName + " < "
        + this.getStringValue(fieldAttributeName, value);
        addExpression(jcrExpression);
    }

    public void addLike(String fieldAttributeName, Object value) throws FilterException{
        String jcrExpression = "jcr:like(" + "@" + fieldAttributeName + ", '"
            + value + "')";
        addExpression(jcrExpression);
    }
    

    public void addNotNull(String fieldAttributeName) throws FilterException{
        String jcrExpression = "@" + fieldAttributeName;
        addExpression(jcrExpression);
    }
    
    public void addIsNull(String fieldAttributeName) throws FilterException{
        String jcrExpression = "not(@" + fieldAttributeName + ")";
        addExpression(jcrExpression);
    }

    public void addJCRExpression(String jcrExpression) {
        addExpression(jcrExpression);
    }

    public void addOrFilter(BaseFilter filter) {
        if(filter.getJcrExpression() == null || "".equals(filter.getJcrExpression())) {
            return;
        }
        if(this.jcrExpressionBuilder == null) {
            this.jcrExpressionBuilder = new StringBuilder(filter.getJcrExpression());
        } else {
            this.jcrExpressionBuilder.append(" or ").append(filter.getJcrExpression());
        }
    }

    public void addAndFilter(BaseFilter filter) {
       this.addExpression(filter.getJcrExpression());
    }

    
    private void addExpression(String jcrExpression) {
        if(jcrExpression == null || "".equals(jcrExpression)) {
            return;
        }
        if(this.jcrExpressionBuilder == null) {
            this.jcrExpressionBuilder = new StringBuilder(jcrExpression);
        } else {
            this.jcrExpressionBuilder.append(" and ").append(jcrExpression);
        }
    }
    
    public String getJcrExpression() {
       if(this.jcrExpressionBuilder == null) {
           return null;
       } 
       return jcrExpressionBuilder.toString();
    }

    // TODO support for Long, Int, Boolean, Calendar, etc etc
    private String getStringValue(String fieldAttributeName, Object value) throws FilterException{
        if(value instanceof String) {
            return "'" + value + "'";
        }
        throw new FilterException("Unsupported Object type '"+value.getClass().getName()+"' to query on.");
    }
    
}
