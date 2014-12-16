/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.forge.ecmtagging.editor.autocomplete;

import java.util.Collections;
import java.util.HashSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteBehavior;
import org.hippoecm.frontend.plugins.yui.autocomplete.AutoCompleteSettings;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.onehippo.forge.ecmtagging.editor.TagsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

/**
 * This behaviour adds a dropdownbox with tag autocompletion
 * to the tag input plug-in.
 *
 * @author Jeroen Tietema
 */
public class TagBehaviour extends AutoCompleteBehavior {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TagBehaviour.class);

    private final SearchBuilder searchBuilder;
    private HashSet<String> previousTags;
    private String previousTag;
    private JcrNodeModel nodeModel;

    public TagBehaviour(IPluginContext context, IPluginConfig config, JcrNodeModel nodeModel, TagsModel tagsModel) {
        super(new AutoCompleteSettings(YuiPluginHelper.getConfig(config)));
        this.nodeModel = nodeModel;
        String tags = (String) tagsModel.getObject();
        if (tags != null) {
            tags = tags.trim();
            if (tags.endsWith(",")) {
                tags = tags.substring(0, tags.length() - 1);
            }
            this.previousTags = arrayToHashSet(tags.split(","));
        }
        searchBuilder = new SearchBuilder();
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        // fix me. 
        // removed the javascript autocompletion because it interfered with autocomplete of searchbox.
        super.addHeaderContribution(context);
        context.addModule(TagNamespace.NS, "tagbox");
    }

    @Override
    protected String getClientClassname() {
        return "YAHOO.hippo.TagBox";
    }

    private HashSet<String> arrayToHashSet(String[] tags) {
        HashSet<String> tagSet = new HashSet<String>();
        Collections.addAll(tagSet, tags);
        return tagSet;
    }

    private boolean containsPreviousTag(Value[] tags) throws IllegalStateException,
            RepositoryException {
        for (Value tag : tags) {
            log.debug("Tag: {} matches {}?", tag.getString(), previousTag);
            if (tag.getString().equals(previousTag)) {
                return true;
            }
        }
        return false;
    }

    public String getPreviousTag() {
        return previousTag;
    }

    @Override
    protected void respond(AjaxRequestTarget ajaxTarget) {

        final RequestCycle requestCycle = RequestCycle.get();

        final Request request = requestCycle.getRequest();
        final IRequestParameters requestParameters = request.getRequestParameters();
        final StringValue callbackMethod = requestParameters.getParameterValue("callback");
        final StringValue searchParam = requestParameters.getParameterValue("query");
        StringValue add = requestParameters.getParameterValue("add");

        if (log.isDebugEnabled()) {
            log.debug("Params: " + add + ", " + callbackMethod + ", " + searchParam);
        }

        if (!add.isNull() && !add.isEmpty()) {
            // add the clicked tag
            try {
                Property prop = nodeModel.getNode().getProperty(TaggingNodeType.PROP_TAGS);
                Value[] tags = prop.getValues();
                String[] newTags;
                // NEEDS TESTING --
                if (containsPreviousTag(tags)) {
                    log.debug("Contains previous tag: " + previousTag);
                    newTags = new String[tags.length];
                    for (int i = 0; i < tags.length; i++) {
                        if (tags[i].getString().equals(previousTag)) {
                            newTags[i] = add.toString();
                        } else {
                            newTags[i] = tags[i].getString();
                        }
                    }
                } else {
                    log.debug("Doesn't contain previous tag: " + previousTag);
                    newTags = new String[tags.length + 1];
                    for (int i = 0; i < tags.length; i++) {
                        newTags[i] = tags[i].getString();
                    }
                    newTags[tags.length] = add.toString();
                }
                // NEEDS TESTING --^
                prop.setValue(newTags);
                // TODO mmilicevic, removed explicit model change call
                //context.getService(Io.class.getName(), IJcrService.class).flush(nodeModel);
            } catch (ValueFormatException e) {
                log.error(e.getMessage(), e);
            } catch (PathNotFoundException e) {
                log.error(e.getMessage(), e);
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
            ajaxTarget.focusComponent(getComponent());
            return;
        }

        // find the tags and search for the currently changed one
        // I accomplish this by remembering the last tags array and comparing
        // it with the new one, the changed tag is the one we want to query
        String[] tags = searchParam.toString().split(",");
        if (this.previousTags == null) {
            this.previousTags = arrayToHashSet(tags);
            sendResponse(requestCycle, callbackMethod.toString(), "");
            return;
        }
        HashSet<String> tagSet = arrayToHashSet(tags);

        tagSet.removeAll(this.previousTags);

        if (tagSet.isEmpty()) {
            sendResponse(requestCycle, callbackMethod.toString(), "");
            return;
        }

        String tag = tagSet.iterator().next().trim();
        log.debug("Found tag: " + tag);

        SearchResult sr;
        try {
            sr = searchBuilder.search(tag);
        } catch (RepositoryException e) {
            log.error("An error occured during search", e);
            return;
        }
        this.previousTags = arrayToHashSet(tags);
        this.previousTag = tag;

        JSONObject JSONRoot = new JSONObject();
        JSONObject results = JSONObject.fromObject(sr);
        log.debug("JSON: " + results);
        JSONRoot.element("response", results);

        sendResponse(requestCycle, callbackMethod.toString(), JSONRoot.toString());

    }

    private void sendResponse(RequestCycle requestCycle, String method, String args) {
        final String responseStr = method + '(' + args + ");";
        IRequestHandler jsonHandler = new IRequestHandler() {
            @Override
            public void respond(IRequestCycle requestCycle) {
                WebResponse r = (WebResponse) requestCycle.getResponse();

                // Determine encoding
                final String encoding = Application.get().getRequestCycleSettings().getResponseRequestEncoding();
                r.setContentType("application/json; charset=" + encoding);

                // Make sure it is not cached by a
                r.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                r.setHeader("Cache-Control", "no-cache, must-revalidate");
                r.setHeader("Pragma", "no-cache");

                r.write(responseStr);
            }

            @Override
            public void detach(final IRequestCycle requestCycle) {
            }
        };

        requestCycle.scheduleRequestHandlerAfterCurrent(jsonHandler);
    }

    //I guess this should be loaded as a service instead of being an internal class
    private static class SearchBuilder implements IClusterable {
        private static final long serialVersionUID = 1L;

        private static final String QUERY = "//element(*, hippostd:taggable)";

        private static ResultItem[] EMPTY_RESULTS = new ResultItem[0];

        private ResultItem[] doSearch(String value) {
            value = value.trim();
            if (value.length() == 0) {
                return EMPTY_RESULTS;
            }
            StringBuilder query = new StringBuilder(QUERY);

            // UGLY solve this at the client and only send the to be completed tag
            String[] tags = value.split(",");
            String tag = tags[tags.length - 1];
            query.append("[jcr:contains(@hippostd:tags, '").append(tag).append("*')]");

            final String queryString = query.toString();

            log.debug("Executing search query: {}", queryString);

            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            QueryResult result = null;
            try {
                QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
                final String queryType = "xpath";
                HippoQuery hippoQuery = (HippoQuery) queryManager.createQuery(queryString, queryType);
                session.refresh(true);
                result = hippoQuery.execute();
            } catch (RepositoryException e) {
                log.error("Error executing query[" + queryString + ']', e);
            }

            if (result != null) {
                // build response
                try {
                    TagCollection tagCollection = new TagCollection();
                    for (NodeIterator it = result.getNodes(); it.hasNext(); ) {
                        Node node = it.nextNode();
                        try {
                            log.debug("Found document: {}", node.getName());
                            Value[] documentTags = node.getProperty(TaggingNodeType.PROP_TAGS).getValues();
                            for (Value documentTag : documentTags) {
                                if (documentTag.getString().substring(0, tag.length()).equalsIgnoreCase(tag)) {
                                    tagCollection.add(new Tag(documentTag.getString()));
                                    log.debug("Found tag: {}", documentTag.getString());
                                }
                            }
                        } catch (ItemNotFoundException infe) {
                            log.warn("Item not found", infe);
                        } catch (ValueFormatException vfe) {
                            //Should never happen (JT: then log it when it happens ;-)
                            log.error(vfe.getMessage(), vfe);
                        }
                    }
                    ResultItem[] results = new ResultItem[tagCollection.size()];
                    int count = 0;
                    for (IModel t : tagCollection) {
                        ResultItem item = new ResultItem(((Tag) t.getObject()).getName());
                        results[count] = item;
                        count++;
                    }
                    return results;
                } catch (RepositoryException e) {
                    log.error("Error parsing query results[" + queryString + ']', e);
                }
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

        public ResultItem[] getResults() {
            return results;
        }

        public void setResults(ResultItem[] results) {
            this.results = results;
        }

        public SearchResult(ResultItem[] results) {
            this.results = results;
        }
    }

    public static class ResultItem {

        private String label;

        public ResultItem(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

    }

}
