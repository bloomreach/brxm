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
package org.onehippo.cms7.reports.plugins.brokenlinkslist;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeIterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

public class BrokenLinksStore extends ExtJsonStore<Object> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrokenLinksStore.class);

    @SuppressWarnings("unused")
    @ExtProperty
    private boolean autoSave = false;

    private BrokenLinksListColumns columns;
    private String query;
    private int pageSize;
    private transient HippoNodeIterator hippoNodeIterator;

    BrokenLinksStore(String query, BrokenLinksListColumns columns, int pageSize) {
        super(columns.getAllExtFields());

        this.columns = columns;
        this.query = query;
        this.pageSize = pageSize;
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        final JSONObject properties = super.getProperties();
        properties.put("writer", new JSONIdentifier("new Ext.data.JsonWriter()"));
        return properties;
    }

    @Override
    protected long getTotal() {
        try {
            executeQuery();
            return this.hippoNodeIterator.getTotalSize();

        } catch (RepositoryException e) {
            log.warn("Could not retrieve total document count, paging is disabled", e);
        }

        return -1;
    }

    private void executeQuery() throws RepositoryException{
        QueryManager queryManager = UserSession.get().getJcrSession().getWorkspace().getQueryManager();

        @SuppressWarnings("deprecation") // we have to use XPath
        Query jcrQuery = queryManager.createQuery(this.query, Query.XPATH);

        QueryResult queryResult = jcrQuery.execute();
        this.hippoNodeIterator = (HippoNodeIterator)queryResult.getNodes();
    }

    @Override
    protected JSONArray getData() throws JSONException {

        JSONArray result = new JSONArray();

        final RequestCycle requestCycle = RequestCycle.get();
        ServletWebRequest swr = ((ServletWebRequest) requestCycle.getRequest());
        int startIndex = parseIntParameter(swr, "start", 0);
        int amount = parseIntParameter(swr, "limit", this.pageSize);
        int documentCount = 0;

        try {
            if (this.hippoNodeIterator == null){
                executeQuery();
            }

            this.hippoNodeIterator.skip(startIndex);

            while (hippoNodeIterator.hasNext() && documentCount < amount) {
                final Node node = hippoNodeIterator.nextNode();
                Node canonical = ((HippoNode) node).getCanonicalNode();
                if (canonical == null) {
                    log.warn("Skipped {}, no canonical node available", node.getPath());
                    continue;
                }

                final JSONObject document = new JSONObject();
                for (IBrokenLinkDocumentListColumn column: columns.getAllColumns()) {
                    final String fieldName = column.getExtField().getName();
                    final String value = getValue(canonical, column, fieldName);
                    document.put(fieldName, value);
                }
                result.put(document);
                documentCount++;
            }

        } catch (RepositoryException e) {
            log.error("Error querying data for " + this.query, e);
        }

        return result;
    }

    private String getValue(final Node canonical, final IBrokenLinkDocumentListColumn column, String fieldName) throws RepositoryException {
        try {
            final String value = column.getValue(canonical);
            if (value != null) {
                return value;
            }
        } catch (PathNotFoundException e) {
            log.info("Skipped {} of {}, path not found: {}", new Object[]{fieldName, canonical.getPath(), e.getMessage()});

        }
        return StringUtils.EMPTY;
    }

    private int parseIntParameter(ServletWebRequest request, String name, int defaultValue) {
        String param = request.getParameter(name);
        if (param != null) {
            try {
                return Integer.parseInt(param);
            } catch (NumberFormatException e) {
                log.warn("Value of parameter '" + name + "' is not an integer: '" + param
                        + "', using default value '" + defaultValue + "'");
            }
        }
        return defaultValue;
    }

}
