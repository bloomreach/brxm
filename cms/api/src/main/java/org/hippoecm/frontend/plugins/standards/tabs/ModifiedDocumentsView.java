/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.tabs;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModifiedDocumentsView extends Panel implements IPagingDefinition {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ModifiedDocumentsView.class);

    private ListDataTable dataTable;
    private ModifiedDocumentsProvider provider;

    public ModifiedDocumentsView(String id, final ModifiedDocumentsProvider provider) {
        super(id);

        setOutputMarkupId(true);
        this.provider = provider;

        add(new Label("message", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (provider.size() > 1) {
                    return new StringResourceModel("message", ModifiedDocumentsView.this, new Model(provider))
                            .getObject();
                } else if (provider.size() == 1) {
                    return new StringResourceModel("message-single", ModifiedDocumentsView.this, null).getObject();
                } else {
                    return "";
                }
            }

        }));

        dataTable = new ListDataTable("datatable", getTableDefinition(), this.provider, new ListDataTable.TableSelectionListener(){
            public void selectionChanged(IModel iModel) {
                //Do Nothing for now
            }
        }, true, this);
        add(dataTable);

        add(new CssClassAppender(new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (ModifiedDocumentsView.this.provider.size() == 0) {
                    return "hippo-empty";
                }
                return "";
            }
        }));


        add(new CssClassAppender(new Model<String>("hippo-referring-documents")));
    }


    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new StringResourceModel("doclisting-name", this, null), null);
        column.setAttributeModifier(new DocumentAttributeModifier());
        columns.add(column);

        return new TableDefinition(columns);
    }

    public int getPageSize() {
        return 7;
    }

    public int getViewSize() {
        return 5;
    }

}
