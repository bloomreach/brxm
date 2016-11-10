/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DefaultFocusBehavior;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DeleteDialog;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel displays a pageable, searchable list of groups.
 */
public class ListGroupsPanel extends AdminBreadCrumbPanel implements IObserver<GroupDataProvider> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListGroupsPanel.class);

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    private AdminDataTable table;
    private IPluginContext context;
    private final GroupDataProvider groupDataProvider;

    /**
     * Constructs a new ListGroupsPanel.
     *
     * @param id                the id
     * @param context           the context
     * @param breadCrumbModel   the breadCrumbModel
     * @param groupDataProvider the groupDataProvider
     */
    public ListGroupsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel, final GroupDataProvider groupDataProvider) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.context = context;

        add(new PanelPluginBreadCrumbLink("create-group", breadCrumbModel) {

            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new CreateGroupPanel(componentId, breadCrumbModel);
            }
        });

        List<IColumn<Group, String>> columns = new ArrayList<IColumn<Group, String>>();

        columns.add(new AbstractColumn<Group, String>(new ResourceModel("group-name"), "groupname") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item<ICellPopulator<Group>> item, final String componentId, final IModel<Group> model) {

                AjaxGroupViewActionLinkLabel action = new AjaxGroupViewActionLinkLabel(componentId, model.getObject());
                item.add(action);
            }
        });

        columns.add(new PropertyColumn<Group, String>(new ResourceModel("group-description"), "description"));

        columns.add(new GroupDeleteLinkColumn(new ResourceModel("group-view-actions-title")));

        final Form form = new Form("search-form");
        form.setOutputMarkupId(true);
        add(form);

        this.groupDataProvider = groupDataProvider;
        TextField<String> search = new TextField<String>("search-query",
                                                         new PropertyModel<String>(this.groupDataProvider, "searchTerm"));
        search.add(StringValidator.minimumLength(1));
        search.setRequired(false);
        search.add(new DefaultFocusBehavior());
        form.add(search);

        form.add(new AjaxButton("search-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                target.add(table);
            }
        });

        table = new AdminDataTable<Group>("table", columns, groupDataProvider, NUMBER_OF_ITEMS_PER_PAGE);
        add(table);
    }

    @Override
    protected void onAddedToBreadCrumbsBar() {
        context.registerService(this, IObserver.class.getName());
    }

    @Override
    protected void onRemovedFromBreadCrumbsBar() {
        context.unregisterService(this, IObserver.class.getName());
    }

    @Override
    public GroupDataProvider getObservable() {
        return groupDataProvider;
    }

    @Override
    public void onEvent(final Iterator<? extends IEvent<GroupDataProvider>> events) {
        redraw();
    }

    private class AjaxGroupViewActionLinkLabel extends Panel {

        private AjaxGroupViewActionLinkLabel(final String id, Group group) {
            super(id);

            ViewGroupActionLink link = new ViewGroupActionLink("link", new Model<String>(group.getGroupname()), group,
                                                               context, ListGroupsPanel.this);

            add(link);
        }
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-groups-title");
    }

    private class GroupDeleteLinkColumn extends AbstractColumn<Group, String> {
        private static final long serialVersionUID = 1L;

        private GroupDeleteLinkColumn(final IModel<String> displayModel) {
            super(displayModel);
        }

        @Override
        public void populateItem(final Item<ICellPopulator<Group>> item, final String componentId, final IModel<Group> model) {

            AjaxLinkLabel action = new DeleteGroupActionLink(componentId, new ResourceModel("group-remove-action"),
                                                             model);
            item.add(action);
        }


    }

    private class DeleteGroupActionLink extends AjaxLinkLabel {
        private static final long serialVersionUID = 1L;
        private final IModel<Group> groupModel;

        private DeleteGroupActionLink(String id, IModel model, IModel<Group> groupModel) {
            super(id, model);
            this.groupModel = groupModel;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            context.getService(IDialogService.class.getName(), IDialogService.class).show(
                    new DeleteDialog<Group>(groupModel, this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onOk() {
                            deleteGroup(getModel());
                        }

                        @Override
                        protected String getTitleKey() {
                            return "group-delete-title";
                        }

                        @Override
                        protected String getTextKey() {
                            return "group-delete-text";
                        }
                    });
        }

        private void deleteGroup(final IModel<Group> model) {
            Group group = model.getObject();
            String groupname = group.getGroupname();
            try {
                group.delete();

                // retrieve info message before activating a new ListGroupPanel, as by then a message with key
                // 'group-removed' is no longer found
                final String infoMsg = getString("group-removed", model);
                activateParent();
                activate(new IBreadCrumbPanelFactory() {
                    public BreadCrumbPanel create(final String componentId, final IBreadCrumbModel breadCrumbModel) {
                        final ListGroupsPanel groupsPanel = new ListGroupsPanel(componentId, context, breadCrumbModel, new GroupDataProvider());
                        groupsPanel.info(infoMsg);
                        return groupsPanel;
                    }
                });
            } catch (RepositoryException e) {
                ListGroupsPanel.this.error(getString("group-remove-failed", model));
                AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class);
                if (target !=  null) {
                    target.add(ListGroupsPanel.this);
                }
                log.error("Unable to delete group '" + groupname + "' : ", e);
            }
        }
    }
}
