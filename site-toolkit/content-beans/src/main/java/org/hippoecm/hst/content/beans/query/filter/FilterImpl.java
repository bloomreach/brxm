/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterImpl implements Filter {

    private static final Logger log = LoggerFactory.getLogger(FilterImpl.class);

    private StringBuilder jcrExpressionBuilder;
    
    private boolean negated = false;

    private final Session session;
    private final DateTools.Resolution defaultResolution;
    
    /**
     * AND and OR filters are evaluated at the end when #getJcrExpression is called.
     * This allows us to change those filters even after those are added to filter
     * @see #getJcrExpression()
     */
    private List<FilterTypeWrapper> childFilters = new ArrayList<FilterTypeWrapper>();
    
    private ChildFilterType firstAddedType;

    private enum ChildFilterType {
        OR, AND
    }

    /**
     * @param resolution supported resolutions are {@link DateTools.Resolution#YEAR}, {@link DateTools.Resolution#MONTH},
     *                   {@link DateTools.Resolution#DAY} or {@link DateTools.Resolution#MONTH}
     */
    public FilterImpl(final Session session, final DateTools.Resolution resolution) {
        this.session = session;
        if (resolution == null) {
            defaultResolution = DateTools.Resolution.MILLISECOND;
        } else {
            defaultResolution = resolution;
        }
    }

    public Filter negate(){
        this.negated = !negated;
        return this;
    }
    
    private void addContains(String scope,final String fullTextSearch, boolean isNot) throws FilterException{
        String jcrExpression;
        scope = toXPathProperty(scope, true, "addContains" , new String[]{"."});     
      
        if(fullTextSearch == null) {
            throw new FilterException("Not allowed to search on 'null'.");
        }

        String text = fullTextSearch;
        
        // we rewrite a search for * into a more efficient search
        if("*".equals(text)) {
              if(".".equals(scope)) {
                  // searching on * with scope '.' implies no extra filter: just return
                  return;
              } else {
                  // all we need is to garantuee that the property scope is present, because when it is, '*' will return a hit
                  this.addNotNull(scope);
                  return;
              }
        } else {
            text = SearchInputParsingUtils.removeLeadingWildCardsFromWords(text);
            if(!text.equals(fullTextSearch)) {
                log.warn("Replaced fullTextSearch '{}' with '{}' as " +
                		"it contained terms that started with a wildcard. Use '{}'.parse(...) to first parse the input.", new Object[]{fullTextSearch, text, SearchInputParsingUtils.class.getName()});
            }
        }
        
        jcrExpression = "jcr:contains(" + scope + ", '" + text+ "')";     
        
        if(isNot) {
            addNotExpression(jcrExpression);
        } else {
            addExpression(jcrExpression);
        }
    }
    

    public void addContains(String scope, String fullTextSearch) throws FilterException{
        addContains(scope, fullTextSearch, false);
    }
    
    public void addNotContains(String scope, String fullTextSearch) throws FilterException{
        addContains(scope, fullTextSearch, true);
    }

    private void addBetween(String fieldAttributeName, Object value1, Object value2, boolean isNot) throws FilterException {
        if(value1 == null || value2 == null) {
            throw new FilterException("Not allowed to search on 'null'.");
        }
        if ((value1 instanceof Calendar && value2 instanceof Calendar)) {
            addBetween(fieldAttributeName, (Calendar)value1, (Calendar)value2, defaultResolution, isNot);
            return;
        }
        if ((value1 instanceof Date && value2 instanceof Date)) {
            Calendar start = new GregorianCalendar();
            start.setTime((Date)value1);
            Calendar end = new GregorianCalendar();
            end.setTime((Date)value2);
            addBetween(fieldAttributeName, start, end, defaultResolution, isNot);
            return;
        }

        fieldAttributeName = toXPathProperty(fieldAttributeName, true, "addBetween");
        String jcrExpression = "( " + fieldAttributeName + " >= "
                + this.getStringValue(value1)
                + " and " + fieldAttributeName + " <= "
                + this.getStringValue(value2) + ")";

        if(isNot) {
            addNotExpression(jcrExpression);
        } else {
            addExpression(jcrExpression);
        }
    }

    private void addBetween(final String fieldAttributeName,final Calendar start,final Calendar end, final DateTools.Resolution resolution, final boolean isNot) throws FilterException {
        if(start == null || end == null) {
            throw new FilterException("Not allowed to search on 'null'.");
        }
        final String jcrExpression;
        if (resolution == DateTools.Resolution.MILLISECOND) {
            // EXACT RANGE
            final String xpathProperty= toXPathProperty(fieldAttributeName, true, "addBetween");
            jcrExpression = "( " + xpathProperty + " >= "
                    + DateTools.createXPathConstraint(session, start)
                    + " and " + xpathProperty + " <= "
                    + DateTools.createXPathConstraint(session, end) + ")";
        } else {
            final String xpathProperty= toXPathProperty(fieldAttributeName, true, "addBetween");
            final String xpathPropertyForResolution = DateTools.getPropertyForResolution(xpathProperty, resolution);
            jcrExpression = "( " + xpathPropertyForResolution + " >= "
                    + DateTools.createXPathConstraint(session, start, resolution)
                    + " and " + xpathPropertyForResolution + " <= "
                    + DateTools.createXPathConstraint(session, end, resolution) + ")";
        }
        if(isNot) {
            addNotExpression(jcrExpression);
        } else {
            addExpression(jcrExpression);
        }
    }


    public void addBetween(String fieldAttributeName, Object value1, Object value2) throws FilterException {
        addBetween(fieldAttributeName, value1, value2,false);
    }

    @Override
    public void addBetween(final String fieldAttributeName, final Calendar start, final Calendar end, final DateTools.Resolution resolution) throws FilterException {
        addBetween(fieldAttributeName, start, end, resolution, false);
    }

    public void addNotBetween(String fieldAttributeName, Object value1, Object value2) throws FilterException {
        addBetween(fieldAttributeName, value1, value2,true);
    }

    @Override
    public void addNotBetween(final String fieldAttributeName, final Calendar start, final Calendar end, final DateTools.Resolution resolution) throws FilterException {
        addBetween(fieldAttributeName, start, end, resolution, true);
    }

    private void addConstraintWithOperator(final String fieldAttributeName,final Object value, final String operator, boolean isRangeConstraint) throws FilterException{
        if(value == null ) {
            throw new FilterException("Not allowed to search on 'null'.");
        }
        if (isRangeConstraint) {
            // only for range queries we used defaultResolution, not for equals/notEquals
            if (value instanceof Calendar) {
                addConstraintWithOperator(fieldAttributeName, (Calendar)value, defaultResolution, operator);
                return;
            }
            if (value instanceof Date) {
                Calendar cal = new GregorianCalendar();
                cal.setTime((Date)value);
                addConstraintWithOperator(fieldAttributeName, cal, defaultResolution, operator);
                return;
            }
        }
        final String xpathProperty = toXPathProperty(fieldAttributeName, true, "operator : " + operator);
        final String jcrExpression = xpathProperty + operator + getStringValue(value);
        addExpression(jcrExpression);
    }

    private void addConstraintWithOperator(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution, final String operator) throws FilterException{
        if(calendar == null ) {
            throw new FilterException("Not allowed to search on 'null'.");
        }
        final String jcrExpression;
        if (resolution == DateTools.Resolution.MILLISECOND) {
            final String xpathProperty = toXPathProperty(fieldAttributeName, true, "equal");
            jcrExpression = xpathProperty + operator + DateTools.createXPathConstraint(session, calendar);
        } else {
            final String xpathProperty = toXPathProperty(fieldAttributeName, true, "equal");
            final String xpathPropertyForResolution = DateTools.getPropertyForResolution(xpathProperty, resolution);
            jcrExpression = xpathPropertyForResolution + operator + DateTools.createXPathConstraint(session, calendar, resolution);
        }
        addExpression(jcrExpression);
    }

    @Override
    public void addEqualTo(String fieldAttributeName, Object value) throws FilterException{
        addConstraintWithOperator(fieldAttributeName, value, " = ", false);
    }

    @Override
    public void addEqualTo(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution) throws FilterException {
        addConstraintWithOperator(fieldAttributeName, calendar, resolution, " = ");
    }

    @Override
    public void addNotEqualTo(String fieldAttributeName, Object value) throws FilterException{
        addConstraintWithOperator(fieldAttributeName, value, " != ", false);
    }

    @Override
    public void addNotEqualTo(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution) throws FilterException {
        addConstraintWithOperator(fieldAttributeName, calendar, resolution, " != ");
    }

    @Override
    public void addGreaterOrEqualThan(String fieldAttributeName, Object value) throws FilterException{
        addConstraintWithOperator(fieldAttributeName, value, " >= ", true);
    }

    @Override
    public void addGreaterOrEqualThan(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution) throws FilterException {
        addConstraintWithOperator(fieldAttributeName, calendar, resolution, " >= ");
    }

    @Override
    public void addGreaterThan(String fieldAttributeName, Object value) throws FilterException{
        addConstraintWithOperator(fieldAttributeName, value, " > ", true);
    }

    @Override
    public void addGreaterThan(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution) throws FilterException {
        addConstraintWithOperator(fieldAttributeName, calendar, resolution, " > ");
    }

    @Override
    public void addLessOrEqualThan(String fieldAttributeName, Object value) throws FilterException{
        addConstraintWithOperator(fieldAttributeName, value, " <= ", true);
    }

    @Override
    public void addLessOrEqualThan(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution) throws FilterException {
        addConstraintWithOperator(fieldAttributeName, calendar, resolution, " <= ");
    }

    @Override
    public void addLessThan(String fieldAttributeName, Object value) throws FilterException{
        addConstraintWithOperator(fieldAttributeName, value, " < ", true);
    }

    @Override
    public void addLessThan(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution) throws FilterException {
        addConstraintWithOperator(fieldAttributeName, calendar, resolution, " < ");
    }

    private void addLike(String fieldAttributeName, Object value, boolean isNot) throws FilterException{
        if(value == null ) {
            throw new FilterException("Not allowed to search on 'null'.");
        }
        if (value.toString().startsWith("%")) {
            log.warn("Method '{}' is used with value '{}' but like methods should not be used with a prefix wildcard '%' because this " +
                    "blows up queries memory and cpu wise.", (isNot ? "addNotLike" : "addLike"), value);

        }
        fieldAttributeName = toXPathProperty(fieldAttributeName, false, "addLike");
        String jcrExpression = "jcr:like(" + fieldAttributeName + ", '"
            + value + "')";
        if(isNot) {
            addNotExpression(jcrExpression);
        } else {
            addExpression(jcrExpression);
        }
    }

    @Override
    @Deprecated
    public void addLike(String fieldAttributeName, Object value) throws FilterException{
        addLike(fieldAttributeName, value, false);
    }

    @Override
    public void addLike(String fieldAttributeName, String value) throws FilterException{
        addLike(fieldAttributeName, value, false);
    }

    @Override
    public void addNotLike(String fieldAttributeName, String value) throws FilterException{
        addLike(fieldAttributeName, value, true);
    }
    @Override
     @Deprecated
     public void addNotLike(String fieldAttributeName, Object value) throws FilterException{
        addLike(fieldAttributeName, value, true);
    }

    @Override
    public void addNotNull(String fieldAttributeName) throws FilterException{
        fieldAttributeName = toXPathProperty(fieldAttributeName, true, "addNotNull");
        String jcrExpression = fieldAttributeName;
        addExpression(jcrExpression);
    }

    @Override
    public void addIsNull(String fieldAttributeName) throws FilterException{
        fieldAttributeName = toXPathProperty(fieldAttributeName, true, "addIsNull");
        String jcrExpression = "not(" + fieldAttributeName + ")";
        addExpression(jcrExpression);
    }

    @Override
    public void addJCRExpression(String jcrExpression) {
        addExpression(jcrExpression);
    }

    @Override
    public Filter addOrFilter(BaseFilter filter) {
        if(firstAddedType == null) {
            firstAddedType = ChildFilterType.OR;
        } else if (firstAddedType == ChildFilterType.AND) {
            log.warn("Mixing AND and OR filters within a single parent Filter: This results in ambiguous searches where the order of AND and OR filters matter");
        }
        childFilters.add(new FilterTypeWrapper(filter, false));
        return this;
    }

    private void processOrFilter(BaseFilter filter, StringBuilder builder){
        if(filter.getJcrExpression() == null || "".equals(filter.getJcrExpression())) {
            return;
        }
        if(builder.length() == 0) {
            builder.append("(").append(filter.getJcrExpression()).append(")");;
        } else {
            builder.append(" or ").append("(").append(filter.getJcrExpression()).append(")");
        }
    }

    @Override
    public Filter addAndFilter(BaseFilter filter) {
       if(firstAddedType == null) {
           firstAddedType = ChildFilterType.AND;
       } else if (firstAddedType == ChildFilterType.OR) {
           log.warn("Mixing AND and OR filters within a single parent Filter: This results in ambiguous searches where the order of AND and OR filters matter");
       }
       childFilters.add(new FilterTypeWrapper(filter, true));       
       return this;
    }

    private void processAndFilter(BaseFilter filter, StringBuilder builder){
        if(filter.getJcrExpression() == null || "".equals(filter.getJcrExpression())) {
            return;
        }
        if(builder.length() == 0) {
            builder.append("(").append(filter.getJcrExpression()).append(")");;
        } else {
            builder.append(" and ").append("(").append(filter.getJcrExpression()).append(")");
        }
    }
    
    private void addNotExpression(String jcrExpression){
        if(jcrExpression == null || "".equals(jcrExpression)) {
            return;
        }
        addExpression("not("+jcrExpression+")");
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

    @Override
    public String getJcrExpression() {
        // if we have AND or OR filters, we'll always have expression:
        
        StringBuilder originalExpr = jcrExpressionBuilder == null ?  null : new StringBuilder(jcrExpressionBuilder);
        StringBuilder childFiltersExpression = null;
        if (childFilters.size() > 0) {
             childFiltersExpression = new StringBuilder();
             processChildFilters(childFiltersExpression);
        }
        
        if(childFiltersExpression != null && childFiltersExpression.length() > 0) {
            if(jcrExpressionBuilder == null) {
                jcrExpressionBuilder = new StringBuilder(childFiltersExpression);
            } else {
                if(firstAddedType == ChildFilterType.AND) {
                    // and
                    jcrExpressionBuilder.append(" and ");
                } else {
                    // or
                    jcrExpressionBuilder.append(" or ");
                }
                jcrExpressionBuilder.append(childFiltersExpression);
            }
        }
        // no experssion, no filters, nothing to do:
        if (this.jcrExpressionBuilder == null) {
            return null;
        }
        if(this.negated) {
            String processedExpr = "not("+jcrExpressionBuilder.toString()+")";
            jcrExpressionBuilder = originalExpr == null ? null : new StringBuilder(originalExpr);
            return processedExpr;
        } else {
            String processedExpr = jcrExpressionBuilder.toString();
            jcrExpressionBuilder = originalExpr == null ? null : new StringBuilder(originalExpr);
            return processedExpr;
        }
        
    }

    /**
     * Process AND or OR filters
     * @return  jcr query expression  or null 
     */
    private void processChildFilters(StringBuilder childFiltersExpression) {
        for (FilterTypeWrapper filter : childFilters) {
            if (filter.isAnd()) {
                processAndFilter(filter.getFilter(), childFiltersExpression);
            } else {
                processOrFilter(filter.getFilter(), childFiltersExpression);
            }
        }
    }
    


    public String getStringValue(Object value) throws FilterException{
        if(value instanceof String || value instanceof Boolean) {
            return "'" + value.toString() + "'";
        } else if(value instanceof Long || value instanceof Double) {
            return value.toString();
        } else if(value instanceof Calendar){
            return DateTools.createXPathConstraint(session, (Calendar)value);
        } else if(value instanceof Date){
            Calendar cal = new GregorianCalendar();
            cal.setTime((Date)value);
            return DateTools.createXPathConstraint(session, cal);
        }
        throw new FilterException("Unsupported Object type '"+value.getClass().getName()+"' to query on.");
    }

    /**
     * BaseFilter wrapper so we can distinguish between AND and or filters.
     * 
     */
    private static class FilterTypeWrapper {
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
    
    String toXPathProperty(String path, boolean childAxisAllowed, String methodName) throws FilterException{
        return toXPathProperty(path, childAxisAllowed, methodName, null);
    }
            
    String toXPathProperty(String path, boolean childAxisAllowed, String methodName, String[] skips)  throws FilterException{
        if(path == null) {
            throw new FilterException("Scope is not allowed to be null for '"+methodName+"'");
        }
        if(skips != null) {
            for(String skip : skips) {
                if(skip.equals(path)) {
                    return path;
                }
            }
        }
        if(childAxisAllowed) {
            if(path.indexOf("/") > -1) {
                String[] parts = path.split("/");
                StringBuilder newPath = new StringBuilder();
                int i = 0;
                for(String part : parts) {
                    i++;
                    if(i == parts.length) {
                        if(i > 1) {
                            newPath.append("/");
                        }
                        if(!part.startsWith("@")) {
                            newPath.append("@"); 
                        }
                        newPath.append(part);
                    } else {
                        if(part.startsWith("@")) {
                            throw new FilterException("'@' in path only allowed for a property: invalid path: '"+path+"'");
                        }
                        if(i > 1) {
                            newPath.append("/");
                        }
                        newPath.append(part);
                    }
                }
                return newPath.toString();
            } else {
                if(path.startsWith("@")) {
                    return path;
                } else {
                    return "@"+path;
                } 
            }
        } else {
            if(path.indexOf("/") > -1) {
                throw new FilterException("Not allowed to use a child path for '"+methodName+"'. Invalid: '"+path+"'");
            } 
            if(path.startsWith("@")) {
                return path;
            } else {
                return "@"+path;
            }
        }
    }

}
