/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.InlineFrame;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.session.PluginUserSession;

public class NavAppPage extends Home {

    public NavAppPage() {
        this(PluginUserSession.get().getApplicationFactory());
    }

    private final InlineFrame cmsInlineFrame;

    public NavAppPage(final IApplicationFactory ignored) {
        super();
        cmsInlineFrame = new InlineFrame("cms", PluginPage.class);
        add(cmsInlineFrame);
        cmsInlineFrame.setVisible(true);
        add(new Label("pageTitle", getString("pageTitle")));
    }

    @Override
    public void refresh() {

    }

    @Override
    public void processEvents() {

    }

    @Override
    public void render(final PluginRequestTarget target) {
    }

    @Override
    public void showContextMenu(final IContextMenu active) {

    }

    @Override
    public void collapseAllContextMenus() {

    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(NavAppHeaderItem.get());
    }

}
