/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.list.datatable;

import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.hippoecm.cmsprototype.frontend.plugins.list.datatable.paging.CustomizableNavigationToolBar;

public class CustomizableDocumentListingDataTable extends DataTable {

    private static final long serialVersionUID = 1L;
    
    private AbstractToolbar ajaxTopNavigationToolbar;
    private AbstractToolbar ajaxBottomNavigationToolbar;
    private AbstractToolbar ajaxFallbackTopHeadersToolbar;
    private AbstractToolbar ajaxFallbackBottomHeadersToolbar;
    
    private ISortableDataProvider dataProvider;
    
    public CustomizableDocumentListingDataTable(String id, List columns, ISortableDataProvider dataProvider, int rowsPerPage, boolean defaultsOn) {
        this(id, (IColumn[])columns.toArray(new IColumn[columns.size()]), dataProvider, rowsPerPage, defaultsOn);
    }

    /**
     * Constructor
     * 
     * @param id
     *            component id
     * @param columns
     *            list of IColumn objects
     * @param dataProvider
     *            imodel for data provider
     * @param rowsPerPage
     *            number of rows per page
     */
    public CustomizableDocumentListingDataTable(String id, IColumn[] columns, ISortableDataProvider dataProvider, int rowsPerPage, boolean defaultsOn) {
        super(id, columns, dataProvider, rowsPerPage);
        setOutputMarkupId(true);
        setVersioned(false);
        this.dataProvider = dataProvider;
        if(defaultsOn) {
            addTopColumnHeaders();
            addBottomPaging();
        }
    }
    
    protected Item newRowItem(String id, int index, IModel model)
    {
        return new OddEvenItem(id, index, model);
    }
    
    /**
     * Adds default top paging above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public void addTopPaging(){
        if(ajaxTopNavigationToolbar == null ) {
            ajaxTopNavigationToolbar = new AjaxNavigationToolbar(this);
        } 
        if(!this.contains(ajaxTopNavigationToolbar, true)) { 
            super.addTopToolbar(ajaxTopNavigationToolbar);
        } 
    }
    
    /**
     * Adds default ajax top headers above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public void addTopColumnHeaders() {
        if(ajaxFallbackTopHeadersToolbar == null ) {
            ajaxFallbackTopHeadersToolbar = new AjaxFallbackHeadersToolbar(this, dataProvider);
        }
        if(!this.contains(ajaxFallbackTopHeadersToolbar, true)) { 
            super.addTopToolbar(ajaxFallbackTopHeadersToolbar);
        } 
    }

    /**
     * Add default ajax bottom paging above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public void addBottomPaging() {
        if(ajaxBottomNavigationToolbar == null ) {
            ajaxBottomNavigationToolbar = new CustomizableNavigationToolBar(this);
        } 
        if(!this.contains(ajaxBottomNavigationToolbar, true)) { 
            super.addBottomToolbar(ajaxBottomNavigationToolbar);
        } 
    }

    /**
     * Adds default ajax bottom headers above document listing. Calling this method multiple times
     * will not duplicate the paging, so only calling it ones makes sense.
     *
     */
    public void addBottomColumnHeaders() {
        if(ajaxFallbackBottomHeadersToolbar == null ) {
            ajaxFallbackBottomHeadersToolbar = new AjaxFallbackHeadersToolbar(this, dataProvider);
        }
        if(!this.contains(ajaxFallbackBottomHeadersToolbar, true)) { 
            super.addBottomToolbar(ajaxFallbackBottomHeadersToolbar);
        } 
    }

}
