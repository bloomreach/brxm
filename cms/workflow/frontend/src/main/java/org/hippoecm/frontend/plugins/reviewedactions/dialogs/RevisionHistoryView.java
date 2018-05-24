/*
 *  Copyright 2009-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.StateIconAttributes;
import org.hippoecm.frontend.plugins.reviewedactions.model.Revision;
import org.hippoecm.frontend.plugins.reviewedactions.model.RevisionHistory;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.TableDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.IPagingDefinition;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable;
import org.hippoecm.frontend.plugins.standards.list.datatable.ListDataTable.TableSelectionListener;
import org.hippoecm.frontend.plugins.standards.list.datatable.SortState;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractListAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListCellRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;

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
            @Override
            public ISortState<String> getSortState() {
                return state;
            }

            @Override
            public void detach() {
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
            @Override
            public void selectionChanged(IModel<Revision> model) {
                onSelect(model);
            }
        }, true, this);
        add(dataTable);

        add(CssClass.append(new LoadableDetachableModel<String>() {
            protected String load() {
                return getRevisions().isEmpty() ? "hippo-empty" : "";
            }
        }));

        add(CssClass.append("hippo-history-documents"));
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

        return new TableDefinition<>(Arrays.asList(
                getTimeColumn(),
                getUserColumn(),
                getStateColumn(),
                getLabelsColumn(),
                getNameColumn()));
    }

    /**
     * Returns a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the time information
     */
    private ListColumn<Revision> getTimeColumn() {
        return getColumn("history-time", "null",
                revision -> {
                    final Date creationDate = revision.getCreationDate();
                    return DateTimePrinter.of(creationDate).print(FormatStyle.LONG, FormatStyle.MEDIUM);
                });
    }

    /**
     * Returns a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the information of the user
     */
    private ListColumn<Revision> getUserColumn() {
        return getColumn("history-user", "user",
                revision -> new StateIconAttributes((JcrNodeModel) revision.getDocument()).getLastModifiedBy());
    }

    /**
     * Returns a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the state information.
     */
    private ListColumn<Revision> getStateColumn() {
        ListColumn<Revision> column = new ListColumn<>(Model.of(getString("history-state")), "state");
        column.setRenderer(EmptyRenderer.getInstance());
        column.setAttributeModifier(new AbstractListAttributeModifier<Revision>() {
            @Override
            public AttributeModifier[] getCellAttributeModifiers(IModel<Revision> model) {
                Revision revision = model.getObject();
                StateIconAttributes attrs = new StateIconAttributes((JcrNodeModel) revision.getDocument());
                AttributeModifier[] attributes = new AttributeModifier[2];
                attributes[0] = CssClass.append(new PropertyModel<>(attrs, "cssClass"));
                attributes[1] = TitleAttribute.append(new PropertyModel<>(attrs, "summary"));
                return attributes;
            }

            @Override
            public AttributeModifier[] getColumnAttributeModifiers() {
                return new AttributeModifier[] { CssClass.append("icon-16") };
            }
        });
        return column;
    }

    /**
     * Returns a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the information of the JCR labels.
     */
    private ListColumn<Revision> getLabelsColumn() {
        return getColumn("history-label", "label",
                revision -> revision.getLabels().stream().collect(Collectors.joining(",")));
    }

    /**
     * Returns a {@link org.hippoecm.frontend.plugins.standards.list.ListColumn} containing the information in hippo:branchName
     * if it exists
     */
    private ListColumn<Revision> getNameColumn() {
        return getColumn("history-name", "name", revision -> {
                    final Node versionHistoryNode = revision.getVersionModel().getNode();
                    try {
                        if (versionHistoryNode != null && versionHistoryNode instanceof Version) {
                            final Node frozenNode = ((Version) versionHistoryNode).getFrozenNode();
                            if (frozenNode != null && frozenNode.hasProperty(HIPPO_PROPERTY_BRANCH_NAME)) {
                                return frozenNode.getProperty(HIPPO_PROPERTY_BRANCH_NAME).getString();
                            }
                        }
                    } catch (RepositoryException e) {
                        log.warn("Exception while trying to access versioned node '{}'", JcrUtils.getNodePathQuietly(versionHistoryNode));
                    }
                    return "";
                }
        );
    }

    private ListColumn<Revision> getColumn(final String key, final String sortProperty, final Function<Revision, Object> getRevisionProperty) {
        ListColumn<Revision> column = new ListColumn<>(Model.of(getString(key)), sortProperty);
        column.setRenderer(createRenderer(getRevisionProperty));
        return column;
    }

    private IListCellRenderer<Revision> createRenderer(Function<Revision, Object> getRevisionProperty) {
        return new IListCellRenderer<Revision>() {
            @Override
            public Component getRenderer(String id, final IModel<Revision> model) {
                IModel labelModel = new IModel() {
                    @Override
                    public Object getObject() {
                        return getRevisionProperty.apply(model.getObject());
                    }

                    @Override
                    public void setObject(Object object) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void detach() {
                        model.detach();
                    }
                };
                return new Label(id, labelModel);
            }

            @Override
            public IObservable getObservable(IModel<Revision> model) {
                return null;
            }
        };
    }


    public int getPageSize() {
        return 7;
    }

    public int getViewSize() {
        return 5;
    }

}
