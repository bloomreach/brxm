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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditGroupPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(EditGroupPanel.class);

    private final IModel<Group> model;

    public EditGroupPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel<Group> model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.model = model;

        // add form with markup id setter so it can be updated via ajax
        final Form form = new Form<>("form", CompoundPropertyModel.of(model));
        form.setOutputMarkupId(true);
        add(form);

        form.add(new TextField("description"));

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("save-button", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                final Group group = model.getObject();
                final String groupname = group.getGroupname();
                try {
                    group.save();
                    final Session jcrSession = UserSession.get().getJcrSession();
                    log.info("Group '{}' saved by {}", groupname, jcrSession.getUserID());
                    activateParentAndDisplayInfo(getString("group-saved", model));
                } catch (RepositoryException e) {
                    target.add(EditGroupPanel.this);
                    warn(getString("group-save-failed", model));
                    log.error("Unable to save group '{}' : ", groupname, e);
                }
            }
        });

        form.add(new AjaxButton("cancel-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                activateParent();
            }
        }.setDefaultFormProcessing(false));
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-edit-title", component).setModel(model);
    }

}
