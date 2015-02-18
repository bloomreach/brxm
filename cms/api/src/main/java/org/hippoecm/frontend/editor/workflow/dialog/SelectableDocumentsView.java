/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.RowSelector;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeIconAndStateRenderer;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectableDocumentsView extends Panel implements IPagingDefinition {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SelectableDocumentsView.class);
    private static final Model<String> EMPTY_STRING_MODEL = Model.of(StringUtils.EMPTY);

    private ISortableDataProvider<Node, String> provider;
    private IEditorManager editorMgr;
    private ListDataTable dataTable;
    private WebMarkupContainer actionContainer;
    private List<IModel<Node>> selectedDocuments = new LinkedList<IModel<Node>>();

    public SelectableDocumentsView(final String id,
                                   final IModel<String> message,
                                   final ISortableDataProvider<Node, String> provider,
                                   final IEditorManager editorManager) {
        super(id);

        this.provider = provider;
        this.editorMgr = editorManager;

        setOutputMarkupId(true);

        add(new Label("message", message));

        dataTable = new ListDataTable("datatable", getTableDefinition(), provider, new TableSelectionListener() {
            public void selectionChanged(IModel model) {
            }
        }, true, this);
        dataTable.add(CssClass.append(DocumentListColumn.DOCUMENT_LIST_CSS_CLASS));
        add(dataTable);

        add(actionContainer = new WebMarkupContainer("actions"));

        add(CssClass.append("hippo-selectable-documents"));
        add(CssClass.append(new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                if (SelectableDocumentsView.this.provider.size() == 0) {
                    return "hippo-empty";
                }
                return "";
            }
        }));

        AjaxLink selectAll = new AjaxLink("select-all") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedDocuments.clear();
                ISortableDataProvider<Node, String> provider = SelectableDocumentsView.this.provider;
                Iterator<? extends Node> iter = provider.iterator(0, provider.size());
                while (iter.hasNext()) {
                    selectedDocuments.add(provider.model(iter.next()));
                }
                target.add(SelectableDocumentsView.this);
            }
        };
        actionContainer.add(selectAll);

        AjaxLink selectNone = new AjaxLink("select-none") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedDocuments.clear();
                target.add(SelectableDocumentsView.this);
            }
        };
        actionContainer.add(selectNone);

        Button openBtn = new AjaxButton("open") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                openEditor();
            }
        };
        openBtn.setModel(Model.of(getString("open")));
        if (editorMgr == null) {
            openBtn.setEnabled(false);
        }
        actionContainer.add(openBtn);
    }

    @Override
    protected void onBeforeRender() {
        boolean hasLinks = (provider.size() > 0);
        dataTable.setVisible(hasLinks);
        actionContainer.setVisible(hasLinks);
        super.onBeforeRender();
    }

    private void openEditor() {
        if (editorMgr != null) {
            for (IModel<Node> model : selectedDocuments) {
                IEditor editor = editorMgr.getEditor(model);
                if (editor == null) {
                    try {
                        editorMgr.openEditor(model);
                    } catch (ServiceException ex) {
                        log.error("Could not open editor", ex);
                    }
                }
            }
        }
        onOpen();
    }

    protected void onOpen() {
    }

    protected TableDefinition getTableDefinition() {
        return new TableDefinition(Arrays.asList(
                createSelectorColumn(),
                createIconColumn(),
                createNameColumn()
        ));
    }

    private ListColumn<Node> createSelectorColumn() {
        final ListColumn<Node> column = new ListColumn<>(EMPTY_STRING_MODEL, null);
        column.setRenderer(new RowSelector(selectedDocuments));
        column.setCssClass(DocumentListColumn.SELECTOR.getCssClass());
        return column;
    }

    private ListColumn<Node> createIconColumn() {
        ListColumn<Node> column = new ListColumn<>(EMPTY_STRING_MODEL, null);
        column.setRenderer(TypeIconAndStateRenderer.getInstance());
        column.setCssClass(DocumentListColumn.ICON.getCssClass());
        column.setLink(false);
        return column;
    }

    private ListColumn<Node> createNameColumn() {
        final IModel<String> displayModel = Model.of(getString("doclisting-name"));
        final ListColumn<Node> column = new ListColumn(displayModel, null);
        column.setCssClass(DocumentListColumn.NAME.getCssClass());
        column.setLink(false);
        return column;
    }

    public int getPageSize() {
        return 3;
    }

    public int getViewSize() {
        return 5;
    }

}
