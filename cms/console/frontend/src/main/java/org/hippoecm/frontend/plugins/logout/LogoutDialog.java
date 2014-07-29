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
package org.hippoecm.frontend.plugins.logout;

import javax.jcr.RepositoryException;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutDialog extends AbstractDialog {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LogoutDialog.class);

    boolean logout;

    public LogoutDialog() {
        setOutputMarkupId(true);

        Label messageLabel = new Label("message", new StringResourceModel("message", this, null));
        add(messageLabel);
    }

    public IModel<String> getTitle() {
        return new StringResourceModel("title", this, null);
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
            UserSession userSession = getSession();
            try {
                userSession.getJcrSession().refresh(false);
            } catch (RepositoryException e) {
                log.error("Unable to remove the pending changes upon logout.");
            }
            userSession.logout();
            if (WebApplication.exists()) {
                throw new RestartResponseException(WebApplication.get().getHomePage());
            }
        }
        super.onClose();
    }
    
    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }
}
