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
package org.hippoecm.frontend.plugins.cms.browse.list.types;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IStyledColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.IJcrNodeViewerFactory;
import org.hippoecm.frontend.plugins.standards.list.datatable.CustomizableDocumentListingDataTable;
import org.hippoecm.frontend.plugins.standards.list.resolvers.NameResolver;
import org.hippoecm.frontend.service.ITitleDecorator;

public class TypesListingPlugin extends AbstractListingPlugin implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: DocumentListingPlugin.java 12651 2008-07-18 11:59:05Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    public TypesListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected void onSelect(JcrNodeModel model, AjaxRequestTarget target) {
        try {
            if (model.getNode().getParent() != null) {
                setModel(model);
                return;
            }
        } catch (RepositoryException ex) {
        }
        setModel(new JcrNodeModel((javax.jcr.Node) null));
    }

    @Override
    protected Component getTable(String wicketId, ISortableDataProvider provider, int pageSize, int viewSize) {
        List<IStyledColumn> columns = createTableColumns();
        CustomizableDocumentListingDataTable dataTable = new CustomizableDocumentListingDataTable(wicketId, columns,
                provider, pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        return dataTable;
    }

    @Override
    protected List<IStyledColumn> createTableColumns() {
        List<IStyledColumn> columns = new ArrayList<IStyledColumn>();
        columns.add(getNodeColumn(new Model(""), "icon", new IJcrNodeViewerFactory() {
            private static final long serialVersionUID = 1L;

            public Component getViewer(String id, JcrNodeModel node) {
                Component result = new Label(id);
                result.add(new AttributeModifier("class", true, new Model("document-16")));
                return result;
            }
        }));
        columns.add(getNodeColumn(new Model("Name"), "name", new NameResolver()));
        columns.add(getNodeColumn(new Model("Type"), "type", new IJcrNodeViewerFactory() {
            private static final long serialVersionUID = 1L;

            public Component getViewer(String id, JcrNodeModel node) {
                return new Label(id, "Document type");
            }
        }));

        return columns;
    }

    public String getTitle() {
        return "Document listing";
    }

}
