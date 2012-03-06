/*
 *  Copyright 2008 Hippo.
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.HippoSecurityEventConstants;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupActionLink;
import org.hippoecm.frontend.plugins.cms.admin.permissions.SetPermissionsPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabelContainer;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabelListPanel;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

public class SetMembershipsPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetMembershipsPanel.class);
    private Group selectedGroup;
    private final IModel model;
    private final ListView localList;
    private final IPluginContext context;
    private final IBreadCrumbModel breadCrumbModel;

    public SetMembershipsPanel(final String id, final IPluginContext context,
                               final IBreadCrumbModel breadCrumbModel, final IModel model) {
        super(id, breadCrumbModel);

        setOutputMarkupId(true);

        selectedGroup = null;
        this.model = model;
        this.context = context;
        final User user = (User) model.getObject();
        this.breadCrumbModel = breadCrumbModel;

        // All local groups
        Form form = new Form("form");

        AjaxButton submit = new AjaxButton("submit", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                try {
                    if (selectedGroup.getMembers().contains(user.getUsername())) {
                        info(getString("user-membership-already-member", new DetachableGroup(selectedGroup)));
                    } else {
                        selectedGroup.addMembership(user.getUsername());
                        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                        if (eventBus != null) {
                            final UserSession userSession = UserSession.get();
                            HippoEvent event = new HippoEvent(userSession.getApplicationName())
                                    .user(userSession.getJcrSession().getUserID())
                                    .action("add-user-to-group")
                                    .category(HippoSecurityEventConstants.CATEGORY_GROUP_MANAGEMENT)
                                    .message("added user " + user.getUsername() + " to group " + selectedGroup.getGroupname());
                            eventBus.post(event);
                        }
                        info(getString("user-membership-added", new DetachableGroup(selectedGroup)));
                        localList.removeAll();
                    }
                } catch (RepositoryException e) {
                    error(getString("user-membership-add-failed", new DetachableGroup(selectedGroup)));
                    log.error("Failed to add memberships", e);
                }
                target.addComponent(SetMembershipsPanel.this);
            }

        };
        form.add(submit);

        List<Group> localGroups = Group.getLocalGroups();
        DropDownChoice<Group> ddc = new DropDownChoice<Group>("local-groups", new PropertyModel<Group>(this, "selectedGroup"),
                localGroups, new ChoiceRenderer<Group>("groupname"));
        ddc.setNullValid(false);
        ddc.setRequired(true);

        form.add(ddc);
        add(form);

        // local memberships
        Label localLabel = new Label("local-memberships-label", new ResourceModel("user-local-memberships"));
        localList = new MembershipsListEditView("local-memberships", "local-membership", user);
        form.add(localLabel);
        form.add(localList);

        // external memberships
        Label externalLabel = new Label("external-memberships-label", new ResourceModel("user-external-memberships"));
        ListView externalList = new MembershipsListView("external-memberships", "external-membership", user);
        externalLabel.setVisible((user.getExternalMemberships().size() > 0));
        externalList.setVisible((user.getExternalMemberships().size() > 0));
        form.add(externalLabel);
        form.add(externalList);


        // add a cancel/back button
        form.add(new AjaxButton("back-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                // one up
                List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(l.get(l.size() - 2));
            }
        }.setDefaultFormProcessing(false));
    }

    /**
     * list view to be nested in the form.
     */
    private final class MembershipsListEditView extends ListView<DetachableGroup> {
        private static final long serialVersionUID = 1L;
        private String labelId;
        public MembershipsListEditView(final String id, final String labelId, final User user) {
            super(id, new PropertyModel<List<DetachableGroup>>(user, "localMemberships"));
            this.labelId = labelId;
            this.user = user;
            setReuseItems(false);
            setOutputMarkupId(true);
        }

        private User user;

        protected void populateItem(final ListItem<DetachableGroup> item) {
            /* Set contains the list of Security Domains linked to the group.*/
            ArrayList<AjaxLinkLabelContainer> securityDomains = new ArrayList<AjaxLinkLabelContainer>();
            /* Set contains the list of names of roles linked to the group.*/
            Set<String> roles = new HashSet<String>();

            item.setOutputMarkupId(true);
            final DetachableGroup model = item.getModelObject();
            Group group = model.getGroup();
            final IModel<Group> groupModel = new Model<Group>(group);

            AdminBreadCrumbPanel viewUserPanel = this.findParent(ViewUserPanel.class);

            ViewGroupActionLink groupLink = new ViewGroupActionLink(labelId, new PropertyModel(groupModel, "groupname"),
                    groupModel, context, viewUserPanel);

            item.add(groupLink.getAjaxFallbackLink());

            try {
                // Determine the role and security domains via the domain provider.
                DomainDataProvider domainDataProvider = new DomainDataProvider();
                Iterator<Domain> domainIterator = domainDataProvider.iterator(0, domainDataProvider.size());
                while (domainIterator.hasNext()) {
                    Domain domain = domainIterator.next();
                    SortedMap<String, Domain.AuthRole> authRoles = domain.getAuthRoles();

                    List<String> keys = new ArrayList<String>(authRoles.keySet());
                    for (String role : keys) {
                        String roleName = (authRoles.get(role)).getRole();
                        List groupNamesList = new ArrayList<String>((authRoles.get(role)).getGroupnames());
                        if ((groupNamesList.contains(model.getGroup().getGroupname()))
                                && (model.getGroup().getMembers().contains(user.getUsername()))) {
                            securityDomains.add(createSecurityDomainLinkLabel(domain.getName(), labelId, domain));
                            roles.add(roleName);
                        }
                    }
                }
            } catch (RepositoryException e) {
                error(getString("get-group-members-failed", model));
                log.error("Failed to get group member", e);
            }

            item.add(new AjaxLinkLabelListPanel("securityDomain",
                    new Model<ArrayList<AjaxLinkLabelContainer>>(securityDomains)));
            item.add(new Label("role", createDelimitedString(roles)));
            item.add(new AjaxLinkLabel("remove", new ResourceModel("user-membership-remove-action")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        model.getGroup().removeMembership(user.getUsername());
                        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                        if (eventBus != null) {
                            final UserSession userSession = UserSession.get();
                            HippoEvent event = new HippoEvent(userSession.getApplicationName())
                                    .user(userSession.getJcrSession().getUserID())
                                    .action("remove-user-from-group")
                                    .category(HippoSecurityEventConstants.CATEGORY_GROUP_MANAGEMENT)
                                    .message(
                                            "removed user " + user.getUsername()
                                                    + " from group " + model.getGroup().getGroupname());
                            eventBus.post(event);
                        }
                        info(getString("user-membership-removed", model));
                        localList.removeAll();
                    } catch (RepositoryException e) {
                        error(getString("user-membership-remove-failed", model));
                        log.error("Failed to remove memberships", e);
                    }
                    target.addComponent(SetMembershipsPanel.this);
                }
            }

            );
        }
    }

    /**
     * @param securityDomainName The name of the security domain which must be processed.
     * @param labelId The wicket Id of the label.
     * @param domain The domain which is linked to the link.
     * @return the security domain link processed into a AjaxLinkLabelContainer
     */
    private AjaxLinkLabelContainer createSecurityDomainLinkLabel(final String securityDomainName,
                                                                 final String labelId, final Domain domain) {
        return new AjaxLinkLabelContainer(labelId, new Model<String>(securityDomainName)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                /**
                 * Get the originating parent to be able to do a replace of the panel.
                 */
                AdminBreadCrumbPanel viewUserPanel =
                        (this.getAjaxFallbackLink()).findParent(ViewUserPanel.class);
                AdminBreadCrumbPanel setPermissionsPanel =
                        new SetPermissionsPanel(viewUserPanel.getId(), breadCrumbModel, new Model<Domain>(domain));
                viewUserPanel.replaceWith(setPermissionsPanel);
                // Reset the reference
                viewUserPanel = setPermissionsPanel;
                target.addComponent(viewUserPanel);
                activate(setPermissionsPanel);
            }
        };
    }

    /**
     * @param listToConvert The list which much be converted in to a comma separated string.
     * @return The string.
     */
    private String createDelimitedString(final Set<String> listToConvert) {
        StringBuilder stringBuffer = new StringBuilder();
        for (String listItem : listToConvert) {
            if (stringBuffer.length() > 0) {
                stringBuffer.append(", ");
            }
            stringBuffer.append(listItem);
        }
        return stringBuffer.toString();
    }

    /**
     * list view to be nested in the form.
     */
    private final class MembershipsListView extends ListView<DetachableGroup> {
        private static final long serialVersionUID = 1L;
        private String labelId;

        public MembershipsListView(final String id, final String labelId, final User user) {
            super(id, new PropertyModel<List<DetachableGroup>>(user, "externalMemberships"));
            this.labelId = labelId;
            setReuseItems(true);
        }

        protected void populateItem(final ListItem item) {
            final DetachableGroup dg = (DetachableGroup) item.getModelObject();
            item.add(new Label(labelId, dg.getGroup().getGroupname()));
        }

    }

    /**
     * Return the resource id processed into a model.
     * @param component The comonent.
     * @return The model containing the title extracted from the resource.
     */
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("user-set-memberships-title", component, model);
    }

}
