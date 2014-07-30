/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditUserPanel extends AdminBreadCrumbPanel {

    private static final long serialVersionUID = 1L;

    private static final String QUERY_SECURITY_PROVIDERS = "//element(*, hipposys:securityprovider)";

    private static final Logger log = LoggerFactory.getLogger(EditUserPanel.class);

    private final Form form;
    private final IModel model;

    public EditUserPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        
        this.model = model;

        // add form with markup id setter so it can be updated via ajax
        form = new Form("form", new CompoundPropertyModel(model));
        form.setOutputMarkupId(true);
        add(form);

        FormComponent fc;

        fc = new TextField("firstName");
        form.add(fc);

        fc = new TextField("lastName");
        form.add(fc);

        fc = new TextField("email");
        fc.add(EmailAddressValidator.getInstance());
        fc.setRequired(false);
        form.add(fc);

        fc = new CheckBox("active");
        form.add(fc);

        fc = new DropDownChoice<String>("provider", getAvailableSecurityProviderNames());
        form.add(fc);

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("save-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                User user = (User) model.getObject();
                String username = user.getUsername();
                try {
                    user.save();
                    log.info("User '" + username + "' saved by "
                            + UserSession.get().getJcrSession().getUserID());
                    Session.get().info(getString("user-saved", model));
                    // one up
                    List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                    breadCrumbModel.setActive(l.get(l.size() -2));
                } catch (RepositoryException e) {
                    Session.get().warn(getString("user-save-failed", model));
                    log.error("Unable to save user '" + username + "' : ", e);
                }
            }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.add(EditUserPanel.this);
            }
        });

        // add a cancel/back button
        form.add(new AjaxButton("cancel-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // one up
                List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(l.get(l.size() -2));
            }
        }.setDefaultFormProcessing(false));
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("user-edit-title", component, model);
    }

    private List<String> getAvailableSecurityProviderNames() {
        List<String> providers = new ArrayList<String>();

        try {
            UserSession session = UserSession.get();
            Query query = session.getQueryManager().createQuery(QUERY_SECURITY_PROVIDERS, Query.XPATH);
            QueryResult result = query.execute();
            Node node = null;

            for (NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext(); ) {
                node = nodeIt.nextNode();

                if (node != null) {
                    providers.add(node.getName());
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to query all the available security provider names.", e);
        }

        if (providers.size() > 1) {
            Collections.sort(providers);
        }

        return providers;
    }
}
