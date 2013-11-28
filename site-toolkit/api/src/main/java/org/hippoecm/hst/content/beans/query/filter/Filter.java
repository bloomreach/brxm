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

import java.util.Calendar;
import java.util.Date;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.repository.util.DateTools;


public interface Filter extends BaseFilter {

    /**
     * Adds a fulltext search to this Filter. A fulltext search is a search on the indexed text of the <code>scope</code>. When the 
     * <code>scope</code> is just a <code><b>.</b></code>, the search will be done on the entire document. When the <code>scope</code> is 
     * for example <code><b>@myproject:title</b></code>, the free text search is done on this property only. You can also point to properties of 
     * child nodes, for example a scope like <code><b>myproject:paragraph/@myproject:header</b></code>
     * @param scope the scope to search in. <code>scope = "."</code> means searching in the entire node/document. <code>scope = "example:title"</code> only 
     * searches in the property "example:title". <code>scope</code> is also allowed to be a path to a property in a descendant node, for example  <code>scope = "address/example:street"</code>. The 
     * latter is equivalent to  <code>scope = "address/@example:street"</code> 
     * @param fullTextSearch the text to search on
     * @throws FilterException when <code>scope</code> or <code>fullTextSearch</code> is <code>null</code>
     */
    void addContains(String scope, String fullTextSearch) throws FilterException ;
    
    /**
     * The negated version of {@link #addContains(String, String)}
     * @see {@link #addContains(String, String)}
     * @param scope the scope to search in. <code>scope = "."</code> means searching in the entire node/document. <code>scope = "example:title"</code> only 
     * searches in the property "example:title". <code>scope</code> is also allowed to be a path to a property in a descendant node, for example  <code>scope = "address/example:street"</code>. The 
     * latter is equivalent to  <code>scope = "address/@example:street"</code> 
     * @param fullTextSearch the text to search on
     * @throws FilterException when <code>scope</code> or <code>fullTextSearch</code> is <code>null</code>
     */
    void addNotContains(String scope, String fullTextSearch) throws FilterException ;

    /**
     * <p>
     *      Adds a constraint that the value <code>fieldAttributeName</code> is between <code>value1</code> and <code>value2</code> (boundaries included).
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #addBetween(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param fieldAttributeName the name of the attribute, eg "hippo:lastModified"
     * @param value1 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @param value2 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code>, <code>value1</code> or <code>value2</code> are invalid types/values or one of them is <code>null</code>
     */
    void addBetween(String fieldAttributeName, Object value1, Object value2) throws FilterException ;

    /**
     * Adds a <b>FAST DATE RANGE</b> constraint that the Calendar value for <code>fieldAttributeName</code> is between <code>start</code> and <code>end</code> (boundaries included) BASED ON the
     * granularity <code>resolution</code>. Thus suppose the Resolution is <code>Resolution.DAY</code>, then results with the same DAY as value for <code>fieldAttributeName</code>
     * will be included. The higher the Resolution (year is highest) the better the performance!
     * @param fieldAttributeName the name of the attribute, eg "hippo:lastModified"
     * @param start the date to start from (including)
     * @param end the date to end  (including)
     * @param resolution the resolution to use to compare dates. The higher the Resolution (year is highest) the better the performan
     * @throws FilterException
     */
    void addBetween(String fieldAttributeName, Calendar start, Calendar end, DateTools.Resolution resolution) throws FilterException;

    /**
     * <p>
     *      Adds a constraint that the value <code>fieldAttributeName</code> is NOT between <code>value1</code> and <code>value2</code>,
     *      including NOT <code>value1</code> and <code>value2</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #addNotBetween(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param fieldAttributeName the name of the attribute, eg "example:date"
     * @param value1 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @param value2 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code>, <code>value1</code> or <code>value2</code> are invalid types/values or one of them is <code>null</code>
     */
    void addNotBetween(String fieldAttributeName, Object value1, Object value2) throws FilterException ;

    /**
     * @see {@link #addBetween(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution)} but now negated
     */
    void addNotBetween(String fieldAttributeName, Calendar start, Calendar end, DateTools.Resolution resolution) throws FilterException;

    /**
     * Adds a constraint that the value <code>fieldAttributeName</code> is equal to <code>value</code>
     * @param fieldAttributeName the name of the attribute, eg "example:author"
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code> or <code>value</code> is of invalid type/value or is <code>null</code>
     */
    void addEqualTo(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * Adds a constraint that the Calendar value for <code>fieldAttributeName</code> rounded to its resolution is equal to the
     * rounded value for <code>calendar</code>. Thus assume the <code>Resolution</code> is equal to <code>Resolution.DAY</code>,
     * then all nodes/documents where the property <code>fieldAttributeName</code> as a Calendar value with the <string>same</string>
     * date rounded to days (eg 20130128) has the same value as <code>calendar</code> rounded to days, will match.
     */
    void addEqualTo(String fieldAttributeName, Calendar calendar, DateTools.Resolution resolution) throws FilterException;
   
    /**
     * Adds a constraint that the value <code>fieldAttributeName</code> is NOT equal to <code>value</code>
     * @param fieldAttributeName the name of the attribute, eg "example:author"
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code> or  <code>value</code> is of invalid type/value or is <code>null</code>
     */
    void addNotEqualTo(String fieldAttributeName, Object value) throws FilterException;

    /**
     * @see {@link #addEqualTo(String, java.util.Calendar, DateTools.Resolution)} only now negated
     */
    void addNotEqualTo(String fieldAttributeName, Calendar calendar, DateTools.Resolution resolution) throws FilterException;

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is greater than or equal to <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #addGreaterOrEqualThan(String, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param fieldAttributeName the name of the attribute, eg "example:date"
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code> or  <code>value</code> is of invalid type/value or is <code>null</code>
     */
    void addGreaterOrEqualThan(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * @see {@link #addBetween(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     * but now no upper bound
     */
    void addGreaterOrEqualThan(String fieldAttributeName, Calendar calendar, DateTools.Resolution resolution) throws FilterException;

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is greater than <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #addGreaterThan(String, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param fieldAttributeName the name of the attribute, eg "example:date"
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code> or  <code>value</code> is of invalid type/value or is <code>null</code>
     */
    void addGreaterThan(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * @see {@link #addBetween(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     * but now no upper bound and lower bound not included
     */
    void addGreaterThan(String fieldAttributeName, Calendar calendar, DateTools.Resolution resolution) throws FilterException;

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is less than or equal to <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #addLessOrEqualThan(String, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param fieldAttributeName the name of the attribute, eg "example:date"
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code> or  <code>value</code> is of invalid type/value or is <code>null</code>
     */
    void addLessOrEqualThan(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * @see {@link #addBetween(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     * but now no lower bound
     */
    void addLessOrEqualThan(String fieldAttributeName, Calendar calendar, DateTools.Resolution resolution) throws FilterException;

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is less than <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #addLessThan(String, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param fieldAttributeName the name of the attribute, eg "example:date"
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @throws FilterException when <code>fieldAttributeName</code> or  <code>value</code> is of invalid type/value or is <code>null</code>
     */
    void addLessThan(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * @see {@link #addBetween(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     * but now no lower bound and upper bound not included
     */
    void addLessThan(String fieldAttributeName, Calendar calendar, DateTools.Resolution resolution) throws FilterException;

    /**
     * <p>
     *      This function is based on the LIKE predicate found in SQL. This method maps to <code>jcr:like</code> as
     *      <code>jcr:like($property as attribute(), $pattern as xs:string)</code>. Also see JCR spec 1.0 6.6.5.1 jcr:like
     * </p>
     * <p>
     *     <strong>usage:</strong> For example, the query “Find all documents whose <code>myproject:title</code> property starts with
     *     <code>hip</code>”,  is expressed as: <code>addLike("myproject:title","hip%")</code>.
     *     The <code>%</code> after <code>hip</code> is the wildcard.
     * </p>
     * <p>
     *     This method is particularly helpful in <i>key</i> kind of fields, where the <i>key</i> values contain chars on which
     *     Lucene text indexing tokenizes. For example, "give me all the documents that have a key that start with JIRA key
     *     <code>HSTTW0-23</code>" can be expressed as <code>addLike("myproject:key","HSTTW0-23%")</code>.
     *     This results in documents having key <code>HSTTW0-2345</code>, <code>HSTTW0-2357</code>, etc.
     * </p>
     * <p>
     *     <strong>DO NOT USE '%' AS A PREFIX</strong>. Thus do not use a query like
     *     <code>addLike("myproject:key","%HSTTW0-23%")</code>. Note the prefix '%'. Prefix wildcards blow up in memory and CPU
     *     as they cannot be efficiently done in Lucene.
     * </p>
     * @param fieldAttributeName the name of the attribute, eg "example:author"
     * @param value object that must be of type String
     * @throws FilterException when <code>fieldAttributeName</code> or  <code>value</code> is of invalid type/value or is <code>null</code>
     */
    void addLike(String fieldAttributeName, String value) throws FilterException ;

    /**
     * @see {@link #addLike(String, String)}
     * @deprecated since 7.9.0 use {@link #addLike(String, String)} instead as the Object param was stringified anyway
     */
    @Deprecated
    void addLike(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * @see {@link #addLike(String, String)} only now inverted
     *
     */
    void addNotLike(String fieldAttributeName, String value) throws FilterException ;

    /**
     * @see {@link #addLike(String, String)} only now inverted
     *
     * @deprecated since 7.9.0 use {@link #addNotLike(String, String)} instead as the Object param was stringified anyway
     */
    @Deprecated
    void addNotLike(String fieldAttributeName, Object value) throws FilterException ;

    /**
     * Add a constraint that the result <b>does</b> have the property <code>fieldAttributeName</code>, regardless its value
     * @param fieldAttributeName the name of the attribute, eg "example:author"
     * @throws FilterException when <code>fieldAttributeName</code> is <code>null</code>
     */
    void addNotNull(String fieldAttributeName) throws FilterException ;

    /**
     * Add a constraint that the result <b>does NOT</b> have the property <code>fieldAttributeName</code>
     * @param fieldAttributeName the name of the attribute, eg "example:author"
     * @throws FilterException when <code>fieldAttributeName</code> is <code>null</code>
     */
    void addIsNull(String fieldAttributeName) throws FilterException ;
    
    /**
     * Adds the xpath <code>jcrExpression</code> as constraint. See jsr-170 spec for the xpath format 
     * @param jcrExpression
     */
    void addJCRExpression(String jcrExpression);
    
    /**
     * @param filter to OR added
     * @return the current filter
     */
    Filter addOrFilter(BaseFilter filter);

    /**
     * 
     * @param filter to AND added
     * @return the current filter
     */
    Filter addAndFilter(BaseFilter filter);

    /**
     * negates the current filter
     */
    Filter negate();
}
