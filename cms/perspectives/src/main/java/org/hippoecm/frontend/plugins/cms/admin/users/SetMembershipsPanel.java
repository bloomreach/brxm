/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupActionLink;
import org.hippoecm.frontend.plugins.cms.admin.permissions.DomainLinkListPanel;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionBean;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.util.EventBusUtils;
import org.hippoecm.frontend.widgets.UpdateFeedbackInfo;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetMembershipsPanel extends Panel {
    private static final Logger log = LoggerFactory.getLogger(SetMembershipsPanel.class);

    private final Group selectedGroup;
    private final IModel userModel;
    private final IPluginContext context;
    private final HippoForm hippoForm;

    public SetMembershipsPanel(final String id, final IPluginContext context,
                               final IBreadCrumbModel breadCrumbModel, final IModel<User> userModel) {
        super(id);

        setOutputMarkupId(true);

        selectedGroup = null;
        this.userModel = userModel;
        this.context = context;
        final User user = userModel.getObject();

        // All local groups
        hippoForm = new HippoForm("form");

        final WebMarkupContainer localMembershipContainer = new WebMarkupContainer("localMembershipsContainer");
        localMembershipContainer.setOutputMarkupId(true);
        hippoForm.add(localMembershipContainer);

        final WebMarkupContainer externalMembershipsContainer = new WebMarkupContainer("externalMembershipsContainer");
        externalMembershipsContainer.setOutputMarkupId(true);
        hippoForm.add(externalMembershipsContainer);

        final AjaxButton submit = new AjaxButton("submit", hippoForm) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                hippoForm.clearFeedbackMessages();
                try {
                    if (selectedGroup.getMembers().contains(user.getUsername())) {
                        showInfo(getString("user-membership-already-member", new DetachableGroup(selectedGroup)), target);
                    } else {
                        selectedGroup.addMembership(user.getUsername());

                        final String msg = String.format("added user %s to group %s",
                                user.getUsername(), selectedGroup.getGroupname());
                        EventBusUtils.post("add-user-to-group", HippoEventConstants.CATEGORY_GROUP_MANAGEMENT, msg);
                        showInfo(getString("user-membership-added", new DetachableGroup(selectedGroup)), target);
                    }
                } catch (RepositoryException e) {
                    showError(getString("user-membership-add-failed", new DetachableGroup(selectedGroup)), target);
                    log.error("Failed to add memberships", e);
                }
                target.add(localMembershipContainer);
            }

            @Override
            public boolean isEnabled() {
                return selectedGroup != null;
            }
        };
        hippoForm.add(submit);

        final List<Group> localGroups = Group.getLocalGroups();
        final DropDownChoice<Group> ddc = new DropDownChoice<>("local-groups", PropertyModel.of(this, "selectedGroup"),
                localGroups, new ChoiceRenderer<>("groupname"));
        ddc.setNullValid(false);
        ddc.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(submit);
            }
        });

        hippoForm.add(ddc);
        add(hippoForm);

        // local memberships
        final Label localLabel = new Label("local-memberships-label", new ResourceModel("user-local-memberships"));
        final MembershipsListEditView localList =
                new MembershipsListEditView("local-memberships", user, localMembershipContainer);
        hippoForm.add(localLabel);
        localMembershipContainer.add(localList);

        // external memberships
        final Label externalLabel = new Label("external-memberships-label", new ResourceModel("user-external-memberships"));
        final ListView externalList = new MembershipsListView("external-memberships", "label", user);
        externalLabel.setVisible(!user.getExternalMemberships().isEmpty());
        externalList.setVisible(!user.getExternalMemberships().isEmpty());
        hippoForm.add(externalLabel);
        externalMembershipsContainer.add(externalList);

        // add a cancel/back button
        hippoForm.add(new AjaxButton("back-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                // one up
                final List<IBreadCrumbParticipant> all = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(all.get(all.size() - 2));
            }
        }.setDefaultFormProcessing(false));
    }

    private void showError(final String message, final AjaxRequestTarget target) {
        hippoForm.error(message);
        // update feedbackpanel in ViewUserPanel
        send(this, Broadcast.BUBBLE, new UpdateFeedbackInfo(target));
    }

    private void showInfo(final String message, final AjaxRequestTarget target) {
        hippoForm.info(message);
        // update feedbackpanel in ViewUserPanel
        send(this, Broadcast.BUBBLE, new UpdateFeedbackInfo(target));
    }

    /**
     * list view to be nested in the form.
     */
    private final class MembershipsListEditView extends ListView<Group> {

        private final User user;
        private final WebMarkupContainer updateTarget;

        MembershipsListEditView(final String id, final User user, final WebMarkupContainer updateTarget) {
            super(id);
            this.user = user;
            this.updateTarget = updateTarget;

            setModel(new PropertyModel<>(user, "localMembershipsAsListOfGroups"));
            setReuseItems(false);
            setOutputMarkupId(true);
            DomainDataProvider.setDirty();
        }

        @Override
        protected void populateItem(final ListItem<Group> item) {
            item.setOutputMarkupId(true);
            final Group group = item.getModelObject();

            final AdminBreadCrumbPanel viewUserPanel = findParent(ViewUserPanel.class);

            final ViewGroupActionLink groupLink = new ViewGroupActionLink(
                    "link", new PropertyModel<>(group, "groupname"),
                    group, context, viewUserPanel
            );

            item.add(groupLink);

            addDomainLinkListPanelForGroup(item, group);

            item.add(new AjaxLinkLabel("remove", new ResourceModel("user-membership-remove-action")) {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    hippoForm.clearFeedbackMessages();
                    try {
                        group.removeMembership(user.getUsername());

                        EventBusUtils.post("remove-user-from-group", HippoEventConstants.CATEGORY_GROUP_MANAGEMENT,
                                String.format("removed user %s from group %s", user.getUsername(), group.getGroupname()));
                        showInfo(getString("user-membership-removed", Model.of(group)), target);
                    } catch (final RepositoryException e) {
                        showError(getString("user-membership-remove-failed", Model.of(group)), target);
                        log.error("Failed to remove memberships", e);
                    }
                    target.add(updateTarget);
                }
            });
        }
    }

    /**
     * list view to be nested in the hippoForm.
     */
    private static final class MembershipsListView extends ListView<DetachableGroup> {
        private final String labelId;

        MembershipsListView(final String id, final String labelId, final User user) {
            super(id, new PropertyModel<List<DetachableGroup>>(user, "externalMemberships"));
            this.labelId = labelId;
            setReuseItems(true);
        }

        @Override
        protected void populateItem(final ListItem item) {
            final DetachableGroup dg = (DetachableGroup) item.getModelObject();
            final Group group = dg.getGroup();
            final Label label = new Label(labelId, group.getGroupname());
            label.setRenderBodyOnly(true);
            item.add(label);

            addDomainLinkListPanelForGroup(item, group);
        }
    }

    private static void addDomainLinkListPanelForGroup(final ListItem item, final Group group) {
        final List<PermissionBean> groupPermissions = group.getPermissions();
        final Map<Domain, List<String>> domainsWithRoles = new HashMap<>();
        for (final PermissionBean permission : groupPermissions) {
            final Domain domain = permission.getDomain().getObject();
            List<String> roles = domainsWithRoles.get(domain);
            if (roles == null) {
                roles = new ArrayList<>();
            }
            roles.add(permission.getAuthRole().getRole());
            domainsWithRoles.put(domain, roles);
        }

        final DomainLinkListPanel domainLinkList = new DomainLinkListPanel(
                "securityDomains", domainsWithRoles, item.findParent(ViewUserPanel.class)
        );

        item.add(domainLinkList);
    }

    /**
     * Return the resource id processed into a model.
     *
     * @param component The comonent.
     * @return The model containing the title extracted from the resource.
     */
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("user-set-memberships-title", component).setModel(userModel);
    }

}
