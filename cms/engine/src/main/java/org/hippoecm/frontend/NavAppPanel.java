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

import java.util.function.Function;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.filter.FilteredHeaderItem;
import org.apache.wicket.markup.head.filter.HeaderResponseContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.service.NavAppSettings;

public class NavAppPanel extends Panel {

    public static final String NAVAPP_JAVASCRIPT_HEADER_ITEM = "navapp-javascript-header-item";

    private final HeaderItem navAppJavascriptHeaderItem;

    public NavAppPanel(String id, NavAppSettings navAppSettings) {
        super(id);


        final Function<String, ResourceReference> mapper = NavAppUtils.getMapper(navAppSettings.getAppSettings());
        final Function<String, JavaScriptHeaderItem> toJsHeaderItem = mapper.andThen(JavaScriptHeaderItem::forReference);
        final NavAppJavascriptHeaderItem javascriptHeaderItem = new NavAppJavascriptHeaderItem(navAppSettings, toJsHeaderItem);

        // Put it in a filter header item so that it will render in the body instead of the head
        // The NavAppPanel.html must contain a wicket:container with the same id as this filtered header item.
        this.navAppJavascriptHeaderItem = new FilteredHeaderItem(javascriptHeaderItem, NAVAPP_JAVASCRIPT_HEADER_ITEM);
        add(new HeaderResponseContainer(NAVAPP_JAVASCRIPT_HEADER_ITEM, NAVAPP_JAVASCRIPT_HEADER_ITEM));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(navAppJavascriptHeaderItem);
    }
}
