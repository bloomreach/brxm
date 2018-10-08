/*
 * Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.onehippo.cms7.channelmanager;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.CmsHeaderItem;
import org.onehippo.cms7.channelmanager.channeleditor.ChannelEditorApiHeaderItem;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelIconPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelOverview;
import org.onehippo.cms7.channelmanager.common.CommonBundle;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class ChannelManagerHeaderItem extends HeaderItem {

    private static final long serialVersionUID = 1L;

    private static final JavaScriptResourceReference BUNDLE = new JavaScriptResourceReference(ChannelManagerHeaderItem.class,
            "channel-manager-bundle.js");

    private static final String ROOT_PANEL = "RootPanel.js";
    private static final String BLUEPRINT_LIST_PANEL = "BlueprintListPanel.js";
    private static final String CHANNEL_FORM_PANEL = "ChannelFormPanel.js";
    private static final String BREADCRUMB_TOOLBAR = "BreadcrumbToolbar.js";
    private static final String PINGER = "Pinger.js";

    private static final JavaScriptResourceReference[] JAVASCRIPT_RESOURCE_REFERENCES;

    static {
        List<JavaScriptResourceReference> references = new ArrayList<JavaScriptResourceReference>();
        references.add(new JavaScriptResourceReference(CommonBundle.class, CommonBundle.MARK_REQUIRED_FIELDS));
        references.add(new JavaScriptResourceReference(ExtStoreFuture.class, ExtStoreFuture.EXT_STORE_FUTURE));
        references.add(new JavaScriptResourceReference(ChannelManagerHeaderItem.class, BREADCRUMB_TOOLBAR));
        references.add(new JavaScriptResourceReference(ChannelManagerHeaderItem.class, PINGER));
        references.add(new JavaScriptResourceReference(ChannelManagerHeaderItem.class, ROOT_PANEL));
        references.add(new JavaScriptResourceReference(ChannelManagerHeaderItem.class, BLUEPRINT_LIST_PANEL));
        references.add(new JavaScriptResourceReference(ChannelManagerHeaderItem.class, CHANNEL_FORM_PANEL));
        references.add(new JavaScriptResourceReference(ChannelOverview.class, ChannelOverview.CHANNEL_OVERVIEW_PANEL_JS));
        references.add(new JavaScriptResourceReference(ChannelGridPanel.class, ChannelGridPanel.CHANNEL_GRID_PANEL_JS));
        references.add(new JavaScriptResourceReference(ChannelIconPanel.class, ChannelIconPanel.CHANNEL_ICON_PANEL_JS));
        JAVASCRIPT_RESOURCE_REFERENCES = references.toArray(new JavaScriptResourceReference[references.size()]);
    }

    private static final ChannelManagerHeaderItem INSTANCE = new ChannelManagerHeaderItem();

    public static ChannelManagerHeaderItem get() {
        return INSTANCE;
    }

    private ChannelManagerHeaderItem() {}

    @Override
    public List<HeaderItem> getDependencies() {
        return Arrays.asList(CmsHeaderItem.get(), ExtResourcesHeaderItem.get(), ChannelEditorApiHeaderItem.get());
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("channel-manager-header-item");
    }

    @Override
    public void render(final Response response) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            for (JavaScriptResourceReference resourceReference : JAVASCRIPT_RESOURCE_REFERENCES) {
                JavaScriptHeaderItem.forReference(resourceReference).render(response);
            }
        } else {
            JavaScriptHeaderItem.forReference(BUNDLE).render(response);
        }
    }

}
