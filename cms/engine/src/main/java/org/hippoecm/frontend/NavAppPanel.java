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

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.filter.FilteredHeaderItem;
import org.apache.wicket.markup.head.filter.HeaderResponseContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.service.NavAppSettings;

public class NavAppPanel extends Panel {

    static final String NAVAPP_HEADER_ITEM = "navapp-header-item";

    private final HeaderItem navAppHeaderItem;

    public NavAppPanel(String id, NavAppSettings navAppSettings) {
        super(id);
        this.navAppHeaderItem = new FilteredHeaderItem(new NavAppHeaderItem(navAppSettings), NAVAPP_HEADER_ITEM);
        add(new HeaderResponseContainer(NAVAPP_HEADER_ITEM, NAVAPP_HEADER_ITEM));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(navAppHeaderItem);
    }
}
