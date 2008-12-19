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
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;

public class UsersPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private final Fragment detailsFragment = new Fragment("fragment-panel", "details-fragment", this);
    private final static String panelId = "details-panel";
    
    public UsersPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-users-title", component, null);
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