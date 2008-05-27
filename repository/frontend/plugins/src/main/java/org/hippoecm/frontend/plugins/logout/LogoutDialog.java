/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.logout;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.sa.dialog.AbstractDialog;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.service.ITitleDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutDialog extends AbstractDialog implements ITitleDecorator {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LogoutDialog.class);

    boolean logout;

    public LogoutDialog(IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);
        setOutputMarkupId(true);

        Label messageLabel = new Label("message", "There are unsaved changes. Do you want to logout?");
        add(messageLabel);
    }

    public String getTitle() {
        return "Logout";
    }

    @Override
    protected void ok() throws Exception {
        logout = true;
    }

    @Override
    protected void cancel() {
        logout = false;
    }
}
