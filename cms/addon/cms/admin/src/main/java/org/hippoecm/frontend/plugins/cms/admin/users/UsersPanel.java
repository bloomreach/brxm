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
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugins.cms.admin.AdminPerspective;

public class UsersPanel extends BreadCrumbPanel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private final Fragment detailsFragment = new Fragment("fragment-panel", "details-fragment", this);
    private final static String panelId = "details-panel";
    
    public UsersPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        // add feedback panel to show errors
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        
        //add(new ListUsersPanel(panelId, this));
        //add(detailsFragment);
    }

    public String getTitle() {
        return getString("admin-users-title");
    }
    
//    void refresh() {
//        parentPerspective.refresh();
//    }
//
//    void showList() {
//        detailsFragment.addOrReplace(new ListUsersPanel(panelId, this));
//        UsersPanel.this.replace(detailsFragment);
//        refresh();
//    }
//
//    void showView(AjaxRequestTarget target, IModel model) {
//        detailsFragment.addOrReplace(new ViewUserPanel(panelId, model, this));
//        UsersPanel.this.replace(detailsFragment);
//        refresh();
//    }
//    
//    void showAddForm() {
//        detailsFragment.addOrReplace(new CreateUserPanel(panelId, this));
//        UsersPanel.this.replace(detailsFragment);
//        refresh();
//    }
//
//    void showEditForm(AjaxRequestTarget target, IModel model) {
//        detailsFragment.addOrReplace(new EditUserPanel(panelId, model, this));
//        UsersPanel.this.replace(detailsFragment);
//        refresh();
//    }
//
//    void showPasswordForm(AjaxRequestTarget target, IModel model) {
//        detailsFragment.addOrReplace(new SetPasswordPanel(panelId, model, this));
//        UsersPanel.this.replace(detailsFragment);
//        refresh();
//    }
//
//    void showSetMembershipsForm(AjaxRequestTarget target, IModel model) {
//        detailsFragment.addOrReplace(new SetMembershipsPanel(panelId, model, this));
//        UsersPanel.this.replace(detailsFragment);
//        refresh();
//    }
//
//    void showDialog(IDialogService.Dialog dialog) {
//        parentPerspective.showDialog(dialog);
//    }
}