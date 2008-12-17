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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugins.cms.admin.AdminPerspective;

public class UsersPanel extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private final AdminPerspective parentPerspective;

    Fragment listUsersFragment = new Fragment("panel", "list-users-fragment", this);
    Fragment viewUserFragment = new Fragment("panel", "view-user-fragment", this);
    Fragment addUserFragment = new Fragment("panel", "add-user-fragment", this);
    Fragment editUserFragment = new Fragment("panel", "edit-user-fragment", this);
    Fragment setPasswordFragment = new Fragment("panel", "set-password-fragment", this);

    public UsersPanel(final String id, final AdminPerspective parent) {
        super(id);
        this.parentPerspective = parent;
        setOutputMarkupId(true);

        add(new AjaxFallbackLink("menu-main") {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                parent.showConfigPanel();
            }
        });

        add(new AjaxFallbackLink("menu-users") {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                showList();
            }
        });

        // add feedback panel to show errors
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        
        listUsersFragment.add(new ListUsersPanel("list-users-panel", this));
        add(listUsersFragment);
    }

    void refresh() {
        parentPerspective.refresh();
    }

    void showList() {
        UsersPanel.this.replace(listUsersFragment);
        refresh();
    }

    void showView(AjaxRequestTarget target, IModel model) {
        viewUserFragment.addOrReplace(new ViewUserPanel("view-user-panel", model, this));
        UsersPanel.this.replace(viewUserFragment);
        refresh();
    }
    
    void showAddForm() {
        addUserFragment.addOrReplace(new CreateUserPanel("add-user-panel", this));
        UsersPanel.this.replace(addUserFragment);
        refresh();
    }

    void showEditForm(AjaxRequestTarget target, IModel model) {
        editUserFragment.addOrReplace(new EditUserPanel("edit-user-panel", model, this));
        UsersPanel.this.replace(editUserFragment);
        refresh();
    }

    void showPasswordForm(AjaxRequestTarget target, IModel model) {
        setPasswordFragment.addOrReplace(new SetPasswordPanel("set-password-panel", model, this));
        UsersPanel.this.replace(setPasswordFragment);
        refresh();
    }

    void showDialog(IDialogService.Dialog dialog) {
        parentPerspective.showDialog(dialog);
    }
}