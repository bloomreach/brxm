/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.repository;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

public interface FacetedNavigationEngine<Q extends FacetedNavigationEngine.Query,C extends FacetedNavigationEngine.Context>
{
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
      public String toString() {
        return Integer.toString(count);
      }
    }

    public static class HitsRequested {
        /**
         * Wether results should be returned at all.
         */
        private boolean resultRequested;

        /**
         * How many results should be returned.  Defaults to 10, large values imply slow responses.
         */    
        private int limit = 10;

        /**
         * The offset in the resultset to start from.
         */
        private int offset = 0;
    
        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public boolean isResultRequested() {
            return resultRequested;
        }

        public void setResultRequested(boolean resultRequested) {
            this.resultRequested = resultRequested;
        }
    }


    /** An instance of a Result class contains the matching documents of a faceted view.
     */
    abstract class Result {
      /**
       * Total number of matches (even if iterator will return only a few.
       * @return the number of hits
       */
      public abstract int length();
      /**
       * An iterator over the matched documents.  Returns either the Path or
       * the JCR UUID (to be decided on).
       * @return an iterator over java.lang.String.
       */
      public abstract Iterator<String> iterator();
    }

    /** An abstract class passed between invocations of the parse()
     * and #view methods.
     */
    abstract class Query {
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
     * @param principal The user identification, in some non-interpretable form other
     *                  than for equality and order.
     * @param authorizationQuery The authorization part used by the principal
     * @param initialQueries A list of initial queries later used in the #view methods
     * @see #unprepare(C)
     */
    public C prepare(String principal, Map<String,String[]> authorizationQuery, List<Q> initialQueries, Session session);
    
    /**
     * This method is called when a user logouts from the system.
     *
     * @param principal The user identification, in some non-interpretable form other
     *                  than for equality and order.
     * @see #prepare(String,Map,List)
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
    public void reload(Map<String,String[]> facetValues);
    
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
    public void notify(String docId, Map<String,String[]> oldFacets, Map<String,String[]> newFacets);
    
    /**
     * The purge method is used to inform the engine that it should clear out
     * all its state.  This method is used in case of crash recovery for
     * instance.
     */
    public void purge();
     
    /**
     * While the other methods in this interface are meant for informing the
     * engine on changes, the view method is used to query the faceted navigation
     * engine.
     *
     * @param queryName An abstract name of the query, which can be used for
     * identification and presentation purposes.
     * @param initialQuery The initial query as described in the description
     * of this interface.  This initial query may never be empty.
     * @param authorizationQuery A map from facet-key to facet-value pairs
     * representing the equality terms which should be OR'ed to compose the
     * authorization query as in the description of this interface.
     * @param facetsQuery A map from facet-key to facet-value pairs
     * representing the equality terms which should be AND'ed to compose the
     * authorization query as in the description of this interface.  This map
     * may be empty.
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
     * @param futureFacetsQueries Given the already specified faceted search,
     * attempt to evaluate the future faceted searches as indicated by the key
     * of the first map.  If one would join the key of the futureFacetsQueries
     * map with the facetsQuery then this would form the basis of the
     * facetsQuery parameter of the future faceted view.  The value of the
     * futureFacetsQueries map would take the place of the resultset parameter
     * of the future view method call.
     * An implementation may choose to completely ignore this field, or
     * fill-in only parts of the structure.  It does not need to call an
     * UnsupportedOperationException
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
    public Result view(String queryName, Q initialQuery, C authorization,
               Map<String,String> facetsQuery, Q openQuery,
               Map<String,Map<String,Count>> resultset,
               Map<Map<String,String>,Map<String,Map<String,Count>>> futureFacetsQueries,
               HitsRequested hitsRequested) throws UnsupportedOperationException;

    /**
     * The second view method is trimmed down version of the first one and used
     * not the obtained the facet-value counts of a faceted navigation query,
     *   but used when the actual resultset is required.  In this case.
     */
    public Result view(String queryName, Q initialQuery, C authorization,
               Map<String,String> facetsQuery, Q openQuery, HitsRequested hitsRequested);

    /**
     * This method is used to build a Query object from a query encoded in a
     * string.  The language used, either XPATH, SQL or other should match the
     * language expected by the interface implementation.
     */
    public Q parse(String query);
}
