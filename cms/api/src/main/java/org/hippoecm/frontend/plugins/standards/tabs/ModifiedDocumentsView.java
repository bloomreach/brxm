/*
 *  Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

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
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;

class ModifiedDocumentsView extends Panel implements IPagingDefinition {

    private final ListDataTable<Node> dataTable;
    private final ModifiedDocumentsProvider provider;

    public ModifiedDocumentsView(String id, final ModifiedDocumentsProvider provider) {
        super(id);

        setOutputMarkupId(true);
        this.provider = provider;

        add(new Label("message", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                IModel<String> message = Model.of("");
                if (provider.size() > 1) {
                    message = new StringResourceModel("message", ModifiedDocumentsView.this)
                            .setModel(Model.of(provider));
                } else if (provider.size() == 1) {
                    message = new StringResourceModel("message-single", ModifiedDocumentsView.this);
                }
                return message.getObject();
            }
        }));

        dataTable = new ListDataTable<>("datatable", getTableDefinition(), this.provider, model -> {}, true, this);
        add(dataTable);

        add(CssClass.append(new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return ModifiedDocumentsView.this.provider.size() == 0 ? "hippo-empty" : "";
            }
        }));

        add(CssClass.append("hippo-referring-documents"));
    }

    protected TableDefinition<Node> getTableDefinition() {
        final List<ListColumn<Node>> columns = Collections.singletonList(createNameColumn());
        return new TableDefinition<>(columns);
    }

    private ListColumn<Node> createNameColumn() {
        final Model<String> displayModel = Model.of(getString("doclisting-name"));
        final ListColumn<Node> column = new ListColumn<>(displayModel, null);
        column.setAttributeModifier(DocumentAttributeModifier.getInstance());
        return column;
    }

    public int getPageSize() {
        return 7;
    }

    public int getViewSize() {
        return 5;
    }

    @Override
    protected void onDetach() {
        provider.detach();
        super.onDetach();
    }
}
