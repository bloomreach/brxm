/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.reports.plugins.ReportPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtEventAjaxBehavior;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.Reports.BrokenLinksListPanel")
public class BrokenLinksListPanel extends ReportPanel {

    private static final long serialVersionUID = 1L;

    private static final String EVENT_DOCUMENT_SELECTED = "documentSelected";
    private static final String EVENT_DOCUMENT_SELECTED_PARAM_PATH = "path";

    private static final String CONFIG_COLUMNS = "columns";
    private static final String CONFIG_AUTO_EXPAND_COLUMN = "auto.expand.column";
    private static final String TITLE_TRANSLATOR_KEY = "title";
    private static final String NO_DATA_TRANSLATOR_KEY = "no-data";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final CssResourceReference BROKENLINKS_CSS = new CssResourceReference(BrokenLinksListPanel.class, "Hippo.Reports.BrokenLinksList.css");
    private static final JavaScriptResourceReference BROKENLINKS_JS = new JavaScriptResourceReference(BrokenLinksListPanel.class, "Hippo.Reports.BrokenLinksList.js");

    private static final Logger log = LoggerFactory.getLogger(BrokenLinksListPanel.class);

    private final IPluginContext context;
    private final int pageSize;
    private final BrokenLinksListColumns columns;
    private final ExtJsonStore<Object> store;
    private String updateText;

    public BrokenLinksListPanel(final IPluginContext context, final IPluginConfig config, final String query) {
        super(context, config);

        this.context = context;
        pageSize = config.getInt("page.size", DEFAULT_PAGE_SIZE);
        columns = new BrokenLinksListColumns(config.getStringArray(CONFIG_COLUMNS));
        store = new BrokenLinksStore(query, columns, pageSize);
        add(store);

        updateText = null;
        try {
            QueryManager queryManager = UserSession.get().getJcrSession().getWorkspace().getQueryManager();
            QueryResult queryResult = queryManager.createQuery("SELECT * FROM [brokenlinks:config]",
                                                           Query.JCR_SQL2).execute();
            for (NodeIterator brokenlinksConfigs = queryResult.getNodes(); brokenlinksConfigs.hasNext();) {
                Node brokenlinksConfig = brokenlinksConfigs.nextNode();
                if (brokenlinksConfig.isNodeType(HippoNodeType.NT_DOCUMENT) && brokenlinksConfig.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    brokenlinksConfig = brokenlinksConfig.getParent();
                    if (brokenlinksConfig.hasProperty("hippo:request/hipposched:triggers/default/hipposched:nextFireTime")) {
                        Calendar schedule = brokenlinksConfig.getProperty("hippo:request/hipposched:triggers/default/hipposched:nextFireTime").getValue().getDate();
                        if (updateText == null) {
                            updateText = "";
                        } else {
                            updateText += "\n";
                        }
                        updateText += new StringResourceModel("update-known", this).setParameters(schedule.getTime()).getObject();
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Unable to load next fire time from the repository, the status message in the CMS UI will not contain the next fire time", ex);
        }
        if (updateText == null) {
            updateText = new StringResourceModel("update-unknown", this).setDefaultValue("").getObject();
        }
       
        
        addEventListener(EVENT_DOCUMENT_SELECTED, new ExtEventListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(final AjaxRequestTarget ajaxRequestTarget, Map<String, JSONArray> parameters) {
                String path = getParameterOrNull(parameters, EVENT_DOCUMENT_SELECTED_PARAM_PATH);
                if (path != null) {
                    browseToDocument(path);
                }
            }
        });
    }

    private static String getParameterOrNull(final Map<String, JSONArray> parameters, final String name) {
        final JSONArray values = parameters.get(name);
        if (values != null && values.length() > 0 && !values.isNull(0)) {
            try {
                return values.getString(0);
            } catch (JSONException e) {
                log.warn("Ignoring parameter '{}', element 0 of {} cannot be parsed as a string", name, values.toString(), e);
            }
        }
        return null;
    }

    @Override
    protected ExtEventAjaxBehavior newExtEventBehavior(final String event) {
        switch (event) {
            case EVENT_DOCUMENT_SELECTED:
                return new ExtEventAjaxBehavior(EVENT_DOCUMENT_SELECTED_PARAM_PATH);
            default:
                return super.newExtEventBehavior(event);
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(BROKENLINKS_CSS));
        response.render(JavaScriptHeaderItem.forReference(BROKENLINKS_JS));
    }

    private Node getNode(String path) {
        try {
            Session session = UserSession.get().getJcrSession();
            return session.getNode(path);
        } catch (RepositoryException e) {
            log.warn("Unable to get the node " + path, e);
        }
        return null;
    }

    private void browseToDocument(String path) {
        if (path == null || path.length() == 0) {
            log.warn("No document path to browse to");
            return;
        }

        final Node node = getNode(path);
        if (node == null) {
            return;
        }

        JcrNodeModel nodeModel = new JcrNodeModel(node);

        @SuppressWarnings("unchecked") // IBrowseService always uses a JcrNodeModel
        IBrowseService<JcrNodeModel> browseService = context.getService("service.browse", IBrowseService.class);

        browseService.browse(nodeModel);
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        properties.put("columns", getColumnsConfig());
        properties.put("store", new JSONIdentifier(store.getJsObjectId()));
        properties.put("pageSize", this.pageSize);
        properties.put("paging", config.getAsBoolean("paging", true));
        properties.put("noDataText", getString(NO_DATA_TRANSLATOR_KEY));
        properties.put("updateText", this.updateText);

        if (config.containsKey(CONFIG_AUTO_EXPAND_COLUMN)) {
            String autoExpandColumn = config.getString(CONFIG_AUTO_EXPAND_COLUMN);

            if (!columns.containsColumn(autoExpandColumn)) {
                // prevent an auto-expand column that is not an actual column name, otherwise ExtJs stops rendering
                log.warn("Ignoring unknown auto-expand column '{}'", autoExpandColumn);
            } else {
                properties.put("autoExpandColumn", autoExpandColumn);
            }
        }

        properties.put("title", getString(TITLE_TRANSLATOR_KEY));
    }

    private JSONArray getColumnsConfig() throws JSONException {
        JSONArray result = new JSONArray();

        for (IBrokenLinkDocumentListColumn column: columns.getAllColumns()) {
            JSONObject config = column.getExtColumnConfig();
            if (config != null) {
                result.put(config);
            }
        }

        return result;
    }

}
