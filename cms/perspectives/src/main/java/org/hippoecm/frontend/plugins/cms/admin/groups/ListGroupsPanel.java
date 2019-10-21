/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.dialog.Confirm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.SearchTermPanel;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel displays a pageable, searchable list of groups.
 */
public class ListGroupsPanel extends AdminBreadCrumbPanel implements IObserver<GroupDataProvider> {

    private static final Logger log = LoggerFactory.getLogger(ListGroupsPanel.class);

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    private AdminDataTable table;
    private IPluginContext context;
    private final GroupDataProvider groupDataProvider;
    private final boolean isSecurityUserManager;

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

        final HippoSession session = UserSession.get().getJcrSession();
        isSecurityUserManager = session.isUserInRole(SecurityConstants.USERROLE_SECURITY_USER_MANAGER);

        this.context = context;
        this.groupDataProvider = groupDataProvider;

        final Component createGroupLink = new PanelPluginBreadCrumbLink("create-group", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new CreateGroupPanel(componentId, breadCrumbModel);
            }
        };
        createGroupLink.setVisible(isSecurityUserManager);
        add(createGroupLink);

        final List<IColumn<Group, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<Group, String>(new ResourceModel("group-name"), "groupname") {
            @Override
            public void populateItem(final Item<ICellPopulator<Group>> cellItem, final String componentId,
                                     final IModel<Group> rowModel) {

                cellItem.add(new AjaxGroupViewActionLinkLabel(componentId, rowModel.getObject()));
            }
        });

        columns.add(new PropertyColumn<>(new ResourceModel("group-description"), "description"));
        columns.add(new GroupDeleteLinkColumn(new ResourceModel("group-view-actions-title")));

        final SearchTermPanel searchTermPanel = new SearchTermPanel("search-field") {
            @Override
            public void processSubmit(final AjaxRequestTarget target, final String searchTerm) {
                super.processSubmit(target, searchTerm);
                groupDataProvider.setSearchTerm(searchTerm);
                target.add(table);
            }
        };
        add(searchTermPanel);

        table = new AdminDataTable<>("table", columns, groupDataProvider, NUMBER_OF_ITEMS_PER_PAGE);
        add(table);
    }

    @Override
    protected void onAddedToBreadCrumbsBar() {
        context.registerService(this, IObserver.class.getName());
    }

    @Override
    protected void onRemovedFromBreadCrumbsBar() {
        groupDataProvider.setSearchTerm(null);
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

        private AjaxGroupViewActionLinkLabel(final String id, final Group group) {
            super(id);

            final ViewGroupActionLink link = new ViewGroupActionLink("link", Model.of(group.getGroupname()), group,
                                                               context, ListGroupsPanel.this);
            add(link);
        }
    }

    @Override
    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-groups-title");
    }

    private final class GroupDeleteLinkColumn extends AbstractColumn<Group, String> {

        private GroupDeleteLinkColumn(final IModel<String> displayModel) {
            super(displayModel);
        }

        @Override
        public void populateItem(final Item<ICellPopulator<Group>> cellItem, final String componentId,
                                 final IModel<Group> rowModel) {
            Group group = rowModel.getObject();
            if (isSecurityUserManager && !group.isSystem() && !group.isExternal()) {
                cellItem.add(new DeleteGroupActionLink(componentId, new ResourceModel("group-remove-action"), rowModel));
            } else {
                cellItem.add(new Label(componentId, ""));
            }
        }
    }

    private class DeleteGroupActionLink extends AjaxLinkLabel {
        private final IModel<Group> groupModel;

        private DeleteGroupActionLink(final String id, final IModel<String> model, final IModel<Group> groupModel) {
            super(id, model);
            this.groupModel = groupModel;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
            final Confirm confirm = new Confirm(
                    getString("group-delete-title", groupModel),
                    getString("group-delete-text", groupModel)
            ).ok(() -> deleteGroup(groupModel));

            dialogService.show(confirm);
        }

        private void deleteGroup(final IModel<Group> model) {
            final Group group = model.getObject();
            final String groupname = group.getGroupname();
            try {
                group.delete();
                ListGroupsPanel.this.info(getString("group-removed", model));
            } catch (RepositoryException e) {
                ListGroupsPanel.this.error(getString("group-remove-failed", model));
                log.error("Unable to delete group '{}' : ", groupname, e);
            }

            redraw();
        }
    }
}
