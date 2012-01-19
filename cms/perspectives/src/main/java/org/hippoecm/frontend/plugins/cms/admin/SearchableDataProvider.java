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
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a searchable list of beans. Creation of the list itself is managed by subclasses to allow the usage
 * of static lists.
 *
 * TODO: Remove primitive total count accounting when it's possible to get the size of the resultset without
 * going through the accessmanager.
 */
public abstract class SearchableDataProvider<T> extends SortableDataProvider<T> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SearchableDataProvider.class);

    private static final String[][] JCR_CONTAINS_QUERY_REPLACEMENTS = {
            { "'", "''" },
            { "(", "\\(" },
            { ")", "\\)" },
            { " or ", " OR "}
    };

    private static String sessionId = "none";

    private String queryAll;
    private String query;

    /**
     * Creates a searchable provider.
     *
     * @param queryAll the JCR SQL query to search for all beans
     */
    public SearchableDataProvider(String queryAll) {
        this.queryAll = queryAll;
    }

    /**
     * Creates a bean from a JCR node.
     *
     * @param node the JCR node
     * @return the bean representing the JCR node
     * @throws RepositoryException when creating the bean failed
     */
    protected abstract T createBean(Node node) throws RepositoryException;

    /**
     * @return the list of beans to manipulate or update.
     */
    protected abstract List<T> getList();

    /**
     * @return whether the list of beans should be updated or not.
     */
    protected abstract boolean isDirty();

    /**
     * Sets whether the list of beans should be updated or not
     * @param dirty true if the list of beans should be updated, false otherwise.
     */
    protected abstract void setDirty(boolean dirty);

    public Iterator<T> iterator(int first, int count) {
        List<T> result = new ArrayList<T>();
        List<T> list = getList();
        for (int i = first; i < (count + first); i++) {
            result.add(list.get(i));
        }
        return result.iterator();
    }

    public int size() {
        populateList(query);
        return getList().size();
    }

    /**
     * Populate list, refresh when a new session id is found or when dirty
     */
    private synchronized void populateList(String query) {
        // synchronize on the runtime class, as there can be multiple implementations of this abstract class
        synchronized(getClass()) {
            if (!isDirty() && sessionId.equals(Session.get().getId())) {
                return;
            }
            final List list = getList();
            list.clear();

            StringBuilder sqlQuery = new StringBuilder(queryAll);
            if (StringUtils.isNotEmpty(query)) {
                sqlQuery.append(" and CONTAINS(., '");
                sqlQuery.append(query);
                sqlQuery.append("')");
            }
            log.debug("Executing query: {}", sqlQuery);
            try {
                Query listQuery = ((UserSession) Session.get()).getQueryManager().createQuery(sqlQuery.toString(), Query.SQL);
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
                sessionId = Session.get().getId();
                setDirty(false);
            } catch (RepositoryException e) {
                log.error("Error while executing query: " + sqlQuery, e);
            }
        }
    }

    /**
     * Set the search query. Only beans that match the query will be included. A '*' in the query acts as a wildcard.
     * When the query is null or empty, all beans will be included.
     *
     * @param newQuery the query to search for users with
     */
    public void setQuery(final String newQuery) {
        this.query = escapeJcrContainsQuery(newQuery);
        setDirty(true);
    }

    /**
     * Returns the query used by this provider to limit the provided beans. The query can be null or empty,
     * in which case there are no limitations and all beans will be included.
     *
     * @return the search query to use
     */
    public String getQuery() {
        return this.query;
    }

    /**
     * Escapes special/illegal characters and constructs in a query for the JCR 'CONTAINS' query:
     * <ul>
     *     <li>The string is trimmed</li>
     *     <li>Illegal XPath characters are escaped</li>
     *     <li>Single quotes are replaced by double quotes</li>
     *     <li>Parentheses are escaped with \ to avoid Lucene parsing errors</li>
     *     <li>' or ' is replaced by ' OR ' so they are recognized as custom OR constructs</li>
     *     <li>'and' and 'or' at the start and end of the query is removed to avoid Lucene parsing errors</li>
     *     <li>a single '*' is replaced by an empty string</li>
     * </ul>
     * @param input the free text query to escape
     * @return the escaped query, which can safely be used in a JCR 'CONTAINS' query
     */
    static String escapeJcrContainsQuery(String input) {
        String result = StringUtils.trimToEmpty(input);
        if (StringUtils.isNotEmpty(result)) {
            result = Text.escapeIllegalXpathSearchChars(result);
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

}
