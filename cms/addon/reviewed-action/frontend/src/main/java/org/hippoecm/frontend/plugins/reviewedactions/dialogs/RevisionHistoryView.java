/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.reviewedactions.model.Revision;
import org.hippoecm.frontend.plugins.reviewedactions.model.RevisionHistory;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel that displays the revision history of a document as a list.  Revisions
 * can be selected and opened.
 */
public class RevisionHistoryView extends Panel implements IPagingDefinition {
    private static final long serialVersionUID = -6072417388871990194L;

    static final Logger log = LoggerFactory.getLogger(RevisionHistoryView.class);

    private RevisionHistory history;
    private ISortableDataProvider provider;
    private IEditorManager editorMgr;
    private ListDataTable dataTable;
    private WebMarkupContainer actionContainer;
    private List<IModel/*<Revision>*/> selectedRevisions = new LinkedList<IModel/*<Revision>*/>();

    public RevisionHistoryView(String id, RevisionHistory history, IEditorManager mgr) {
        super(id);

        this.history = history;
        this.editorMgr = mgr;

        final SortState state = new SortState();
        this.provider = new ISortableDataProvider() {
            private static final long serialVersionUID = 1L;

            public void setSortState(ISortState arg0) {
                throw new UnsupportedOperationException();
            }

            public ISortState getSortState() {
                return state;
            }

            public void detach() {
                // TODO Auto-generated method stub
            }

            public int size() {
                return getRevisions().size();
            }

            public IModel model(Object object) {
                return new Model((Revision) object);
            }

            public Iterator<Revision> iterator(int first, int count) {
                return getRevisions().subList(first, first + count).iterator();
            }
        };

        dataTable = new ListDataTable("datatable", getTableDefinition(), provider, new TableSelectionListener() {
            private static final long serialVersionUID = 1L;

            public void selectionChanged(IModel model) {
            }

        }, true, this);
        add(dataTable);

        add(actionContainer = new WebMarkupContainer("actions"));

        add(new CssClassAppender(new IModel() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                if (getRevisions().size() == 0) {
                    return "hippo-empty";
                }
                return "";
            }

            public void setObject(Object object) {
                // TODO Auto-generated method stub

            }

            public void detach() {
                // TODO Auto-generated method stub

            }

        }));

        AjaxLink selectAll = new AjaxLink("select-all") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedRevisions.clear();
                Iterator<?> iter = provider.iterator(0, provider.size());
                while (iter.hasNext()) {
                    selectedRevisions.add(provider.model(iter.next()));
                }
                target.addComponent(RevisionHistoryView.this);
            }
        };
        actionContainer.add(selectAll);

        AjaxLink selectNone = new AjaxLink("select-none") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                selectedRevisions.clear();
                target.addComponent(RevisionHistoryView.this);
            }
        };
        actionContainer.add(selectNone);

        Button open = new AjaxButton("open") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (editorMgr != null) {
                    for (IModel/*<Revision>*/model : selectedRevisions) {
                        Revision revision = (Revision) model.getObject();
                        JcrNodeModel versionModel = revision.getRevisionNodeModel();
                        // get nt:version node
                        versionModel = versionModel.getParentModel();
                        IEditor editor = editorMgr.getEditor(versionModel);
                        if (editor == null) {
                            try {
                                editorMgr.openPreview(versionModel);
                            } catch (ServiceException ex) {
                                log.error("Could not open editor for " + versionModel.getItemModel().getPath(), ex);
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

        add(new CssClassAppender(new Model("hippo-history-documents")));
    }

    protected List<Revision> getRevisions() {
        return history.getRevisions();
    }

    public void onSelect(IModel/*<Revision>*/revision) {
    }

    @Override
    protected void detachModel() {
        history.detach();
        super.detachModel();
    };

    @Override
    protected void onBeforeRender() {
        boolean hasLinks = (getRevisions().size() > 0);
        dataTable.setVisible(hasLinks);
        actionContainer.setVisible(hasLinks);
        super.onBeforeRender();
    }

    protected void onOpen() {
    }

    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new Model(""), null);
        column.setRenderer(new RowSelector(selectedRevisions));
        columns.add(column);

        column = new ListColumn(new StringResourceModel("history-name", this, null), null);
        column.setRenderer(new IListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, IModel model) {
                return new Label(id, new PropertyModel(model, "creationDate"));
            }

        });
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
