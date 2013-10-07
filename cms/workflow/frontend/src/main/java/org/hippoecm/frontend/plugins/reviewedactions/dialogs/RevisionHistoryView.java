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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.StateIconAttributes;
import org.hippoecm.frontend.plugins.reviewedactions.model.Revision;
import org.hippoecm.frontend.plugins.reviewedactions.model.RevisionHistory;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel that displays the revision history of a document as a list.
 */
public class RevisionHistoryView extends Panel implements IPagingDefinition {

    private static final long serialVersionUID = -6072417388871990194L;

    static final Logger log = LoggerFactory.getLogger(RevisionHistoryView.class);

    private RevisionHistory history;

    public RevisionHistoryView(String id, RevisionHistory history) {
        super(id);

        this.history = history;

        final SortState state = new SortState();
        final ISortableDataProvider<Revision, String> provider = new ISortableDataProvider<Revision, String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public ISortState<String> getSortState() {
                return state;
            }

            @Override
            public void detach() {
                // TODO Auto-generated method stub
            }

            @Override
            public long size() {
                return getRevisions().size();
            }

            @Override
            public IModel<Revision> model(Revision revision) {
                return Model.of(revision);
            }

            @Override
            public Iterator<Revision> iterator(long first, long count) {
                return getRevisions().subList((int) first, (int) (first + count)).iterator();
            }
        };

        final ListDataTable<Revision> dataTable = new ListDataTable<>("datatable", getTableDefinition(), provider, new TableSelectionListener<Revision>() {
            private static final long serialVersionUID = 1L;

            public void selectionChanged(IModel<Revision> model) {
                onSelect(model);
            }

        }, true, this);
        add(dataTable);

        add(new CssClassAppender(new LoadableDetachableModel<String>() {
            private static final long serialVersionUID = 1L;

            protected String load() {
                if (getRevisions().size() == 0) {
                    return "hippo-empty";
                }
                return "";
            }

        }));

        add(new CssClassAppender(new Model<>("hippo-history-documents")));
    }

    protected List<Revision> getRevisions() {
        return history.getRevisions();
    }

    public void onSelect(IModel<Revision> model) {
    }

    @Override
    protected void detachModel() {
        history.detach();
        super.detachModel();
    }

    /**
     * Gets a {@link org.hippoecm.frontend.plugins.standards.list.TableDefinition} whichs contains all columns and
     * information needed for the view.
     * @return the {@link org.hippoecm.frontend.plugins.standards.list.TableDefinition} with all data
     */
    protected TableDefinition<Revision> getTableDefinition() {
        List<ListColumn<Revision>> columns = new ArrayList<>();

        addTimeColumn(columns);
        addUserColumn(columns);
        addStateColumn(columns);

        return new TableDefinition<>(columns);
    }

    /**
     * Adds a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the time information
     * to the list of columns.
     * @param columns the list of columns.
     */
    private void addTimeColumn(List<ListColumn<Revision>> columns) {
        ListColumn<Revision> column = new ListColumn<>(new StringResourceModel("history-time", this, null), null);
        column.setRenderer(new IListCellRenderer<Revision>() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, final IModel<Revision> model) {
                IModel labelModel = new IModel() {

                    public Object getObject() {
                        Format format = new MessageFormat("{0,date} {0,time}", getLocale());
                        return format.format(new Object[] { model.getObject().getCreationDate() });
                    }

                    public void setObject(Object object) {
                        throw new UnsupportedOperationException();
                    }

                    public void detach() {
                        model.detach();
                    }
                };
                return new Label(id, labelModel);
            }

            public IObservable getObservable(IModel<Revision> model) {
                return null;
            }

        });
        columns.add(column);
    }

    /**
     * Adds a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the information of the user
     * to the list of columns.
     * @param columns the list of columns.
     */
    private void addUserColumn(List<ListColumn<Revision>> columns) {
        ListColumn<Revision> column = new ListColumn<>(new StringResourceModel("history-user", this, null), "user");
        column.setRenderer(new IListCellRenderer<Revision>() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, final IModel<Revision> model) {
                IModel labelModel = new IModel() {

                    public Object getObject() {
                        Revision revision = model.getObject();
                        StateIconAttributes attrs = new StateIconAttributes((JcrNodeModel) revision.getDocument());
                        return attrs.getLastModifiedBy();
                    }

                    public void setObject(Object object) {
                        throw new UnsupportedOperationException();
                    }

                    public void detach() {
                        model.detach();
                    }
                };
                return new Label(id, labelModel);
            }

            public IObservable getObservable(IModel<Revision> model) {
                return null;
            }

        });
        columns.add(column);
    }
    
    /**
     * Adds a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the state information to the list of columns.
     * @param columns the list of columns.
     */
    private void addStateColumn(List<ListColumn<Revision>> columns) {
        ListColumn<Revision> column = new ListColumn<>(new StringResourceModel("history-state", this, null), "state");
        column.setRenderer(new EmptyRenderer<Revision>());
        column.setAttributeModifier(new AbstractListAttributeModifier<Revision>() {
            private static final long serialVersionUID = 1L;

            @Override
            public AttributeModifier[] getCellAttributeModifiers(IModel<Revision> model) {
                Revision revision = model.getObject();
                StateIconAttributes attrs = new StateIconAttributes((JcrNodeModel) revision.getDocument());
                AttributeModifier[] attributes = new AttributeModifier[2];
                attributes[0] = new CssClassAppender(new PropertyModel<String>(attrs, "cssClass"));
                attributes[1] = new AttributeAppender("title", new PropertyModel(attrs, "summary"), " ");
                return attributes;
            }

            @Override
            public AttributeModifier[] getColumnAttributeModifiers() {
                return new AttributeModifier[] { new CssClassAppender(new Model<>("icon-16")) };
            }
        });
        columns.add(column);
    }

    public int getPageSize() {
        return 7;
    }

    public int getViewSize() {
        return 5;
    }

}
