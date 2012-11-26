/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a searchable list of beans. Subclasses provide the query to search for all beans. The query to limit the
 * list is used as a free text query. All Lucene-specific characters are stripped from the query, except the wildcards *
 * and ?, the - operator to negate the term after it, and the 'or' keyword to OR terms instead of ANDing them.
 * <p/>
 * Whenever something changes below the JCR path and in the JCR types provided to the constructor, this observable will
 * notify registered {@link org.hippoecm.frontend.model.event.IObserver}s.
 * <p/>
 * TODO: Remove primitive total count accounting when it's possible to get the size of the resultset without going
 * through the accessmanager.
 */
public abstract class SearchableDataProvider<T extends Comparable<T>> extends SortableDataProvider<T> implements IObservable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SearchableDataProvider.class);

    private static final String[][] JCR_CONTAINS_QUERY_REPLACEMENTS = {
            // replace single quotes by double ones to avoid the search query breaking out of the SQL query itself
            {"'", "''"},
            // escape Lucene special characters: + && || ! ( ) { } [ ] ^ " ~ : \
            // we keep the - for negating, and * and ? for wildcards
            {"\\", "\\\\"},
            {"+", "\\+"},
            {"&&", "\\&&"},
            {"||", "\\||"},
            {"!", "\\!"},
            {"(", "\\("},
            {")", "\\)"},
            {"{", "\\{"},
            {"}", "\\}"},
            {"[", "\\["},
            {"]", "\\]"},
            {"^", "\\^"},
            {"\"", "\\\""},
            {":", "\\:"},
            // escaping ~ does not work, so we remove it entirely
            {"~", " "},
            // for usability, replace 'or' by 'OR' so users do not have to explicitly type capitals
            {" or ", " OR "}
    };

    private final String queryAll;
    private final String observePath;
    private final String[] observeNodeTypes;
    private String query;
    private List<T> list = new ArrayList<T>();
    private volatile boolean dirty = true;
    private IObservationContext<JcrNodeModel> context;
    private JcrEventListener listener;

    /**
     * Creates a searchable provider.
     *
     * @param queryAll         the JCR SQL query to search for all beans
     * @param observePath      the JCR path to observe for changes
     * @param observeNodeTypes the node types to observe for changes
     */
    public SearchableDataProvider(String queryAll, String observePath, String... observeNodeTypes) {
        this.queryAll = queryAll;
        this.observePath = observePath;
        this.observeNodeTypes = observeNodeTypes;
    }

    /**
     * Creates a bean from a JCR node.
     *
     * @param node the JCR node
     * @return the bean representing the JCR node
     * @throws RepositoryException when creating the bean failed
     */
    protected abstract T createBean(Node node) throws RepositoryException;

    public Iterator<T> iterator(int first, int count) {
        populateList(query);
        List<T> result = new ArrayList<T>();
        for (int i = first; i < (count + first); i++) {
            result.add(list.get(i));
        }
        return result.iterator();
    }

    public int size() {
        populateList(query);
        return list.size();
    }

    protected List<T> getList() {
        return list;
    }

    /**
     * Populate list, refresh when a new session id is found or when dirty
     *
     * @param query the query used for free text search to limit the list
     */
    private void populateList(String query) {
        // synchronize on the runtime class, as there can be multiple implementations of this abstract class
        if (!dirty) {
            return;
        }
        list.clear();

        StringBuilder sqlQuery = new StringBuilder(queryAll);
        if (StringUtils.isNotEmpty(query)) {
            sqlQuery.append(" and CONTAINS(., '");
            sqlQuery.append(query);
            sqlQuery.append("')");
        }
        log.debug("Executing query: {}", sqlQuery);
        try {
            UserSession session = UserSession.get();
            @SuppressWarnings("deprecation") Query listQuery =
                    session.getQueryManager().createQuery(sqlQuery.toString(), Query.SQL);
            NodeIterator iter = listQuery.execute().getNodes();
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node != null) {
                    try {
                        list.add(createBean(node));
                    } catch (RepositoryException e) {
                        log.warn("Unable to instantiate new bean.", e);
                    }
                }
            }
            Collections.sort(list);
            dirty = false;
        } catch (RepositoryException e) {
            log.error("Error while executing query: " + sqlQuery, e);
        }
    }

    /**
     * Set the search query. Only beans that match the query will be included. A '*' in the query acts as a wildcard.
     * When the query is null or empty, all beans will be included.
     * <p/>
     * N.B. this method is needed to let Wicket use setQuery/getQuery in a PropertyModel instead of reflection.
     *
     * @param newQuery the query to search for users with
     */
    @SuppressWarnings("unused")
    public void setQuery(final String newQuery) {
        this.query = escapeJcrContainsQuery(newQuery);
        dirty = true;
    }

    /**
     * Returns the query used by this provider to limit the provided beans. The query can be null or empty, in which
     * case there are no limitations and all beans will be included.
     * <p/>
     * N.B. this method is needed to let Wicket use setQuery/getQuery in a PropertyModel instead of reflection.
     *
     * @return the search query to use
     */
    @SuppressWarnings("unused")
    public String getQuery() {
        return this.query;
    }

    /**
     * Escapes special/illegal characters and constructs in a query for the JCR 'CONTAINS' query: <ul> <li>The string is
     * trimmed</li> <li>Illegal XPath characters are escaped</li> <li>Single quotes are replaced by double quotes</li>
     * <li>Parentheses are escaped with \ to avoid Lucene parsing errors</li> <li>' or ' is replaced by ' OR ' so they
     * are recognized as custom OR constructs</li> <li>'and' and 'or' at the start and end of the query is removed to
     * avoid Lucene parsing errors</li> <li>a single '*' is replaced by an empty string</li> </ul>
     *
     * @param input the free text query to escape
     * @return the escaped query, which can safely be used in a JCR 'CONTAINS' query
     */
    static String escapeJcrContainsQuery(String input) {
        String result = StringUtils.trimToEmpty(input);
        if (StringUtils.isNotEmpty(result)) {
            for (String[] replacement : JCR_CONTAINS_QUERY_REPLACEMENTS) {
                result = StringUtils.replace(result, replacement[0], replacement[1]);
            }

            // remove all standalone occurences of '*', '**', etc.
            result = result.replaceAll("\\*\\*+", "*");
            result = StringUtils.replace(result, " * ", " ");
            result = replaceStart(result, "* ", " ");
            result = replaceEnd(result, " *", " ");
            result = result.trim();

            // remove 'and' and 'or' from start and end of the query to avoid Lucene parsing errors
            result = removeStartIgnoreCase(result, "and ");
            result = removeStartIgnoreCase(result, "or ");
            result = removeEndIgnoreCase(result, " and");
            result = removeEndIgnoreCase(result, " or");

            // replace multiple -'s by a single -
            result = result.replaceAll("--+", "-");

            // remove - without anything after it
            result = StringUtils.replace(result, "- ", " ");
            result = removeEndIgnoreCase(result, "-");

            // replace a single '*' with an empty string
            result = result.trim();
            result = clearExactIgnoreCase(result, "*", "and", "or");
        }
        return result;
    }

    static String replaceStart(String s, String replace, String with) {
        if (s.startsWith(replace)) {
            return with + s.substring(replace.length());
        }
        return s;
    }

    static String replaceEnd(String s, String replace, String with) {
        if (s.endsWith(replace)) {
            return s.substring(0, s.length() - replace.length()) + with;
        }
        return s;
    }

    static String removeStartIgnoreCase(String s, String remove) {
        if (StringUtils.isNotEmpty(s)
                && StringUtils.isNotEmpty(remove)
                && s.length() >= remove.length()
                && s.substring(0, remove.length()).equalsIgnoreCase(remove)) {
            return s.substring(remove.length());
        }
        return s;
    }

    static String removeEndIgnoreCase(String s, String remove) {
        if (StringUtils.isNotEmpty(s)
                && StringUtils.isNotEmpty(remove)
                && s.length() >= remove.length()
                && s.substring(s.length() - remove.length()).equalsIgnoreCase(remove)) {
            return s.substring(0, s.length() - remove.length());
        }
        return s;
    }

    static String clearExactIgnoreCase(String s, String... clears) {
        for (String clear : clears) {
            if (s.equalsIgnoreCase(clear)) {
                return StringUtils.EMPTY;
            }
        }
        return s;
    }

    // implement IObservable

    @Override
    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.context = (IObservationContext<JcrNodeModel>) context;
    }

    @Override
    public void startObservation() {
        listener = new JcrEventListener(context, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED
                | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED | Event.NODE_MOVED, observePath, true, null, observeNodeTypes) {
            @Override
            public void onEvent(final EventIterator events) {
                dirty = true;
                super.onEvent(events);
            }
        };
        listener.start();
    }

    @Override
    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

    @Override
    public void detach() {
        if (listener == null) {
            dirty = true;
            list.clear();
        }
        super.detach();
    }

    // override Object

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SearchableDataProvider)) {
            return false;
        }
        SearchableDataProvider other = (SearchableDataProvider) object;

        return new EqualsBuilder()
                .append(queryAll, other.queryAll)
                .append(observePath, other.observePath)
                .append(observeNodeTypes, other.observeNodeTypes)
                .append(query, other.query)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(57, 433)
                .append(queryAll)
                .append(observePath)
                .append(observeNodeTypes)
                .append(query)
                .toHashCode();
    }

}
