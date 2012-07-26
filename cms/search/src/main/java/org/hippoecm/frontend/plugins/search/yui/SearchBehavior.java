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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import net.sf.json.JSONObject;

import org.apache.wicket.Application;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.search.TextSearchBuilder;
import org.hippoecm.frontend.plugins.standards.search.TextSearchDataProvider;
import org.hippoecm.frontend.plugins.standards.search.TextSearchMatch;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteBehavior;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchBehavior extends AutoCompleteBehavior {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SearchBehavior.class);

    private static final String SEARCH_PATHS = "search.paths";
    private static final String EXCLUDE_PRIMARY_TYPES = "exclude.primary.types";
    private static final String IGNORE_CHARS = "ignore.chars";
    private static final String WILDCARD_SEARCH = "wildcard.search";

    private static final String CALLBACK_PARAM = "callback";
    private static final String SEARCH_QUERY_PARAM = "query";
    private static final String BROWSE_PARAM = "browse";

    private final IBrowseService<IModel> browseService;
    private final TextSearchBuilder searchBuilder;

    public SearchBehavior(IPluginConfig config, IBrowseService<IModel> browse) {
        super(new AutoCompleteSettings(YuiPluginHelper.getConfig(config)));
        this.browseService = browse;
        searchBuilder = new TextSearchBuilder();
        searchBuilder.setScope(getSearchPaths(config.getStringArray(SEARCH_PATHS)));
        searchBuilder.setExcludedPrimaryTypes(config.getStringArray(EXCLUDE_PRIMARY_TYPES));
        searchBuilder.setIgnoredChars(config.getString(IGNORE_CHARS));
        searchBuilder.setWildcardSearch(config.getBoolean(WILDCARD_SEARCH));
        searchBuilder.setLimit(15);

        settings.setSchemaFields("label", "path", "state", "excerpt");
        settings.setSchemaResultList("response.results");
    }

    private String[] getSearchPaths(String[] basePaths) {
        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();

        List<String> paths = new LinkedList<String>();
        for (String path : basePaths) {
            try {
                Node content = (Node) session.getItem(path);
                NodeIterator ni = content.getNodes();
                while (ni.hasNext()) {
                    Node cn = ni.nextNode();
                    if (cn.isNodeType("mix:referenceable")) {
                        paths.add(cn.getPath());
                    }
                }
            } catch (PathNotFoundException e) {
                log.warn("Search path not found: " + path);
            } catch (RepositoryException e) {
                log.error("Error determinining search paths", e);
            }
        }
        return paths.toArray(new String[paths.size()]);
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

        List<ResultItem> resultList = new ArrayList<ResultItem>(15);
        try {
            searchBuilder.setText(searchParam);
            IDataProvider<TextSearchMatch> results = new TextSearchDataProvider(searchBuilder.getResultModel());
            Iterator<? extends TextSearchMatch> iter = results.iterator(0, results.size());
            while (iter.hasNext()) {
                TextSearchMatch match = iter.next();
                Node node = match.getNode();
                try {
                    String state;
                    if (node.hasProperty("hippostd:state")) {
                        state = node.getProperty("hippostd:state").getString();
                        TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel("hippostd:publishable"));
                        state = translator.getValueName("hippostd:state", new Model<String>(state)).getObject();
                    } else {
                        state = "null";
                    }

                    String excerpt = match.getExcerpt();
                    String displayName = new NodeTranslator(new JcrNodeModel(node)).getNodeName().getObject();
                    String url = node.getPath();

                    resultList.add(new ResultItem(displayName, url, state, excerpt));
                } catch (ItemNotFoundException infe) {
                    log.warn("Item not found", infe);
                } catch (ValueFormatException vfe) {
                    //Should never happen
                }
            }
        } catch (RepositoryException e) {
            log.error("Error processing results", e);
        }
        SearchResult sr = new SearchResult(resultList.toArray(new ResultItem[resultList.size()]));

        JSONObject JSONRoot = new JSONObject();
        JSONObject results = JSONObject.fromObject(sr);
        JSONRoot.element("response", results);

        final String responseStr = callbackMethod + "(" + JSONRoot.toString() + ");";
        final IRequestTarget searchResponse = new SearchResponse(responseStr);
        requestCycle.setRequestTarget(searchResponse);
    }

    private static class SearchResponse implements IRequestTarget {

        private final String responseStr;

        public SearchResponse(final String responseStr) {
            this.responseStr = responseStr;
        }

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
            // do nothing
        }

    }

}
