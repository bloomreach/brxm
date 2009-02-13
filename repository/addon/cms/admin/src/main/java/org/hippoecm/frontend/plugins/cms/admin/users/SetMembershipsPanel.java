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

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetMembershipsPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetMembershipsPanel.class);

    private Group selectedGroup;
    private final IModel model;
    private final ListView localList;
    private final ListView externalList;

    public SetMembershipsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
            final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        this.model = model;
        final User user = (User) model.getObject();

        // All local groups
        Form form = new Form("form");

        AjaxButton submit = new AjaxButton("submit", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    selectedGroup.addMembership(user.getUsername());
                    info(getString("user-membership-added", new DetachableGroup(selectedGroup)));
                    localList.removeAll();
                    target.addComponent(SetMembershipsPanel.this);
                } catch (RepositoryException e) {
                    error(getString("user-membership-add-failed", new DetachableGroup(selectedGroup)));
                    log.error("Failed to add memberships", e);
                }
            }

        };
        form.add(submit);

        List<Group> localGroups = Group.getLocalGroups();
        DropDownChoice ddc = new DropDownChoice("local-groups", new PropertyModel(this, "selectedGroup"), localGroups,
                new ChoiceRenderer("groupname"));
        ddc.setNullValid(false);
        ddc.setRequired(true);

        form.add(ddc);
        add(form);

        // local memberships
        Label localLabel = new Label("local-memberships-label", new ResourceModel("user-local-memberships"));
        localList = new MembershipsListEditView("local-memberships", "local-membership", user);
        add(localLabel);
        add(localList);

        // external memberships
        Label externalLabel = new Label("external-memberships-label", new ResourceModel("user-external-memberships"));
        externalList = new MembershipsListView("external-memberships", "external-membership", user);
        externalLabel.setVisible((user.getExternalMemberships().size() > 0));
        externalList.setVisible((user.getExternalMemberships().size() > 0));
        add(externalLabel);
        add(externalList);
    }

    /** list view to be nested in the form. */
    private final class MembershipsListEditView extends ListView {
        private static final long serialVersionUID = 1L;
        private String labelId;
        private User user;

        public MembershipsListEditView(final String id, final String labelId, final User user) {
            super(id, new PropertyModel(user, "localMemberships"));
            this.labelId = labelId;
            this.user = user;
            setReuseItems(false);
            setOutputMarkupId(true);
        }

        protected void populateItem(ListItem item) {
            item.setOutputMarkupId(true);
            final DetachableGroup model = (DetachableGroup) item.getModelObject();
            item.add(new Label(labelId, model.getGroup().getGroupname()));
            item.add(new AjaxLinkLabel("remove", new ResourceModel("user-membership-remove-action")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        model.getGroup().removeMembership(user.getUsername());
                        info(getString("user-membership-removed", model));
                        localList.removeAll();
                        target.addComponent(SetMembershipsPanel.this);
                    } catch (RepositoryException e) {
                        error(getString("user-membership-remove-failed", model));
                        log.error("Failed to remove memberships", e);
                    }
                }
            });
        }
    }

    /** list view to be nested in the form. */
    private final class MembershipsListView extends ListView {
        private static final long serialVersionUID = 1L;
        private String labelId;

        public MembershipsListView(final String id, final String labelId, final User user) {
            super(id, new PropertyModel(user, "externalMemberships"));
            this.labelId = labelId;
            setReuseItems(true);
        }

        protected void populateItem(ListItem item) {
            final DetachableGroup dg = (DetachableGroup) item.getModelObject();
            item.add(new Label(labelId, dg.getGroup().getGroupname()));
        }
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("user-set-memberhips-title", component, model);
    }

}
