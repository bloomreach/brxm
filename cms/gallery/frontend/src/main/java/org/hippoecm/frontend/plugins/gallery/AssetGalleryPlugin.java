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
package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.Node;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.columns.FallbackAssetGalleryListColumnProvider;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ExpandCollapseListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListPagingDefinition;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.NodeDragBehavior;

public class AssetGalleryPlugin extends ExpandCollapseListingPlugin<Node> {

    private static final long serialVersionUID = 1L;

    public AssetGalleryPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        setClassName("asset-gallery-plugin");
        getSettings().setAutoWidthClassName("assetgallery-name");

        add(CSSPackageResource.getHeaderContribution(AssetGalleryPlugin.class, "AssetGalleryPlugin.css"));
    }

    @Override
    protected ISortableDataProvider<Node> newDataProvider() {
        return new DocumentsProvider(getModel(), new DocumentListFilter(getPluginConfig()),
                getTableDefinition().getComparators());
    }

    @Override
    protected ListDataTable<Node> newListDataTable(String id,
                                                   TableDefinition<Node> tableDefinition,
                                                   ISortableDataProvider<Node> dataProvider,
                                                   ListDataTable.TableSelectionListener<Node> selectionListener,
                                                   boolean triState,
                                                   ListPagingDefinition pagingDefinition) {
        return new ListDataTable<Node>(id, tableDefinition, dataProvider, selectionListener, triState,
                pagingDefinition) {

            @Override
            protected Item newRowItem(String id, int index, IModel model) {
                Item item = super.newRowItem(id, index, model);
                if (model instanceof JcrNodeModel) {
                    JcrNodeModel nodeModel = (JcrNodeModel) model;
                    item.add(new NodeDragBehavior(new DragSettings(YuiPluginHelper.getConfig(getPluginConfig())),
                            nodeModel));
                }
                return item;
            }
        };
    }

    @Override
    protected IListColumnProvider getDefaultColumnProvider() {
        return new FallbackAssetGalleryListColumnProvider();
    }
}
