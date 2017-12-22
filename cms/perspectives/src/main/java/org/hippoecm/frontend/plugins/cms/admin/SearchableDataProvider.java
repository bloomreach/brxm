/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.frontend.plugins.cms.admin.search.AdminTextSearchBuilder;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a searchable list of beans. Subclasses provide the query to search for all beans.
 * <p/>
 * Whenever something changes below the JCR path and in the JCR types provided to the constructor, this observable will
 * notify registered {@link org.hippoecm.frontend.model.event.IObserver}s.
 * <p/>
 * TODO: Remove primitive total count accounting when it's possible to get the size of the resultset without going
 * through the accessmanager.
 */
public abstract class SearchableDataProvider<T extends Comparable<T>> extends SortableDataProvider<T, String> implements IObservable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SearchableDataProvider.class);

    private final String searchAllSqlStatement;
    private final String observePath;
    private final String[] observeNodeTypes; // TODO: why do we observe changes on folder types?
    private String searchTerm;
    private String[] includePrimaryTypes = new String[] {};
    private List<T> list = new ArrayList<T>();
    private volatile boolean dirty = true;
    private IObservationContext<JcrNodeModel> context;
    private JcrEventListener listener;

    /**
     * Creates a searchable provider.
     *
     * @param searchAllSqlStatement          the JCR SQL searchTerm to search for all beans
     * @param observePath                    the JCR path to observe for changes
     * @param observeNodeTypes               the node types to observe for changes
     */
    public SearchableDataProvider(final String searchAllSqlStatement, final String observePath,
                                  final String... observeNodeTypes) {
        this.searchAllSqlStatement = searchAllSqlStatement;
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

    @Override
    public Iterator<T> iterator(long first, long count) {
        populateList(searchTerm);
        List<T> result = new ArrayList<T>();
        for (long i = first; i < (count + first); i++) {
            result.add(list.get((int) i));
        }
        return result.iterator();
    }

    @Override
    public long size() {
        populateList(searchTerm);
        return list.size();
    }

    protected List<T> getList() {
        return list;
    }

    /**
     * Populate list, refresh when a new session id is found or when dirty
     *
     * @param searchTerm the searchTerm used for free text search to limit the list
     */
    private void populateList(final String searchTerm) {
        // synchronize on the runtime class, as there can be multiple implementations of this abstract class
        if (!dirty) {
            return;
        }
        list.clear();

        try {
            final NodeIterator nodeIterator;

            if (StringUtils.isNotEmpty(searchTerm)) {
                final AdminTextSearchBuilder searchBuilder = new AdminTextSearchBuilder();
                searchBuilder.setText(searchTerm);
                searchBuilder.setWildcardSearch(true);
                searchBuilder.setIncludePrimaryTypes(includePrimaryTypes);
                searchBuilder.setScope(new String[]{observePath});
                nodeIterator = searchBuilder.getResultModel().getObject().getQueryResult().getNodes();
            } else {
                final UserSession session = UserSession.get();
                @SuppressWarnings("deprecation")
                final Query listQuery = session.getQueryManager().createQuery(searchAllSqlStatement, Query.SQL);
                nodeIterator = listQuery.execute().getNodes();
            }

            while (nodeIterator.hasNext()) {
                final Node node = nodeIterator.nextNode();
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
            log.error("Error while executing query.", e);
        }
    }

    /**
     * Set the search searchTerm.
     * <p/>
     * N.B. this method is needed to let Wicket use setSearchTerm/getSearchTerm in a PropertyModel instead of
     * reflection.
     *
     * @param searchTerm the searchTerm to search for
     */
    @SuppressWarnings("unused")
    public void setSearchTerm(final String searchTerm) {
        if (!StringUtils.equalsIgnoreCase(searchTerm, this.searchTerm)) {
            this.searchTerm = searchTerm;
            dirty = true;
        }
    }

    /**
     * Returns the searchTerm used by this provider to limit the provided beans. The searchTerm can be null or empty, in
     * which case there are no limitations and all beans will be included.
     * <p/>
     * N.B. this method is needed to let Wicket use setSearchTerm/getSearchTerm in a PropertyModel instead of
     * reflection.
     *
     * @return the search searchTerm to use
     */
    @SuppressWarnings("unused")
    public String getSearchTerm() {
        return this.searchTerm;
    }

    public void setIncludePrimaryTypes(final String[] includePrimaryTypes) {
        this.includePrimaryTypes = includePrimaryTypes;
    }

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
                .append(searchAllSqlStatement, other.searchAllSqlStatement)
                .append(observePath, other.observePath)
                .append(observeNodeTypes, other.observeNodeTypes)
                .append(searchTerm, other.searchTerm)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(57, 433)
                .append(searchAllSqlStatement)
                .append(observePath)
                .append(observeNodeTypes)
                .append(searchTerm)
                .toHashCode();
    }

}
