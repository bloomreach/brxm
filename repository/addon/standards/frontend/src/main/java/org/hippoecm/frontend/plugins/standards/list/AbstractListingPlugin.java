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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrNodeModelComparator;
import org.hippoecm.frontend.model.SortableDataAdapter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractListingPlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_VIEW_SIZE = 5;

    private int pageSize;
    private int viewSize;

    private SortableDataAdapter<JcrNodeModel> provider;
    private Map<String, Comparator<? super JcrNodeModel>> compare;

    public AbstractListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // register for flush notifications
        context.registerService(this, IJcrService.class.getName());

        compare = new HashMap<String, Comparator<? super JcrNodeModel>>();
        compare.put("name", new JcrNodeModelComparator("name"));
        compare.put(JcrConstants.JCR_PRIMARYTYPE, new JcrNodeModelComparator(JcrConstants.JCR_PRIMARYTYPE));
        compare.put("state", new JcrNodeModelComparator("state"));
        add(new EmptyPanel("table"));
        
        pageSize = DEFAULT_PAGE_SIZE;
        viewSize = DEFAULT_VIEW_SIZE;
        createTableColumns();

        modelChanged();
    }

    public void setDataProvider(IDataProvider provider) {
        this.provider = new SortableDataAdapter<JcrNodeModel>(provider, compare);
        this.provider.setSort("name", true);

        Component table = getTable("table", this.provider, pageSize, viewSize);
        table.setModel(getModel());
        replace(table);
    }


    @Override
    public void onModelChanged() {
        super.onModelChanged();
        // calculate list of node models
        // FIXME: move into separate service
        JcrNodeModel model = (JcrNodeModel) getModel();
        final List<JcrNodeModel> entries = new ArrayList<JcrNodeModel>();
        Node node = (Node) model.getNode();
        try {
            while (node != null) {
                if (!(node.isNodeType(HippoNodeType.NT_DOCUMENT) && !node.isNodeType("hippostd:folder"))
                        && !node.isNodeType(HippoNodeType.NT_HANDLE) && !node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)
                        && !node.isNodeType(HippoNodeType.NT_REQUEST) && !node.isNodeType("rep:root")) {
                    NodeIterator childNodesIterator = node.getNodes();
                    while (childNodesIterator.hasNext()) {
                        entries.add(new JcrNodeModel(childNodesIterator.nextNode()));
                    }
                    break;
                }
                if (!node.isNodeType("rep:root")) {
                    model = model.getParentModel();
                    node = model.getNode();
                } else {
                    break;
                }
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        setDataProvider(new ListDataProvider(entries) {
            private static final long serialVersionUID = 1L;

            @Override
            public void detach() {
                for (JcrNodeModel entry : entries) {
                    entry.detach();
                }
                super.detach();
            }
        });
        redraw();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        if (nodeModel.getParentModel() != null) {
            String nodePath = nodeModel.getParentModel().getItemModel().getPath();
            String myPath = ((JcrNodeModel) getModel()).getItemModel().getPath();
            if (myPath.startsWith(nodePath)) {
                modelChanged();
            }
        } else {
            modelChanged();
        }
    }

    /**
     * Called when node is selected.
     */
    protected void onSelect(JcrNodeModel model, AjaxRequestTarget target) {
    }

    @Override
    protected void onDetach() {
        if (provider != null) {
            provider.detach();
        }
        super.onDetach();
    }

    // internals

    protected abstract List<IStyledColumn> createTableColumns();

    protected IStyledColumn getNodeColumn(Model model, String propertyName, IJcrNodeViewerFactory resolver) {
        return new NodeColumn(model, propertyName, resolver, this);
    }

    protected abstract Component getTable(String wicketId, ISortableDataProvider provider, int pageSize, int viewSize);

}
