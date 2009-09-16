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

import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
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
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.StateIconAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel that displays the revision history of a document as a list.
 */
public class RevisionHistoryView extends Panel implements IPagingDefinition {
    private static final long serialVersionUID = -6072417388871990194L;

    static final Logger log = LoggerFactory.getLogger(RevisionHistoryView.class);

    private RevisionHistory history;
    private ISortableDataProvider provider;
    private ListDataTable dataTable;

    public RevisionHistoryView(String id, RevisionHistory history) {
        super(id);

        this.history = history;

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
                onSelect(model);
            }

        }, true, this);
        add(dataTable);

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

        add(new CssClassAppender(new Model("hippo-history-documents")));
    }

    protected List<Revision> getRevisions() {
        return history.getRevisions();
    }

    public void onSelect(IModel/*<Revision>*/model) {
    }

    @Override
    protected void detachModel() {
        history.detach();
        super.detachModel();
    };

    protected TableDefinition getTableDefinition() {
        List<ListColumn> columns = new ArrayList<ListColumn>();

        ListColumn column = new ListColumn(new StringResourceModel("history-name", this, null), null);
        column.setRenderer(new IListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, IModel model) {
                Revision revision = (Revision) model.getObject();
                Node node = revision.getRevisionNodeModel().getNode();
                IModel nameModel;
                try {
                    nameModel = new NodeTranslator(new JcrNodeModel(node.getNode("jcr:frozenNode"))).getNodeName();
                } catch (PathNotFoundException e) {
                    nameModel = new Model("Missing node " + e.getMessage());
                    log.error(e.getMessage(), e);
                } catch (RepositoryException e) {
                    nameModel = new Model("Error " + e.getMessage());
                    log.error(e.getMessage(), e);
                }
                return new Label(id, nameModel);
            }

        });
        column.setAttributeModifier(new DocumentAttributeModifier());
        columns.add(column);

        column = new ListColumn(new StringResourceModel("history-time", this, null), null);
        column.setRenderer(new IListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getRenderer(String id, final IModel model) {
                IModel labelModel = new IModel() {

                    public Object getObject() {
                        Format format = new MessageFormat("{0,date} {0,time}", getLocale());
                        return format.format(new Object[] { ((Revision) model.getObject()).getCreationDate() });
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

        });
        columns.add(column);

        column = new ListColumn(new StringResourceModel("history-state", this, null), "state");
        column.setRenderer(new EmptyRenderer());
        column.setAttributeModifier(new IListAttributeModifier() {
            private static final long serialVersionUID = 1L;

            public AttributeModifier[] getCellAttributeModifiers(IModel model) {
                Revision revision = (Revision) model.getObject();
                StateIconAttributes attrs = new StateIconAttributes(revision.getRevisionNodeModel());
                AttributeModifier[] attributes = new AttributeModifier[2];
                attributes[0] = new CssClassAppender(new PropertyModel(attrs, "cssClass"));
                attributes[1] = new AttributeAppender("title", new PropertyModel(attrs, "summary"), " ");
                return attributes;
            }

            public AttributeModifier[] getColumnAttributeModifiers(IModel model) {
                return new AttributeModifier[] { new CssClassAppender(new Model("icon-16")) };
            }
        });
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
