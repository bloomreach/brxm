/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.poll.cms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Render class for the extra field in the poll document. 
 * Contains the logic to retrieve the number of votes for the current poll.
 */
public class PollResultsPlugin extends RenderPlugin<Object> implements IHeaderContributor {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PollResultsPlugin.class);

    // keys in the properties files
    private static final String DIALOG_NAME_LABEL = "name-label";
    private static final String DIALOG_OPTIONSVALUE_LABEL = "optionsvalue-label";
    private static final String DIALOG_COUNT_LABEL = "count-label";

    // poll data root path
    private static final String POLLDATA_ROOT_DEFAULT = "/polldata";
    private static final String CONFIG_POLL_DATA_ROOT_PATH = "pollDataRootPath";
    private String pollDataRootPath = POLLDATA_ROOT_DEFAULT;

    private static final String CONTENT_DOCUMENTS_PATH = "/content/documents/";

    private static final CssResourceReference CSS = new CssResourceReference(PollResultsPlugin.class, "PollResultsPlugin.css");

    private static final String NAMESPACE_POLL_COMPOUND = "poll:poll";
    private static final String NAMESPACE_POLL_OPTION = "poll:option";
    private static final String NAMESPACE_POLL_VALUE = "poll:value";
    private static final String NAMESPACE_POLL_COUNT = "poll:count";

    public PollResultsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final Map<String, Long> results = getPollResults(config);

        // add result-objects to refreshing view to make them available for Wicket page rendering
        RefreshingView<PollResult> refreshingView = new RefreshingView<PollResult>("row") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<PollResult>> getItemModels() {
                List<IModel<PollResult>> list = new ArrayList<>();
                for (Map.Entry<String, Long> entry : results.entrySet()) {
                    list.add(new Model<>(new PollResult(entry.getKey(), entry.getValue())));
                }
                return list.iterator();
            }

            @Override
            protected void populateItem(Item<PollResult> item) {
                PollResult result = item.getModelObject();
                item.add(new Label("option", result.getValue() ));
                item.add(new Label("count", result.getCountAsString()));
            }
        };

        final Fragment resultsFragment = new Fragment("poll-results-panel","poll-results", this);

        final IModel<String> nameLabelModel = getResourceModel(DIALOG_NAME_LABEL);
        resultsFragment.add(new Label(DIALOG_NAME_LABEL, nameLabelModel));

        final IModel<String> optionLabelModel = getResourceModel(DIALOG_OPTIONSVALUE_LABEL);
        resultsFragment.add(new Label(DIALOG_OPTIONSVALUE_LABEL, optionLabelModel));

        final IModel<String> countLabelModel = getResourceModel(DIALOG_COUNT_LABEL);
        resultsFragment.add(new Label(DIALOG_COUNT_LABEL, countLabelModel));

        resultsFragment.add(refreshingView);
        add(resultsFragment);
    }

    /**
     * Get the poll results as a map from String (option values) to Long (number of votes).
     */
    protected Map<String, Long> getPollResults(final IPluginConfig config) {

        final Map<String, Long> results = new LinkedHashMap<>();

        if (config.containsKey(CONFIG_POLL_DATA_ROOT_PATH)) {
            pollDataRootPath = config.getString(CONFIG_POLL_DATA_ROOT_PATH);
            log.debug("{} is configured to be {}", CONFIG_POLL_DATA_ROOT_PATH, pollDataRootPath);
        }

        try {
            final Session jcrSession = getSession().getJcrSession();
            final Node pollNode = (Node) getModelObject();

            // find the poll compound that this plugin works on
            Node pollCompoundNode = pollNode;
            if (!pollCompoundNode.isNodeType(NAMESPACE_POLL_COMPOUND)) {
                final NodeIterator it = pollNode.getNodes();
                while (it.hasNext()) {
                    final Node next =  it.nextNode();
                    if (next.isNodeType(NAMESPACE_POLL_COMPOUND)) {
                        pollCompoundNode = next;
                        break;
                    }
                }
            }

            if (!pollCompoundNode.isNodeType(NAMESPACE_POLL_COMPOUND)) {
                throw new WicketRuntimeException("PollResultsPlugin plugin should be configured to a " + NAMESPACE_POLL_COMPOUND
                        + "compound node, or a parent (document) node containing such compound. Current node type is " +
                        pollNode.getPrimaryNodeType().getName() + ", path is " + pollNode.getPath());
            }

            // Load all possible options in the result map, give them the default count 0
            NodeIterator optionNodes = pollCompoundNode.getNodes(NAMESPACE_POLL_OPTION);
            while (optionNodes.hasNext()) {
                final Node next = (Node) optionNodes.next();
                final String value = next.getProperty(NAMESPACE_POLL_VALUE).getString();
                results.put(value, 0L);
            }

            final String pollDataPath = getPollDataPath(pollNode);

            if (jcrSession.nodeExists(pollDataPath)) {

                // Load all option counts from the polldata folder (the actual results)
                final Node resultNode = jcrSession.getNode(pollDataPath);
                final NodeIterator voteNodes = resultNode.getNodes();
                while (voteNodes.hasNext()) {
                    final Node next = (Node) voteNodes.next();
                    results.put(next.getName(), next.getProperty(NAMESPACE_POLL_COUNT).getLong());
                }
            }
            else {
                log.info("Could not find results node on full path location {}. " +
                        "Please update the configuration of the PollComponent", pollDataPath);
            }

        } catch (RepositoryException e) {
            log.error("RepositoryException while reading poll results", e);
        }
        return results;
    }

    /**
     * Adds the css to the html header.
     */
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
    }


    /**
     * Get a property from the plugin config, or from this plugin's properties file.
     *
     * @param key the key under which the property is stored
     * @return a string property model
     */
    protected IModel<String> getResourceModel(final String key) {
        final IPluginConfig localeConfig = getPluginConfig().getPluginConfig(getSession().getLocale().toString());
        if (localeConfig != null) {
            final String property = localeConfig.getString(key, "_" + key + "_");
            return new Model<>(property);
        }

        // default: from properties
        return new StringResourceModel(key, this, null);
    }
    
    /**
     * Get the path where the data (votes) are stored below the data root (normally /polldata).
     *
     * @param pollNode poll compound node, type poll:poll
     * @return a full path of pollData node relative to the pollDataRootNode
     * @throws javax.jcr.RepositoryException
     *
     */
    protected String getPollDataPath(Node pollNode) throws RepositoryException {

        // find handle parent
        Node handle = pollNode;
        while (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
            handle = handle.getParent();
        }

        String relativeDataPath = handle.getPath();

        // strip /content/documents/
        if (relativeDataPath.startsWith(CONTENT_DOCUMENTS_PATH)) {
            relativeDataPath = relativeDataPath.substring(CONTENT_DOCUMENTS_PATH.length());
        }

        // prefix with /polldata/
        final String fullDataPath = pollDataRootPath + "/" + relativeDataPath;
        if (pollNode.getSession().nodeExists(fullDataPath)) {
            log.debug("Found poll data path {} ", fullDataPath);
            return fullDataPath;
        }

        // As a backup method, try to find a path created before version 1.08.01 that didn't contain the site content
        // base path yet. So strip one more level.
        final String alternateDataPath = pollDataRootPath + relativeDataPath.substring(relativeDataPath.indexOf("/"));
        if (pollNode.getSession().nodeExists(alternateDataPath)) {
            log.debug("Found alternate, already existing poll data path {} ", alternateDataPath);
            return alternateDataPath;
        }

        // return path that doesn't exist (yet)
        return fullDataPath;
    }
}
