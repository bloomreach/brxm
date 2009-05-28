package org.hippoecm.hst.content.beans.query.filter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.core.request.HstRequestContext;

public class FilterImpl implements Filter{

    private HstRequestContext hstRequestContext; 
    private StringBuilder jcrExpressionBuilder;
    /**
     * AND and OR filters are evaluated at the end when #getJcrExpression is called.
     * This allows us to change those filters even after those are added to filter
     * @see #processChildFilters()
     * @see #getJcrExpression()
     */
    private List<FilterTypeWrapper> childFilters = new ArrayList<FilterTypeWrapper>();
    
    public FilterImpl(HstRequestContext hstRequestContext){
        this.hstRequestContext = hstRequestContext;
    }

    public void addContains(String scope, String fullTextSearch) throws FilterException{
        String jcrExpression;
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

    public Filter addOrFilter(BaseFilter filter) {
        childFilters.add(new FilterTypeWrapper(filter, false));
        return this;
    }

    private void processOrFilter(BaseFilter filter){
        if(filter.getJcrExpression() == null || "".equals(filter.getJcrExpression())) {
            return;
        }
        if(this.jcrExpressionBuilder == null) {
            this.jcrExpressionBuilder = new StringBuilder(filter.getJcrExpression());
        } else {
            this.jcrExpressionBuilder.append(" or ").append(filter.getJcrExpression());
        }
    }

    public Filter addAndFilter(BaseFilter filter) {
       childFilters.add(new FilterTypeWrapper(filter, true));       
       return this;
    }

    private void processAndFilter(BaseFilter filter){
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
        // if we have and or filters, we'll always have expression:
        if (childFilters.size() > 0) {
             processChildFilters();
        }
        // no experssion, no filters, nothing to do:
        if (this.jcrExpressionBuilder == null) {
            return null;
        }
        return jcrExpressionBuilder.toString();
    }

    /**
     * Process AND or OR filters
     * @return  jcr query expression  or null 
     */
    private void processChildFilters() {
        for (FilterTypeWrapper filter : childFilters) {
            if (filter.isAnd()) {
                processAndFilter(filter.getFilter());
            } else {
                processOrFilter(filter.getFilter());
            }
        }
    }
    


    public String getStringValue(String fieldAttributeName, Object value) throws FilterException{
        if(value instanceof String || value instanceof Boolean) {
            return "'" + value.toString() + "'";
        } else if(value instanceof Long || value instanceof Double) {
            return value.toString();
        } else if(value instanceof Calendar){
            return getCalendarWhereXPath((Calendar)value);
        } else if(value instanceof Date){
            Calendar cal = new GregorianCalendar();
            cal.setTime((Date)value);
            return getCalendarWhereXPath(cal);
        }
        throw new FilterException("Unsupported Object type '"+value.getClass().getName()+"' to query on.");
    }
    
    public String getCalendarWhereXPath(Calendar value) throws FilterException{
        try {
            // TODO : is this a repository roundtrip over rmi? If so, cache locally created values?
            Value val =  this.hstRequestContext.getSession().getValueFactory().createValue(value);
            return "xs:dateTime('"+val.getString() + "')";
         } catch (RepositoryException e) {
             throw new FilterException("Repository Exception: " , e);
         }
    }

    /**
     * BaseFilter wrapper so we can distinguish between AND and or filters.
     * 
     */
    private class FilterTypeWrapper {
        private boolean and;
        private BaseFilter filter;

        FilterTypeWrapper(BaseFilter filter, boolean and) {
            this.and = and;
            this.filter = filter;
        }

        private BaseFilter getFilter() {
            return filter;
        }

        private boolean isAnd() {
            return and;
        }
    }

}
