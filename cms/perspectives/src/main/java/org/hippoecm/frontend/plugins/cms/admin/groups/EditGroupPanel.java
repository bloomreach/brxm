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

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
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
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EditGroupPanel.class);

    private final Form form;
    private final IModel model;

    public EditGroupPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.model = model;

        // add form with markup id setter so it can be updated via ajax
        form = new Form("form", new CompoundPropertyModel(model));
        form.setOutputMarkupId(true);
        add(form);

        form.add(new TextField("description"));

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("save-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                Group group = (Group) model.getObject();
                String groupname = group.getGroupname();
                try {
                    group.save();
                    log.info("Group '" + groupname + "' saved by " + UserSession.get().getJcrSession().getUserID());
                    final String infoMsg = getString("group-saved", model);
                    final IBreadCrumbParticipant parentBreadCrumb = activateParent();
                    parentBreadCrumb.getComponent().info(infoMsg);
                } catch (RepositoryException e) {
                    target.add(EditGroupPanel.this);
                    warn(getString("group-save-failed", model));
                    log.error("Unable to save group '" + groupname + "' : ", e);
                }
            }
        });

        form.add(new AjaxButton("cancel-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                activateParent();
            }
        }.setDefaultFormProcessing(false));
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-edit-title", component, model);
    }

}
