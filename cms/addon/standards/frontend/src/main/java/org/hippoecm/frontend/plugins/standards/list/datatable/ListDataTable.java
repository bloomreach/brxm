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

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;

public class ListDataTable extends DataTable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private AbstractToolbar ajaxTopNavigationToolbar;
    private AbstractToolbar ajaxBottomNavigationToolbar;
    private AbstractToolbar ajaxFallbackTopHeadersToolbar;
    private AbstractToolbar ajaxFallbackBottomHeadersToolbar;

    private ISortableDataProvider dataProvider;

    public ListDataTable(String id, TableDefinition columns,
            ISortableDataProvider dataProvider, int rowsPerPage) {

        super(id, columns.asArray(), dataProvider, rowsPerPage);

        setOutputMarkupId(true);
        setVersioned(false);
        this.dataProvider = dataProvider;
    }
    
    @Override
    protected Item newRowItem(String id, int index, IModel model) {
        OddEvenItem item = new OddEvenItem(id, index, model);

        // check if a node in the list has been selected, if yes append appropriate CSS class
        JcrNodeModel selectedNode = (JcrNodeModel) getModel();
        if (selectedNode != null
                && model instanceof JcrNodeModel
                && (selectedNode.equals((JcrNodeModel) model) || (selectedNode.getParentModel() != null && selectedNode
                        .getParentModel().equals((JcrNodeModel) model)))) {
            item.add(new AttributeAppender("class", new Model("selected"), " "));
        }

        return item;
    }

    public void addTopPaging() {
        if (ajaxTopNavigationToolbar == null) {
            ajaxTopNavigationToolbar = new ListNavigationToolBar(this);
        }
        if (!this.contains(ajaxTopNavigationToolbar, true)) {
            super.addTopToolbar(ajaxTopNavigationToolbar);
        }
    }

    public void addTopColumnHeaders() {
        if (ajaxFallbackTopHeadersToolbar == null) {
            ajaxFallbackTopHeadersToolbar = new AjaxFallbackHeadersToolbar(this, dataProvider);
        }
        if (!this.contains(ajaxFallbackTopHeadersToolbar, true)) {
            super.addTopToolbar(ajaxFallbackTopHeadersToolbar);
        }
    }

    public void addBottomPaging() {
        if (ajaxBottomNavigationToolbar == null) {
            ajaxBottomNavigationToolbar = new ListNavigationToolBar(this);
        }
        if (!this.contains(ajaxBottomNavigationToolbar, true)) {
            super.addBottomToolbar(ajaxBottomNavigationToolbar);
        }
    }

    public void addBottomColumnHeaders() {
        if (ajaxFallbackBottomHeadersToolbar == null) {
            ajaxFallbackBottomHeadersToolbar = new AjaxFallbackHeadersToolbar(this, dataProvider);
        }
        if (!this.contains(ajaxFallbackBottomHeadersToolbar, true)) {
            super.addBottomToolbar(ajaxFallbackBottomHeadersToolbar);
        }
    }
}
