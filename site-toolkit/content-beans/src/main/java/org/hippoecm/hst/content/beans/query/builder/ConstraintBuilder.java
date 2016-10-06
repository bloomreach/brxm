/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query.builder;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.repository.util.DateTools;

public abstract class ConstraintBuilder {

    private boolean negated;

    protected ConstraintBuilder() {
    }

    /**
     * The {@code fieldName} is in general a property name, for example <em>example:title</em>. Depending on the
     * filter method ({@link ConstraintBuilder#equalTo}, {@link ConstraintBuilder#notEqualTo}, {@link ConstraintBuilder#contains}, etc)
     * the {@code fieldName} is limited to certain conditions. For <strong>all</strong> {@link ConstraintBuilder} constraints
     * methods the fieldName is allowed to be of the form <em>myhippo:title</em> or <em>address/myhippo:street</em> where the
     * latter is a constraint on a child node its property. There is one exception: When you use {@link ConstraintBuilder#like(String)}
     * or {@link ConstraintBuilder#notLike(String)}, it is not allowed to do the constrain on a child node property.
     * <p>
     * For the constraint {@link ConstraintBuilder#contains(String)} and {@link ConstraintBuilder#notContains(String)} the
     * {@code fieldName} can next to <em>myhippo:title</em> or <em>address/myhippo:street</em> also be equal to
     * "<em>.</em>" meaning a text constraint on node scope (instead of property) level is done
     * </p>
     * <p>
     *     When you do not invoke a explicit {@link ConstraintBuilder} method, for example {@link ConstraintBuilder#equalTo(Object)},
     *     but just use something like {@code constraint("myproject:title")} in the {@link HstQueryBuilder#where(ConstraintBuilder)},
     *     the {@link ConstraintBuilder} falls back to a property must exist constraint, thus {@link ConstraintBuilder#exists()}.
     * </p>
     * <p>
     *     If you do invoke an explicit {@link ConstraintBuilder} method, for example {@link ConstraintBuilder#equalTo(Object)},
     *     but the argument is {@code null}, the effect is that this {@link ConstraintBuilder} in {@link HstQueryBuilder#where(ConstraintBuilder)}
     *     is ignored. This way, when using the fluent api, you can skip null checks. For example
     *     <pre>
     *         .where(
                    constraint(".").contains(query)
               )
     *     </pre>
     *     will result in the constraint to be skipped if the {@code query} turns out to be {@code null}.
     * </p>
     * @param fieldName the {@code fieldName} this filter operates on, not allowed to be {@code null}
     * @return a new FilterBuilder for {@code fieldName}
     */
    public static ConstraintBuilder constraint(String fieldName) {
        FieldConstraintBuilder filterBuilder = new FieldConstraintBuilder(fieldName);
        return filterBuilder;
    }

    public static ConstraintBuilder and(ConstraintBuilder... constraintBuilders) {
        AndConstraintBuilder filterBuilder = new AndConstraintBuilder(constraintBuilders);
        return filterBuilder;
    }

    public static ConstraintBuilder or(ConstraintBuilder... constraintBuilders) {
        OrConstraintBuilder filterBuilder = new OrConstraintBuilder(constraintBuilders);
        return filterBuilder;
    }

    public final Filter build(final Session session, final DateTools.Resolution defaultResolution) throws FilterException {
        Filter filter = doBuild(session, defaultResolution);

        if (filter != null && negated) {
            filter.negate();
        }

        return filter;
    }

    protected abstract Filter doBuild(final Session session, final DateTools.Resolution defaultResolution) throws FilterException;

    /**
     * Negates the current filter
     */
    public ConstraintBuilder negate() {
        this.negated = !negated;
        return this;
    }

    /**
     * Adds a constraint that the value <code>fieldAttributeName</code> is equal to <code>value</code>
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder equalTo(Object value);

    /**
     * <p>
     * Adds a constraint that the Calendar value for <code>fieldAttributeName</code> rounded to its resolution is equal to the
     * rounded value for <code>calendar</code>. Thus assume the <code>Resolution</code> is equal to <code>Resolution.DAY</code>,
     * then all nodes/documents where the property <code>fieldAttributeName</code> as a Calendar value with the <string>same</string>
     * date rounded to days (eg 20130128) has the same value as <code>calendar</code> rounded to days, will match.
     * </p>
     * <p>
     * supported resolutions are
     * {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     * {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * </p>
     * @param value The {@link Calendar} value constraint that the results should be equal to. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param dateResolution the resolution to use to compare dates. The higher the Resolution (year is highest) the better the performance.
     *                   supported resolutions are
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder equalTo(Calendar value, DateTools.Resolution dateResolution);

    /**
     * Case insensitive testing of <code>fieldAttributeName</code> for some <code>value</code>.
     * @see #equalTo(Object) same as equalTo(Object) only now the equality is checked
     * case insensitive and the value can only be of type <code>String</code>
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder equalToCaseInsensitive(String value);

    /**
     * Adds a constraint that the value <code>fieldAttributeName</code> is NOT equal to <code>value</code>
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder notEqualTo(Object value);

    /**
     * Case insensitive testing of <code>fieldAttributeName</code> for some <code>value</code>.
     * @see #notEqualTo(Object) same as notEqualTo(Object) only now the inequality is checked
     * case insensitive and the value can only be of type {@link Calendar}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder notEqualTo(Calendar value, DateTools.Resolution dateResolution);

    /**
     * <p><strong>note:</strong> supported resolutions are
     * {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     * {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * </p>
     * @see #equalTo(java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)
     * equalTo(java.util.Calendar, DateTools.Resolution) only now negated
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder notEqualToCaseInsensitive(String value);

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is greater than or equal to <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #greaterOrEqualThan(java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder greaterOrEqualThan(Object value);

    /**
     * <p><strong>note:</strong> supported resolutions are
     * {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     * {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * </p>
     * @see #between(java.util.Calendar, java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)
     * between(java.util.Calendar, java.util.Calendar, DateTools.Resolution) but now no upper bound
     * @param value {@link Calendar} object. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param dateResolution the resolution to use to compare dates. The higher the Resolution (year is highest) the better the performance.
     *                   supported resolutions are
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder greaterOrEqualThan(Calendar value, DateTools.Resolution dateResolution);

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is greater than <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #greaterThan(java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder greaterThan(Object value);

    /**
     * <p><strong>note:</strong> supported resolutions are
     * {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     * {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * </p>
     * @see #between(java.util.Calendar, java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)
     * between(java.util.Calendar, java.util.Calendar, DateTools.Resolution) but now no upper bound and lower bound not included
     * @param value {@link Calendar} object. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param dateResolution the resolution to use to compare dates. The higher the Resolution (year is highest) the better the performance.
     *                   supported resolutions are
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder greaterThan(Calendar value, DateTools.Resolution dateResolution);

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is less than or equal to <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #lessOrEqualThan(java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder lessOrEqualThan(Object value);

    /**
     * <p><strong>note:</strong> supported resolutions are
     * {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     * {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * </p>
     * @see #between(java.util.Calendar, java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)
     * between(java.util.Calendar, java.util.Calendar, DateTools.Resolution) but now no lower bound
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder lessOrEqualThan(Calendar value, DateTools.Resolution dateResolution);

    /**
     * <p>
     * Adds a constraint that the value <code>fieldAttributeName</code> is less than <code>value</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #lessThan(java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param value object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder lessThan(Object value);

    /**
     * <p><strong>note:</strong> supported resolutions are
     * {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     * {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * </p>
     * @see #between(java.util.Calendar, java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)
     * between(java.util.Calendar, java.util.Calendar, DateTools.Resolution) but now no lower bound and upper bound not included
     * @param value {@link Calendar} object. If
     *        the parameter {@code value} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param dateResolution the resolution to use to compare dates. The higher the Resolution (year is highest) the better the performance.
     *                   supported resolutions are
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder lessThan(Calendar value, DateTools.Resolution dateResolution);

    /**
     * Adds a fulltext search to this Filter. A fulltext search is a search on the indexed text of the <code>scope</code>. When the
     * <code>scope</code> is just a <code><b>.</b></code>, the search will be done on the entire document. When the <code>scope</code> is
     * for example <code><b>@myproject:title</b></code>, the free text search is done on this property only. You can also point to properties of
     * child nodes, for example a scope like <code><b>myproject:paragraph/@myproject:header</b></code>
     * @param fullTextSearch the text to search on. If
     *        the parameter {@code fullTextSearch} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder contains(String fullTextSearch);

    /**
     * The negated version of {@link #contains(String)}
     * @see #contains(String)
     * @param fullTextSearch the text to search on. If
     *        the parameter {@code fullTextSearch} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder notContains(String fullTextSearch);

    /**
     * <p>
     *      Adds a constraint that the value <code>fieldAttributeName</code> is between <code>value1</code> and <code>value2</code> (boundaries included).
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #between(java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param value1 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value1} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param value2 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value2} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder between(Object value1, Object value2);

    /**
     * Adds a <b>FAST DATE RANGE</b> constraint that the Calendar value for <code>fieldAttributeName</code> is between <code>start</code> and <code>end</code> (boundaries included) BASED ON the
     * granularity <code>resolution</code>. Thus suppose the Resolution is <code>Resolution.DAY</code>, then results with the same DAY as value for <code>fieldAttributeName</code>
     * will be included. The higher the Resolution (year is highest) the better the performance!
     * @param start the date to start from (including). If
     *        the parameter {@code start} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param end the date to end  (including). If
     *        the parameter {@code end} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param dateResolution the resolution to use to compare dates. The higher the Resolution (year is highest) the better the performance.
     *                   supported resolutions are
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder between(Calendar start, Calendar end, DateTools.Resolution dateResolution);

    /**
     * <p>
     *      Adds a constraint that the value <code>fieldAttributeName</code> is NOT between <code>value1</code> and <code>value2</code>,
     *      including NOT <code>value1</code> and <code>value2</code>
     * </p>
     * <p>
     *     <strong>note</strong> that for range queries on calendar/date instances where the granularity of, say Day, is enough, you
     *     <strong>should</strong> use {@link #notBetween(java.util.Calendar, java.util.Calendar, DateTools.Resolution)}
     *     with the highest resolution that is acceptable for your use case, as this performs much better, OR make sure
     *     that your application runs with a default resolution set to for example 'day'
     * </p>
     * @param value1 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value1} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param value2 object that must be of type String, Boolean, Long, Double, {@link Calendar} or {@link Date}. If
     *        the parameter {@code value2} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder notBetween(Object value1, Object value2);

    /**
     * <p><strong>note:</strong> supported resolutions are
     * {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     * {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     * {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * </p>
     * @see #between(java.util.Calendar, java.util.Calendar, org.hippoecm.repository.util.DateTools.Resolution)
     *      between(String, java.util.Calendar, java.util.Calendar, DateTools.Resolution) but now negated
     * @param start the date to start from (including). If
     *        the parameter {@code start} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param end the date to end  (including). If
     *        the parameter {@code end} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @param dateResolution the resolution to use to compare dates. The higher the Resolution (year is highest) the better the performance.
     *                   supported resolutions are
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#YEAR},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#MONTH},
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#DAY} or
     *                   {@link org.hippoecm.repository.util.DateTools.Resolution#HOUR}
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder notBetween(Calendar start, Calendar end, DateTools.Resolution dateResolution);

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
     * @param value object that must be of type String. If
     *        the parameter {@code start} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder like(String value);

    /**
     * @see #like(String) only now inverted
     * @param value object that must be of type String. If
     *        the parameter {@code start} is {@code null}, this {@link ConstraintBuilder} is ignored (unless another
     *        constraint method is invoked without {@code null} value.
     * @return this {@link ConstraintBuilder}
     *
     */
    public abstract ConstraintBuilder notLike(String value);

    /**
     * Add a constraint that the result <b>does</b> have the property <code>fieldAttributeName</code>, regardless its value
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder exists();

    /**
     * Add a constraint that the result <b>does NOT</b> have the property <code>fieldAttributeName</code>
     * @return this {@link ConstraintBuilder}
     */
    public abstract ConstraintBuilder notExists();

    /**
     * Adds the xpath <code>jcrExpression</code> as constraint. See jsr-170 spec for the xpath format
     * @return this {@link ConstraintBuilder}
     * @param jcrExpression the {@code jcrExpression} to include in this
     */
    public abstract ConstraintBuilder jcrExpression(String jcrExpression);

}
