/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.repository;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;

/**

This interface is to be used internally only by the Hippo Repository.  It
defines how the engine that is responsible for faceted navigation interacts
with the remainder of the repository other than set-up.<p>

<DL><DT><B>Description</B></DT><DD>

The main responsibility of the search engine is to return a facet key-value
count for a authorized faceted query.
Given a result set after a query, the faceted key-value count is an
enumeration of the number of matching documents per facet key-value pair.<p>

The query result set starting off with, is split into multiple parts:
<dl>
<dt>Initial query</dt>
<dd>any query that remains the same any time this faceted navigation query is
run.  There may be multiple of such initial queries, but this will be a
limited set and they refer to different queries, not just different instances
of the query.</dd>
<dt>Faceted query</dt>
<dd>When browsing through a faceted search, certain choices which part of the
search tree is displayed are being made by the user.  Each time the user fixes
a facet-key to a certain value, the result set is limited to the documents
with that facet.</dd>
<dt>Authorization query</dt>
<dd>Even when the faceted search query is the same, the result set of the
query may be different if the query is performed with different authorization
credentials.  The authorization is also faceted based, but with the
authorization query a document is included in the result set not when all
specified facets have the indicated keys, but when at least one of the terms
listed in the authorization query matches.  The idea is that a user seeks for
a specific document (i.e. all terms must match in faceted search), but that a
user sees all documents for which it has a right in any search.</dd>
<dt>Open Query</dt>
<dd>Finally, a user may perform a specific search.  E.g. a full text search on
the remaining documents in the result set.  As this search is totally unbound,
open queries should not be common and may imply a significant performance
penalty.  Also it may be that only specific implementations of this interface
can operate with open-queries.
</dl><p>

A BNF description of the composed query is:

<pre nowrap>
start ::= <b>(</b> initial-query <b>)</b> authorization-query facets-query open-query ;
initial-query        ::= <i>any valid lucene query</i> ;
authorization-query  ::= <b>OR</b> <b>(</b> authorization-cont <b>)</b>
                      |  <i>empty</i> ;
authorization-cont   ::= facet-key <b>=</b> facet-value
                      |  facet-key <b>=</b> facet-value <b>OR</b> authorization-cont ;
facets-query         ::= <b>AND</b> facets-cont
                      |  <i>empty</i> ;
facets-cont          ::= facet-key <b>=</b> facet-value
                      |  facet-key <b>=</b> facet-value <b>AND</b> facets-cont ;
open-query           ::= <b>AND</b> <b>(</b> <i>any valid lucene query</i> <b>)</b>;
                      |  <i>empty</i> ;
</pre>

Given this query, the faceted navigation engine implementation is supposed to
return a mapping of classes of documents to a count how many documents match
the class.  The sum of this count can be larger that the result set, as
certain documents fall into multiple classes.  Classes are composed of the
possible facet key-value pairs.  A collection of these key-value pairs is
presented to the faceted engine in a map with an (initially) null reference to
an integer object.  The faceted search engine should fill this map with the
document count it has discovered.

<i>Optional extension:</i><p>

Certain implementations can evaluate not just a single faceted search, but
also look up the navigation structure of a faceted search of one or more
levels deeper.  To expose this result set, the operator of this interface may
provide a structure which represents the future faceted searches and the facet
count template that go with them.

</DD></DL>

 *
 * @version draft
 */

/**
This interface is to be used internally only by the Hippo Repository.  It
defines how the engine that is responsible for faceted navigation interacts
with the remainder of the repository other than set-up.<p>

<DL><DT><B>Description</B></DT><DD>

The main responsibility of the search engine is to return a facet key-value
count for a authorized faceted query.
Given a result set after a query, the faceted key-value count is an
enumeration of the number of matching documents per facet key-value pair.<p>

The query result set starting off with, is split into multiple parts:
<dl>
<dt>Initial query</dt>
<dd>any query that remains the same any time this faceted navigation query is
run.  There may be multiple of such initial queries, but this will be a
limited set and they refer to different queries, not just different instances
of the query.</dd>
<dt>Faceted query</dt>
<dd>When browsing through a faceted search, certain choices which part of the
search tree is displayed are being made by the user.  Each time the user fixes
a facet-key to a certain value, the result set is limited to the documents
with that facet.</dd>
<dt>Authorization query</dt>
<dd>Even when the faceted search query is the same, the result set of the
query may be different if the query is performed with different authorization
credentials.  The authorization is also faceted based, but with the
authorization query a document is included in the result set not when all
specified facets have the indicated keys, but when at least one of the terms
listed in the authorization query matches.  The idea is that a user seeks for
a specific document (i.e. all terms must match in faceted search), but that a
user sees all documents for which it has a right in any search.</dd>
<dt>Open Query</dt>
<dd>Finally, a user may perform a specific search.  E.g. a full text search on
the remaining documents in the result set.  As this search is totally unbound,
open queries should not be common and may imply a significant performance
penalty.  Also it may be that only specific implementations of this interface
can operate with open-queries.
</dl><p>

A BNF description of the composed query is:

<pre nowrap>
start ::= <b>(</b> initial-query <b>)</b> authorization-query facets-query open-query ;
initial-query        ::= <i>any valid lucene query</i> ;
authorization-query  ::= <b>AND</b> <b>(</b> authorization-cont <b>)</b>
                      |  <i>empty</i> ;
authorization-cont   ::= facet-key <b>=</b> facet-value
                      |  facet-key <b>=</b> facet-value <b>OR</b> authorization-cont ;
facets-query         ::= <b>AND</b> facets-cont
                      |  <i>empty</i> ;
facets-cont          ::= facet-key <b>=</b> facet-value
                      |  facet-key <b>=</b> facet-value <b>AND</b> facets-cont ;
open-query           ::= <b>AND</b> <b>(</b> <i>any valid lucene query</i> <b>)</b>;
                      |  <i>empty</i> ;
</pre>

Given this query, the faceted navigation engine implementation is supposed to
return a mapping of classes of documents to a count how many documents match
the class.  The sum of this count can be larger that the result set, as
certain documents fall into multiple classes.  Classes are composed of the
possible facet key-value pairs.  A collection of these key-value pairs is
presented to the faceted engine in a map with an (initially) null reference to
an integer object.  The faceted search engine should fill this map with the
document count it has discovered.

<i>Optional extension:</i><p>

Certain implementations can evaluate not just a single faceted search, but
also look up the navigation structure of a faceted search of one or more
levels deeper.  To expose this result set, the operator of this interface may
provide a structure which represents the future faceted searches and the facet
count template that go with them.

</DD></DL>

 * @version draft
 */
public interface FacetedNavigationEngine<Q extends FacetedNavigationEngine.Query, C extends FacetedNavigationEngine.Context> {

    /**
     * The Count class is used to encapsulate a simple integer count to be able
     * to store this into a datastructure such as a Map.  Instead of an
     * java.lang.Integer class, this class is mutable to allow in-place updates.
     * For instance, it is permissable for an implementor of this interface to
     * update the values previously returned in a view method call when being
     * notified of a change.
     */
    class Count {
        public Count(int initialCount) {
            count = initialCount;
        }

        public int count;

        @Override
        public int hashCode() {
            return count;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (! (obj instanceof Count)) {
                return false;
            }
            Count other = (Count) obj;
            if (count != other.count) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Count [count=" + count + "]";
        }
    }

    /** An instance of a Result class contains the matching documents of a faceted view.
     */
    abstract class Result implements Iterable<NodeId> {
        /**
         * Total number of matches (even if iterator will return only a few.
         * @return the number of hits
         */
        public abstract int length();

        /**
         * An iterator over the matched documents.  Returns NodeId's
         * @return an iterator over java.lang.String.
         */
        public abstract Iterator<NodeId> iterator();
    }

    /** An abstract class passed between invocations of the parse()
     * and #view methods.
     */
    abstract class Query {
        public static final char  DOCBASE_FILTER_DELIMITER = '\uFFFE';
        public static final char  FILTER_DELIMITER = '\uFFFD';
    }

    /**
     * An abstract class passed between invocations of the #prepare and #view
     * methods which can hold state on the authorization to be used.
     */
    class Context {
    }

    /**
     * The faceted navigation engine is informed when a new principal is set up
     * in the repository.  For instance when a user with unique set of
     * authorization queries logs in.  The engine can take appropriate measures
     * to reload information.
     *
     * @param subject The user identification, in some non-interpretable form other
     *                  than for equality and order.
     * @param initialQueries A list of initial queries later used in the #view methods
     * @throws RepositoryException
     * @see #unprepare(C)
     */

    public C prepare(String userId, Subject subject, List<Q> initialQueries, Session session)
            throws RepositoryException;

    /**
     * This method is called when a user logouts from the system.
     *
     * @see #prepare
     */
    public void unprepare(C context);

    /**
     * In order to inform the engine that the facet definition has changed, the
     * reload() method is invoked.  Changes involve both adding facets as well
     * as adding facet values to existing facets.
     * Note that this function is not invoked when the requiresReload() method
     * returns false.
     *
     * @param facetValues A map listing all possible facet-keys in as the key
     *                    values used in the map and for each key all possible
     *                    facet-values that are used.  In case of a facet value
     *                    who's values cannot be enumerated reasonably (e.g.
     *                    for an integer) the map will contain a <code>null</code>
     *                    value.  An empty array indicates no facet values in
     *                    use for the facet-key in the entry.
     *
     * @see #requiresReload()
     */
    public void reload(Map<Name, String[]> facetValues);

    /**
     * If the implementation of this interface requires to be informed of facet
     * definition changes, it must return a value of true from this method.
     * Otherwise the
     *
     * @return whether the module requires the use of reload()
     */
    public boolean requiresReload();

    /**
     * If the implementation of this interface requires to be informed of
     * changes to the facet set of a document, it must return a value of true
     * from this method.

     * Otherwise the manager of the module may choose to forgo calling the
     * notify methods in order to optimize for any administration
     *
     * @return whether the module requires the use of reload()
     */
    public boolean requiresNotify();

    /**
     * With the notify method, the engine responsible for faceted navigation is
     * informed that the item identified with the docId under faceted navigation
     * has been changed.  Both the old set of facet as well as the new set of
     * facets are presented to the engine, such that the engine can update its
     * internal information.
     *
     * @param docId     a non-interpretable string identification of the document
     * @param oldFacets the old set of facet values
     * @param newFacets the new set of facet values, not just the changed values
     */
    public void notify(String docId, Map<Name, String[]> oldFacets, Map<Name, String[]> newFacets);

    /**
     * The purge method is used to inform the engine that it should clear out
     * all its state.  This method is used in case of crash recovery for
     * instance.
     */
    public void purge();

   /**
     * This method is used to build a Query object from a query encoded in a
     * string.  The language used, either XPATH, SQL or other should match the
     * language expected by the interface implementation.
     * @throws IllegalArgumentException when the query cannot be parsed
     */
    public Q parse(String query) throws IllegalArgumentException;

    /**
     * The second view method is trimmed down version of the first one and used
     * not the obtained the facet-value counts of a faceted navigation query,
     *   but used when the actual resultset is required.  In this case.
     */
    public Result view(String queryName, Q initialQuery, C authorization, List<KeyValue<String, String>> facetsQuery,
            Q openQuery, Map<String, String> inheritedFilter, HitsRequested hitsRequested) throws IllegalArgumentException;

    /**
     * @see #view(String, org.hippoecm.repository.FacetedNavigationEngine.Query, org.hippoecm.repository.FacetedNavigationEngine.Context, List, List, org.hippoecm.repository.FacetedNavigationEngine.Query, Map, Map, HitsRequested) 
     */
    public Result view(String queryName, Q initialQuery, C authorization, List<KeyValue<String, String>> facetsQuery,
            Q openQuery, Map<String, Map<String, Count>> resultset, Map<String, String> inheritedFilter,
            HitsRequested hitsRequested) throws UnsupportedOperationException, IllegalArgumentException;

    /**
     * While the other methods in this interface are meant for informing the
     * engine on changes, the view method is used to query the faceted navigation
     * engine.
     *
     * @param queryName An abstract name of the query, which can be used for
     * identification and presentation purposes.
     * @param initialQuery The initial query as described in the description
     * of this interface.  This initial query may never be empty.
     * @param authorization A map from facet-key to facet-value pairs
     * representing the equality terms which should be OR'ed to compose the
     * authorization query as in the description of this interface.
     * @param facetsQuery A List from facet-key, facet-value pairs
     * representing the equality terms which should be AND'ed to compose the
     * facets query as in the description of this interface.  This map
     * may be empty.
     * @param rangeQuery A List from FacetRange's
     * representing the ranges that should be AND'ed to compose the
     * range query as in the description of this interface.  This map
     * may be empty or null.
     * @param openQuery The open query as described in the description of this
     * interface.  This may be <code>null</code> indicating no open search
     * query.  When not-<code>null</code> the implementation may throw an
     * UnsupportedOperationException.
     * @param resultset The faceted search structure to be filled in by the
     * engine implemenation.  Because this method is called, the map will
     * contain the facet-keys for which the faceted search should count the
     * documents per facet value.  The facet-keys map to an empty map.  This
     * empty map is to be filled in by the implementation with all possible
     * facet-values, mapping to the number of documents matching the key-value
     * class.
     * The implementation should fill in or update this entire structure.
     * @param inheritedFilter the filter that needs to be applied because of parent
     * having some filter, for example a facetselect
     * @param hitsRequested Whether the engine is requested to return a Result
     * objects from a Lucene search.
     * @return The Result object as returned by lucene when performing the
     * combined initial-, authorization-, facets-, and open-query in lucene.
     * If the faceted navigation search engine can perform this search as a
     * by-product of its faceted search counting, or at minor extra cost the
     * implementation may return the result set.  However the implementation
     * is not required to do so and may always return <code>null</code>.  The
     * hitsRequested parameter is set to <code>true</code> if the caller
     * actually wants this result set.  When hitsRequested is set to false,
     * the engine should return a result object which returns the correct
     * count as returned by the length() method, but where the iterator
     * returns <code>null</code>.
     */
    public Result view(String queryName, Q initialQuery, C authorization, List<KeyValue<String, String>> facetsQuery,
            List<FacetRange> rangeQuery, Q openQuery, Map<String, Map<String, Count>> resultset,
            Map<String, String> inheritedFilter, HitsRequested hitsRequested) throws UnsupportedOperationException, IllegalArgumentException;

    /**
     * Query the lucene index with an XPATH search.  This will query the JackRabbit
     * index, i.e. it does not use the Hippo facets.
     * 
     * @param statement
     * @param context
     * @return
     * @throws InvalidQueryException
     * @throws RepositoryException
     */
    public Result query(String statement, C context) throws InvalidQueryException, RepositoryException;

}
