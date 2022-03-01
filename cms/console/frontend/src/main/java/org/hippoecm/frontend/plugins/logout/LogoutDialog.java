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
package org.hippoecm.frontend.plugins.logout;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.service.ILogoutService;

public class LogoutDialog extends Dialog<Void> {

    private final ILogoutService logoutService;
    private boolean logout;

    public LogoutDialog(final ILogoutService logoutService) {
        setOutputMarkupId(true);
        setTitle(new StringResourceModel("title", this));
        setSize(DialogConstants.SMALL);

        this.logoutService = logoutService;

        Label messageLabel = new Label("message", new StringResourceModel("message", this));
        add(messageLabel);
    }

    @Override
    protected void onOk() {
        logout = true;
    }

    @Override
    protected void onCancel() {
        logout = false;
    }

    @Override
    public void onClose() {
        if (logout) {
            logoutService.logout();
        }
        super.onClose();
    }
}
