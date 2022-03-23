/*
 *  Copyright 2015-2022 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.ajax.BrLink;
import org.hippoecm.frontend.ajax.NoDoubleClickAjaxLink;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.RowSelector;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TypeIconAndStateRenderer;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.skin.DocumentListColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectableDocumentsView extends Panel implements IPagingDefinition {

    private static final Logger log = LoggerFactory.getLogger(SelectableDocumentsView.class);

    private static final Model<String> EMPTY_STRING_MODEL = Model.of(StringUtils.EMPTY);

    private ISortableDataProvider<Node, String> provider;
    private IEditorManager editorMgr;
    private ListDataTable dataTable;
    private WebMarkupContainer actionContainer;
    private AjaxLink<Void> openButton;
    private List<IModel<Node>> selectedDocuments = new LinkedList<>();

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
        dataTable.add(ClassAttribute.append(DocumentListColumn.DOCUMENT_LIST_CSS_CLASS));

        dataTable.add(ClassAttribute.append(() -> provider.size() > getPageSize()
                ? "hippo-paging"
                : StringUtils.EMPTY));

        add(dataTable);

        add(actionContainer = new WebMarkupContainer("actions"));

        add(ClassAttribute.append("hippo-selectable-documents"));
        add(ClassAttribute.append(() -> provider.size() == 0
                ? "hippo-empty"
                : StringUtils.EMPTY));

        final AjaxLink<Void> selectAll = new NoDoubleClickAjaxLink<Void>("select-all") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedDocuments.clear();
                // Don't add generics, the provider provides
                // Nodes or JcrNodeModels. Without additional code changes
                // a ClassCastException(RunTimeException) will occur.
                // See commit message.
                ISortableDataProvider provider = SelectableDocumentsView.this.provider;
                Iterator iter = provider.iterator(0, provider.size());
                while (iter.hasNext()) {
                    selectedDocuments.add(provider.model(iter.next()));
                }
                target.add(SelectableDocumentsView.this);
            }
        };
        actionContainer.add(selectAll);

        final AjaxLink<Void> selectNone = new NoDoubleClickAjaxLink<Void>("select-none") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedDocuments.clear();
                target.add(SelectableDocumentsView.this);
            }
        };
        actionContainer.add(selectNone);

        openButton = new BrLink<Void>("open") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                openEditor();
            }
        };
        openButton.setEnabled(editorMgr != null);
        add(openButton);
    }

    @Override
    protected void onBeforeRender() {
        boolean hasLinks = (provider.size() > 0);
        dataTable.setVisible(hasLinks);
        actionContainer.setVisible(hasLinks);
        openButton.setVisible(hasLinks);
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
