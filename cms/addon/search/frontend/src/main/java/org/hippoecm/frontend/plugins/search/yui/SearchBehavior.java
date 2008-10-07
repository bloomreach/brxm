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

import java.util.StringTokenizer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import net.sf.json.JSONObject;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteBehavior;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteSettings;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchBehavior extends AutoCompleteBehavior {
    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(SearchBehavior.class);
    
    private IBrowseService<IModel> browseService; 
    
    public SearchBehavior(AutoCompleteSettings settings, IBrowseService<IModel> browse) {
        super(settings);
        this.browseService = browse;
        contribHelper.addModule(SearchNamespace.NS, "searchbox");
    }

    @Override
    protected String getModuleClass() {
        return "YAHOO.hippo.SearchBox";
    }

    @Override
    protected void respond(AjaxRequestTarget ajaxTarget) {
        final RequestCycle requestCycle = RequestCycle.get();
        
        String browse = requestCycle.getRequest().getParameter("browse");
        if(browse != null && browse.length() > 0) {
            if (browseService != null) {
                browseService.browse(new JcrNodeModel(browse));
            } else {
                log.warn("no browser service found");
            }
            return;
        }

        final String callbackMethod = requestCycle.getRequest().getParameter("callback");
        final String searchParam = requestCycle.getRequest().getParameter("query");
        
        SearchResult sr;
        try {
            sr = new SearchBuilder().search(searchParam);
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
                //final String encoding = Application.get().getRequestCycleSettings().getResponseRequestEncoding();
                //r.setCharacterEncoding(encoding);
                //r.setContentType("text/xml; charset=" + encoding);

                // Make sure it is not cached by a
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
    
    private class SearchBuilder {
        private ResultItem[] emptyResults = new ResultItem[0];
        
        public SearchBuilder() {
        }
        
        private ResultItem[] doSearch(String value) {
            value = value.trim();
            if(value.equals(""))
                return emptyResults;
            
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            Node content;
            String uuid = null;
            try {
                content = (Node)session.getItem("/content");
                uuid = content.getUUID();
            } catch (RepositoryException e) {
               log.error("Node /content not found, query buidl fails", e);
               return emptyResults;
            }
            String where = "";
            if(uuid!=null) {
                where = "hippo:paths =  '" + uuid +"'";
            }
            StringTokenizer st = new StringTokenizer(value, " ");
            while(st.hasMoreTokens()) {
                where += " and jcr:contains(., '" + st.nextToken() + "*')";
            }
            String queryString = "//element(*, hippo:harddocument)[" + where + "]/rep:excerpt(.)";
            String queryType = "xpath";
            QueryResult result = null;
       
            try {
                QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
                HippoQuery query = (HippoQuery) queryManager.createQuery(queryString, queryType);
                session.refresh(true);
                result = query.execute();
            } catch (RepositoryException e) {
                log.error("Error executing query[" + queryString + "]", e);
            }
            
            if(result != null) {
                ResultItem[] results;
                try {
                    results = new ResultItem[(int) result.getRows().getSize()];
                    int count = 0;

                    for(RowIterator it =result.getRows(); it.hasNext(); ){
                        Row row = it.nextRow();
                        try {
                            String path = row.getValue("jcr:path").getString();
                            HippoNode node = (HippoNode)session.getItem(path);
                        
                            String state = node.hasProperty("hippostd:state") ? node.getProperty("hippostd:state").getString() : "";
                            String excerpt = row.getValue("rep:excerpt(.)").getString();
                            String displayName = node.getDisplayName();
                            String url = node.getPath();
                            
                            results[count++] = new ResultItem(displayName, url, state, excerpt);
                        } catch (ItemNotFoundException infe) {
                            log.warn("Item not found", infe);
                        } catch (ValueFormatException vfe) {
                            //Should never happen
                        }
                    }
                    return results;
                } catch (RepositoryException e) {
                    log.error("Error parsing query results[" + queryString + "]", e);
                }
            }
                
            return emptyResults;
        }
        
        public SearchResult search(String value) throws RepositoryException {
            return new SearchResult(doSearch(value));
        }

    }
    
    
    /**
     * Helper bean for an easy jcr-nodes 2 JSON transition
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
            this.url=url;
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
