/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.jcr.query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Session;

import org.hippoecm.repository.util.DateTools;
import org.onehippo.cms7.services.search.jcr.service.HippoJcrSearchService;
import org.onehippo.cms7.services.search.query.constraint.DateConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Filter {

    static final Logger log = LoggerFactory.getLogger(Filter.class);
    
    private StringBuilder jcrExpressionBuilder;

    private boolean negated = false;

    private final boolean fulltextWildcardPostfixEnabled;

    private final int fulltextWildcardPostfixMinLength;

    private final Session session;

    private final DateTools.Resolution defaultResolution = DateTools.Resolution.DAY;

    /**
     * AND and OR filters are evaluated at the end when #getJcrExpression is called.
     * This allows us to change those filters even after those are added to filter
     * @see #getJcrExpression()
     */
    private List<FilterTypeWrapper> childFilters = new ArrayList<>();

    private ChildFilterType firstAddedType;

    public Filter(final Session session) {
        this(session, HippoJcrSearchService.DEFAULT_WILDCARD_POSTFIX_ENABLED, HippoJcrSearchService.DEFAULT_WILDCARD_POSTFIX_MINLENGTH);
    }

    public Filter(final Session session, final boolean wildcardPostfixEnabled, final int wildcardPostfixMinLength) {
        this.session = session;
        this.fulltextWildcardPostfixEnabled = wildcardPostfixEnabled;
        this.fulltextWildcardPostfixMinLength = wildcardPostfixMinLength;
    }

    private enum ChildFilterType {
        OR, AND
    }

    public Filter negate(){
        this.negated = !negated;
        return this;
    }

    public void addContains(String scope, String fullTextSearch) throws JcrQueryException {
        scope = JcrQueryUtils.toXPathProperty(scope, true, "addContains", new String[]{"."});

        if (fullTextSearch == null) {
            throw new JcrQueryException("Not allowed to search on 'null'.");
        }

        final StringBuilder containsBuilder = new StringBuilder();
        // we rewrite a search for * into a more efficient search
        if("*".equals(fullTextSearch)) {
            if(".".equals(scope)) {
                // searching on * with scope '.' implies no extra filter: just return
                return;
            } else {
                // all we need is to garantuee that the property scope is present, because when it is, '*' will return a hit
                addNotNull(scope);
                return;
            }
        } else {
            String parsedText = FullTextSearchParser.fullTextParseCmsSimpleSearchMode(fullTextSearch, false);

            if (fulltextWildcardPostfixEnabled) {
                final String parsedTextWildcarded = FullTextSearchParser.fullTextParseCmsSimpleSearchMode(fullTextSearch, true, fulltextWildcardPostfixMinLength);
                if (parsedTextWildcarded.length() > 0) {
                    if (parsedText.length() > 0) {
                        containsBuilder.append("(");
                        addContainsToBuilder(containsBuilder,  scope, parsedText);
                        containsBuilder.append(" or ");
                        addContainsToBuilder(containsBuilder, scope, parsedTextWildcarded);
                        containsBuilder.append(")");
                    } else {
                        addContainsToBuilder(containsBuilder, scope, parsedTextWildcarded);
                    }
                } else if (parsedText.length() > 0) {
                    addContainsToBuilder(containsBuilder, scope, parsedText);
                }
            } else if (parsedText.length() > 0) {
                addContainsToBuilder(containsBuilder, scope, parsedText);
            }
            log.info("Translated fullTextSearch '{}' to function '{}' with wildcarding={} and minimum length={}.",
                    fullTextSearch, containsBuilder.toString(), fulltextWildcardPostfixEnabled, fulltextWildcardPostfixMinLength);
        }

        if (containsBuilder.length() > 0) {
            addExpression(containsBuilder.toString());
        }
    }

    private static void addContainsToBuilder(StringBuilder builder, String scope, String text) {
        builder.append("jcr:contains(").append(scope).append(",'").append(text).append("')");
    }

    private void addBetween(final String fieldAttributeName,final Calendar start,final Calendar end, final DateTools.Resolution resolution) throws JcrQueryException {
        if(start == null || end == null) {
            throw new JcrQueryException("Not allowed to search on 'null'.");
        }
        final String jcrExpression;
        if (resolution == DateTools.Resolution.MILLISECOND) {
            // EXACT RANGE
            final String xpathProperty= JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "addBetween");
            jcrExpression = "( " + xpathProperty + " >= "
                    + DateTools.createXPathConstraint(session, start)
                    + " and " + xpathProperty + " <= "
                    + DateTools.createXPathConstraint(session, end) + ")";
        } else {
            final String xpathProperty= JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "addBetween");
            final String xpathPropertyForResolution = DateTools.getPropertyForResolution(xpathProperty, resolution);
            jcrExpression = "( " + xpathPropertyForResolution + " >= "
                    + DateTools.createXPathConstraint(session, start, resolution)
                    + " and " + xpathPropertyForResolution + " <= "
                    + DateTools.createXPathConstraint(session, end, resolution) + ")";
        }
        addExpression(jcrExpression);
    }

    public void addBetween(String fieldAttributeName, Object value1, Object value2) throws JcrQueryException {
        if(value1 == null || value2 == null) {
            throw new JcrQueryException("Not allowed to search on 'null'.");
        }
        if ((value1 instanceof Calendar && value2 instanceof Calendar)) {
            addBetween(fieldAttributeName, (Calendar)value1, (Calendar)value2, defaultResolution);
            return;
        }
        if ((value1 instanceof Date && value2 instanceof Date)) {
            Calendar start = new GregorianCalendar();
            start.setTime((Date)value1);
            Calendar end = new GregorianCalendar();
            end.setTime((Date)value2);
            addBetween(fieldAttributeName, start, end, defaultResolution);
            return;
        }

        fieldAttributeName = JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "addBetween");
        final String jcrExpression = "( " + fieldAttributeName + " >= "
                + this.getStringValue(value1)
                + " and " + fieldAttributeName + " <= "
                + this.getStringValue(value2) + ")";
        addExpression(jcrExpression);
    }

    public void addBetween(final String fieldAttributeName, final Calendar start, final Calendar end, final DateConstraint.Resolution resolution) throws JcrQueryException {
        final DateTools.Resolution dateToolsResolution = JcrQueryUtils.getDateToolsResolution(resolution);
        if(start == null || end == null) {
            throw new JcrQueryException("Not allowed to search on 'null'.");
        }
        final String jcrExpression;
        if (dateToolsResolution == DateTools.Resolution.MILLISECOND) {
            // EXACT RANGE
            final String xpathProperty= JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "addBetween");
            jcrExpression = "( " + xpathProperty + " >= "
                    + DateTools.createXPathConstraint(session, start)
                    + " and " + xpathProperty + " <= "
                    + DateTools.createXPathConstraint(session, end) + ")";
        } else {
            final String xpathProperty= JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "addBetween");
            final String xpathPropertyForResolution = DateTools.getPropertyForResolution(xpathProperty, dateToolsResolution);
            jcrExpression = "( " + xpathPropertyForResolution + " >= "
                    + DateTools.createXPathConstraint(session, start, dateToolsResolution)
                    + " and " + xpathPropertyForResolution + " <= "
                    + DateTools.createXPathConstraint(session, end, dateToolsResolution) + ")";
        }
        addExpression(jcrExpression);
    }

    private void addConstraintWithOperator(final String fieldAttributeName,final Object value, final String operator, boolean isRangeConstraint) throws JcrQueryException {
        if(value == null ) {
            throw new JcrQueryException("Not allowed to search on 'null'.");
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
        final String xpathProperty = JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "operator : " + operator);
        final String jcrExpression = xpathProperty + operator + getStringValue(value);
        addExpression(jcrExpression);
    }

    private void addConstraintWithOperator(final String fieldAttributeName, final Calendar calendar, final DateTools.Resolution resolution, final String operator) throws JcrQueryException {
        if(calendar == null ) {
            throw new JcrQueryException("Not allowed to search on 'null'.");
        }
        final String jcrExpression;
        if (resolution == DateTools.Resolution.MILLISECOND) {
            final String xpathProperty = JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "equal");
            jcrExpression = xpathProperty + operator + DateTools.createXPathConstraint(session, calendar);
        } else {
            final String xpathProperty = JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "equal");
            final String xpathPropertyForResolution = DateTools.getPropertyForResolution(xpathProperty, resolution);
            jcrExpression = xpathPropertyForResolution + operator + DateTools.createXPathConstraint(session, calendar, resolution);
        }
        addExpression(jcrExpression);
    }

    public void addEqualTo(String fieldAttributeName, Object value) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, value, " = ", false);
    }

    public void addEqualTo(final String fieldAttributeName, final Calendar calendar, final DateConstraint.Resolution resolution) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, calendar, JcrQueryUtils.getDateToolsResolution(resolution), " = ");
    }

    public void addGreaterOrEqualThan(String fieldAttributeName, Object value) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, value, " >= ", true);
    }
    public void addGreaterOrEqualThan(final String fieldAttributeName, final Calendar calendar, final DateConstraint.Resolution resolution) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, calendar, JcrQueryUtils.getDateToolsResolution(resolution), " >= ");
    }

    public void addGreaterThan(String fieldAttributeName, Object value) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, value, " > ", true);
    }

    public void addGreaterThan(final String fieldAttributeName, final Calendar calendar, final DateConstraint.Resolution resolution) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, calendar, JcrQueryUtils.getDateToolsResolution(resolution), " > ");
    }
    
    public void addLessOrEqualThan(String fieldAttributeName, Object value) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, value, " <= ", true);
    }

    public void addLessOrEqualThan(final String fieldAttributeName, final Calendar calendar, final DateConstraint.Resolution resolution) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, calendar, JcrQueryUtils.getDateToolsResolution(resolution), " <= ");
    }

    public void addLessThan(String fieldAttributeName, Object value) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, value, " < ", true);
    }
    public void addLessThan(final String fieldAttributeName, final Calendar calendar, final DateConstraint.Resolution resolution) throws JcrQueryException {
        addConstraintWithOperator(fieldAttributeName, calendar, JcrQueryUtils.getDateToolsResolution(resolution), " < ");
    }

    public void addNotNull(String fieldAttributeName) throws JcrQueryException {
        fieldAttributeName = JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "addNotNull");
        String jcrExpression = fieldAttributeName;
        addExpression(jcrExpression);
    }
    
    public void addIsNull(String fieldAttributeName) throws JcrQueryException {
        fieldAttributeName = JcrQueryUtils.toXPathProperty(fieldAttributeName, true, "addIsNull");
        String jcrExpression = "not(" + fieldAttributeName + ")";
        addExpression(jcrExpression);
    }

    public Filter addOrFilter(Filter filter) throws JcrQueryException {
        if(firstAddedType == null) {
            firstAddedType = ChildFilterType.OR;
        } else if (firstAddedType == ChildFilterType.AND) {
            throw new JcrQueryException("Mixing AND and OR filters is not supported");
        }
        childFilters.add(new FilterTypeWrapper(filter, false));
        return this;
    }

    private void processOrFilter(Filter filter, StringBuilder builder){
        if(filter.getJcrExpression() == null || "".equals(filter.getJcrExpression())) {
            return;
        }
        if(builder.length() == 0) {
            builder.append("(").append(filter.getJcrExpression()).append(")");
        } else {
            builder.append(" or ").append("(").append(filter.getJcrExpression()).append(")");
        }
    }

    public Filter addAndFilter(Filter filter) throws JcrQueryException {
       if(firstAddedType == null) {
           firstAddedType = ChildFilterType.AND;
       } else if (firstAddedType == ChildFilterType.OR) {
           throw new JcrQueryException("Mixing AND and OR filters is not supported");
       }
       childFilters.add(new FilterTypeWrapper(filter, true));       
       return this;
    }

    private void processAndFilter(Filter filter, StringBuilder builder){
        if(filter.getJcrExpression() == null || "".equals(filter.getJcrExpression())) {
            return;
        }
        if(builder.length() == 0) {
            builder.append("(").append(filter.getJcrExpression()).append(")");
        } else {
            builder.append(" and ").append("(").append(filter.getJcrExpression()).append(")");
        }
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

    public String getStringValue(Object value) throws JcrQueryException {
        if(value instanceof String || value instanceof Boolean) {
            return "'" + value.toString() + "'";
        } else if(value instanceof Long || value instanceof Double || value instanceof Integer) {
            return value.toString();
        } else if(value instanceof Calendar){
            return DateTools.createXPathConstraint(session, (Calendar) value);
        } else if(value instanceof Date){
            Calendar cal = new GregorianCalendar();
            cal.setTime((Date)value);
            return  DateTools.createXPathConstraint(session, cal);
        }
        throw new JcrQueryException("Unsupported Object type '"+value.getClass().getName()+"' to query on.");
    }

    /**
     * Filter wrapper so we can distinguish between AND and or filters.
     * 
     */
    private static class FilterTypeWrapper {
        private boolean and;
        private Filter filter;

        FilterTypeWrapper(Filter filter, boolean and) {
            this.and = and;
            this.filter = filter;
        }

        private Filter getFilter() {
            return filter;
        }

        private boolean isAnd() {
            return and;
        }
    }

}
