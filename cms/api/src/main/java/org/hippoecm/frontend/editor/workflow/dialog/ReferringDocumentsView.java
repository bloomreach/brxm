/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow.dialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.workflow.model.ReferringDocumentsProvider;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.RowSelector;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributeModifier;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferringDocumentsView extends Panel implements IPagingDefinition {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReferringDocumentsView.class);

    private ReferringDocumentsProvider provider;
    private IEditorManager editorMgr;
    private ListDataTable dataTable;
    private WebMarkupContainer actionContainer;
    private List<JcrNodeModel> selectedDocuments = new LinkedList<JcrNodeModel>();

    public ReferringDocumentsView(String id, ReferringDocumentsProvider provider, IEditorManager mgr) {
        super(id);

        this.provider = provider;
        this.editorMgr = mgr;

        setOutputMarkupId(true);

        add(new Label("message", new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                ReferringDocumentsProvider provider = ReferringDocumentsView.this.provider;
                if (provider.getNumResults() < 0) {
                    return new StringResourceModel("message-thousands", ReferringDocumentsView.this, new Model(provider))
                            .getObject();
                } else if (provider.getNumResults() > provider.getLimit()) {
                    return new StringResourceModel("message-many", ReferringDocumentsView.this, new Model(provider))
                            .getObject();
                } else if (provider.size() > 1) {
                    return new StringResourceModel("message", ReferringDocumentsView.this, new Model(provider))
                            .getObject();
                } else if (provider.size() == 1) {
                    return new StringResourceModel("message-single", ReferringDocumentsView.this, null).getObject();
                } else {
                    return new StringResourceModel("message-empty", ReferringDocumentsView.this, null).getObject();
                }
            }

        }));

        dataTable = new ListDataTable("datatable", getTableDefinition(), provider, new TableSelectionListener() {
            public void selectionChanged(IModel model) {
            }

        }, true, this);
        add(dataTable);

        add(actionContainer = new WebMarkupContainer("actions"));

        add(new CssClassAppender(new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (ReferringDocumentsView.this.provider.size() == 0) {
                    return "hippo-empty";
                }
                return "";
            }
        }));

        AjaxLink selectAll = new AjaxLink("select-all") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedDocuments.clear();
                ReferringDocumentsProvider provider = ReferringDocumentsView.this.provider;
                Iterator<?> iter = provider.iterator(0, provider.size());
                while (iter.hasNext()) {
                    selectedDocuments.add((JcrNodeModel) provider.model(iter.next()));
                }
                target.add(ReferringDocumentsView.this);
            }
        };
        actionContainer.add(selectAll);

        AjaxLink selectNone = new AjaxLink("select-none") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedDocuments.clear();
                target.add(ReferringDocumentsView.this);
            }
        };
        actionContainer.add(selectNone);

        Button open = new AjaxButton("open") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (editorMgr != null) {
                    for (JcrNodeModel model : selectedDocuments) {
                        IEditor editor = editorMgr.getEditor(model);
                        if (editor == null) {
                            try {
                                editorMgr.openEditor(model);
                            } catch (ServiceException ex) {
                                log.error("Could not open editor for " + model.getItemModel().getPath(), ex);
                            }
                        }
                    }
                }
                onOpen();
            }
        };
        open.setModel(new StringResourceModel("open", this, null));
        if (editorMgr == null) {
            open.setEnabled(false);
        }
        actionContainer.add(open);

        add(new CssClassAppender(new Model("hippo-referring-documents")));
    }

    @Override
    protected void onBeforeRender() {
        boolean hasLinks = (provider.size() > 0);
        dataTable.setVisible(hasLinks);
        actionContainer.setVisible(hasLinks);
        super.onBeforeRender();
    }

    protected void onOpen() {
    }

    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new Model(""), null);
        column.setRenderer(new RowSelector(selectedDocuments));
        columns.add(column);

        column = new ListColumn(new StringResourceModel("doclisting-name", this, null), null);
        column.setAttributeModifier(new DocumentAttributeModifier());
        column.setCssClass("doclisting-name");
        columns.add(column);

        column = new ListColumn(new StringResourceModel("doclisting-state", this, null), null);
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new StateIconAttributeModifier());
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
