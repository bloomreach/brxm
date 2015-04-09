/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.root;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.logout.LogoutLink;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;

public class UserMenu extends Panel {

    public static final JavaScriptResourceReference SCRIPT_REFERENCE =
            new JavaScriptResourceReference(UserMenu.class, "UserMenu.js");

    public UserMenu(final String id, final User user) {
        super(id);

        setRenderBodyOnly(true);

        add(HippoIcon.fromSprite("icon", Icon.USER_CIRCLE, IconSize.XL));

        final String userName = user.getUsername();
        final String displayName = user.getDisplayName();
        final String email = user.getEmail();

        final Label userNameLabel = new Label("userName", userName);
        userNameLabel.add(TitleAttribute.set(userName));
        add(userNameLabel);

        final Label displayNameLabel = new Label("displayName", displayName);
        displayNameLabel.add(TitleAttribute.set(displayName));
        add(displayNameLabel);

        final Label emailLabel = new Label("email", email);
        emailLabel.add(TitleAttribute.set(email));
        add(emailLabel.setVisible(StringUtils.isNotEmpty(email)));

        add(new LogoutLink("logout"));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(JavaScriptReferenceHeaderItem.forReference(SCRIPT_REFERENCE));

        final String script = String.format("if (Hippo && Hippo.UserMenu) { Hippo.UserMenu.render('%s', '%s'); }",
                getLinkSelector(), getMenuSelector());
        response.render(OnDomReadyHeaderItem.forScript(script));

        super.renderHead(response);
    }

    protected String getLinkSelector() {
        return ".hippo-user-menu";
    }

    protected String getMenuSelector() {
        return ".hippo-user-popup-menu";
    }
}
