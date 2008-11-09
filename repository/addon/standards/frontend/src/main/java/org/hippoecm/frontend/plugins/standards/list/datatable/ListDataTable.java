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
package org.hippoecm.frontend.plugins.standards.list.datatable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class ListDataTable extends DataTable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private TableSelectionListener selectionListener;

    public interface TableSelectionListener {
        public void selectionChanged(IModel model);
    }

    public ListDataTable(String id, TableDefinition tableDefinition, ISortableDataProvider dataProvider,
            TableSelectionListener selectionListener, int rowsPerPage, final boolean triState) {
        super(id, tableDefinition.getColumns(), dataProvider, rowsPerPage);
        setOutputMarkupId(true);
        setVersioned(false);

        this.selectionListener = selectionListener;

        if (tableDefinition.showColumnHeaders()) {
            addTopToolbar(new AjaxFallbackHeadersToolbar(this, dataProvider) {
                private static final long serialVersionUID = 1L;

                @Override
                protected WebMarkupContainer newSortableHeader(String borderId, String property,
                        ISortStateLocator locator) {
                    return new ListTableHeader(borderId, property, locator, ListDataTable.this, triState);
                }
            });
        }
        addBottomToolbar(new ListNavigationToolBar(this));
    }

    @Override
    protected Item newRowItem(String id, int index, final IModel model) {
        OddEvenItem item = new OddEvenItem(id, index, model);

        IModel selected = getModel();
        if (selected != null && selected.equals(model)) {
            item.add(new AttributeAppender("class", new Model("hippo-list-selected"), " "));
        }

        item.add(new AjaxEventBehavior("onclick") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                selectionListener.selectionChanged(model);
            }
        });

        item.add(new AttributeAppender("title", getDocumentType(model), " "));

        return item;
    }

    public TableSelectionListener getSelectionListener() {
        return selectionListener;
    }

    private IModel getDocumentType(IModel model) {
        IModel documentType = new Model("unknown");
        boolean isFolder = true;
        if (model instanceof JcrNodeModel) {
            HippoNode node = (HippoNode) model.getObject();
            try {
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    isFolder = false;
                    NodeIterator nodeIt = node.getNodes();
                    while (nodeIt.hasNext()) {
                        Node childNode = nodeIt.nextNode();
                        if (childNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            documentType = new TypeTranslator(new JcrNodeTypeModel(childNode.getPrimaryNodeType())).getTypeName();
                            break;
                        }
                    }
                } else {
                    documentType = new TypeTranslator(new JcrNodeTypeModel(node.getPrimaryNodeType())).getTypeName();
                }
            } catch (RepositoryException e) {
            }
        }
        if (isFolder) {
            return new StringResourceModel("folder-title", this, null, new Object[] { documentType });
        } else {
            return new StringResourceModel("document-title", this, null, new Object[] { documentType });
        }
    }

}
