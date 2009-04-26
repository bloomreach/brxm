/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.frontend.plugins.search.yui;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import net.sf.json.JSONObject;

import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteBehavior;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchBehavior extends AutoCompleteBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchBehavior.class);

    private static final String CALLBACK_PARAM = "callback";
    private static final String SEARCH_QUERY_PARAM = "query";
    private static final String BROWSE_PARAM = "browse";

    private final IBrowseService<IModel> browseService;
    private final SearchBuilder searchBuilder;

    public SearchBehavior(IPluginContext context, IPluginConfig config, IBrowseService<IModel> browse) {
        super(YuiPluginHelper.getManager(context), new AutoCompleteSettings(YuiPluginHelper.getConfig(config)));
        this.browseService = browse;
        searchBuilder = new SearchBuilder(config);
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        super.addHeaderContribution(context);
        context.addModule(SearchNamespace.NS, "searchbox");
    }

    @Override
    protected String getClientClassname() {
        return "YAHOO.hippo.SearchBox";
    }

    @Override
    protected void respond(AjaxRequestTarget ajaxTarget) {
        final RequestCycle requestCycle = RequestCycle.get();

        String browse = requestCycle.getRequest().getParameter(BROWSE_PARAM);
        if (browse != null && browse.length() > 0) {
            if (browseService != null) {
                browseService.browse(new JcrNodeModel(browse));
            } else {
                log.warn("no browser service found");
            }
            return;
        }

        final String callbackMethod = requestCycle.getRequest().getParameter(CALLBACK_PARAM);
        final String searchParam = requestCycle.getRequest().getParameter(SEARCH_QUERY_PARAM);

        SearchResult sr;
        try {
            sr = searchBuilder.search(searchParam);
        } catch (RepositoryException e) {
            log.error("An error occured during search", e);
            return;
        }

        JSONObject JSONRoot = new JSONObject();
        JSONObject results = JSONObject.fromObject(sr);
        JSONRoot.element("response", results);

        final String responseStr = callbackMethod + "(" + JSONRoot.toString() + ");";
        IRequestTarget requestTarget = new IRequestTarget() {
            public void respond(RequestCycle requestCycle) {
                WebResponse r = (WebResponse) requestCycle.getResponse();

                // Determine encoding
                final String encoding = Application.get().getRequestCycleSettings().getResponseRequestEncoding();
                r.setCharacterEncoding(encoding);
                r.setContentType("application/json; charset=" + encoding);

                // Make sure it is not cached
                r.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                r.setHeader("Cache-Control", "no-cache, must-revalidate");
                r.setHeader("Pragma", "no-cache");

                r.write(responseStr);
            }

            public void detach(RequestCycle requestCycle) {
            }

        };
        requestCycle.setRequestTarget(requestTarget);

    }

    //I guess this should be loaded as a service instead of being an internal class
    private static class SearchBuilder implements IClusterable {
        private static final long serialVersionUID = 1L;

        private static final String SEARCH_PATHS = "search.paths";
        private static final String EXCLUDE_PRIMARY_TYPES = "exclude.primary.types";
        private static final String IGNORE_CHARS = "ignore.chars";
        private static final String WILDCARD_SEARCH = "wildcard.search";

        private static ResultItem[] EMPTY_RESULTS = new ResultItem[0];

        private final String defaultWhere;
        private final boolean wildcardSearch;
        private final String ignoreChars;

        public SearchBuilder(IPluginConfig config) {
            String[] searchPaths = config.getStringArray(SEARCH_PATHS);
            if (searchPaths == null || searchPaths.length == 0) {
                throw new IllegalArgumentException("Property " + SEARCH_PATHS + " is required.");
            }
            String[] excludePrimaryTypes = config.getStringArray(EXCLUDE_PRIMARY_TYPES);

            wildcardSearch = config.getBoolean(WILDCARD_SEARCH);
            ignoreChars = config.getString(IGNORE_CHARS, "");

            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();

            StringBuilder sb = new StringBuilder("//element(*, hippo:harddocument)[");
            if (excludePrimaryTypes.length > 0) {
                sb.append("not(");
                boolean addOr = false;
                for (String exclude : excludePrimaryTypes) {
                    if (addOr) {
                        sb.append(" or ");
                    } else
                        addOr = true;
                    sb.append("@jcr:primaryType='").append(exclude).append('\'');
                }
                sb.append(") and ");
            }
            sb.append("(");

            boolean addOr = false;
            for (String path : searchPaths) {
                if (!path.startsWith("/")) {
                    throw new IllegalArgumentException("Search path should be absolute: " + path);
                }
                try {
                    Node content = (Node) session.getItem(path);
                    NodeIterator ni = content.getNodes();
                    while (ni.hasNext()) {
                        Node cn = ni.nextNode();
                        if (cn.isNodeType("mix:referenceable")) {
                            String uuid = cn.getUUID();
                            if (uuid != null) {
                                if (addOr) {
                                    sb.append(" or ");
                                } else {
                                    addOr = true;
                                }
                                sb.append("hippo:paths = '").append(uuid).append('\'');
                            }
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("An error occured while constructing the default search where-clause part", e);
                    throw new IllegalStateException(
                            "An error occured while constructing the default search where-clause part", e);
                }
            }
            sb.append(')');
            defaultWhere = sb.toString();
        }

        private ResultItem[] doSearch(String value) {
            value = value.trim();
            if (value.equals("")) {
                return EMPTY_RESULTS;
            }
            StringBuilder query = new StringBuilder(defaultWhere);

            for (StringTokenizer st = new StringTokenizer(value, " "); st.hasMoreTokens();) {
                query.append(" and jcr:contains(., '");
                String token = st.nextToken();
                for (int i = 0; i < token.length(); i++) {
                    char c = token.charAt(i);
                    if (ignoreChars.indexOf(c) == -1) {
                        if (c == '\'') {
                            query.append('\'');
                        }
                        query.append(c);
                    }
                }
                if (wildcardSearch) {
                    query.append('*');
                }
                query.append("')");
            }
            query.append(']').append("/rep:excerpt(.)");
            final String queryString = query.toString();
            final String queryType = "xpath";
            QueryResult result = null;

            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            try {
                QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
                HippoQuery hippoQuery = (HippoQuery) queryManager.createQuery(queryString, queryType);
                session.refresh(true);
                hippoQuery.setLimit(15);

                long start = System.currentTimeMillis();
                result = hippoQuery.execute();
                long end = System.currentTimeMillis();
                log.info("Executing search query: " + queryString + " took " + (end-start) + "ms");
            } catch (RepositoryException e) {
                log.error("Error executing query[" + queryString + "]", e);
            }

            try {
                List<ResultItem> resultList = new ArrayList<ResultItem>(15);
                if(result != null) {
                    int count = 0;

                    for (RowIterator it = result.getRows(); it.hasNext();) {
                        Row row = it.nextRow();
                        try {
                            String path = row.getValue("jcr:path").getString();
                            HippoNode node = (HippoNode) session.getItem(path);

                            String state;
                            if (node.hasProperty("hippostd:state")) {
                                state = node.getProperty("hippostd:state").getString();
                                TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel(
                                "hippostd:publishable"));
                                state = (String) translator.getValueName("hippostd:state", new Model(state))
                                .getObject();
                            } else {
                                state = "null";
                            }

                            String excerpt = row.getValue("rep:excerpt()").getString();
                            String displayName = NodeNameCodec.decode(node.getDisplayName());
                            String url = node.getPath();

                            resultList.add(new ResultItem(displayName, url, state, excerpt));
                        } catch (ItemNotFoundException infe) {
                            log.warn("Item not found", infe);
                        } catch (ValueFormatException vfe) {
                            //Should never happen
                        }
                    }
                    return resultList.toArray(new ResultItem[resultList.size()]);
                }
            } catch (RepositoryException e) {
                log.error("Error parsing query results[" + queryString + "]", e);
            }
            return EMPTY_RESULTS;
        }

        public SearchResult search(String value) throws RepositoryException {
            return new SearchResult(doSearch(value));
        }

    }

    /**
     * Helper bean for an easy jcr-nodes2JSON translation
     */
    public static class SearchResult {

        private ResultItem[] results;
        private int totalHits;

        public int getTotalHits() {
            return totalHits;
        }

        public void setTotalHits(int totalHits) {
            this.totalHits = totalHits;
        }

        public ResultItem[] getResults() {
            return results;
        }

        public void setResults(ResultItem[] results) {
            this.results = results;
        }

        public SearchResult(ResultItem[] results) {
            this.results = results;
            this.totalHits = results.length;
        }
    }

    public static class ResultItem {

        String label;
        String url;
        String state;
        String excerpt;

        public ResultItem(String label, String url, String state, String excerpt) {
            this.label = label;
            this.url = url;
            this.state = state;
            this.excerpt = excerpt;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getExcerpt() {
            return excerpt;
        }

        public void setExcerpt(String excerpt) {
            this.excerpt = excerpt;
        }

    }

}
