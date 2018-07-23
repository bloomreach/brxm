/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channeleditor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.CmsHeaderItem;
import org.hippoecm.frontend.extjs.ExtUtilsHeaderItem;
import org.hippoecm.frontend.extjs.ExtWidgetRegistryHeaderItem;
import org.wicketstuff.js.ext.util.ExtResourcesHeaderItem;

public class ChannelEditorApiHeaderItem extends HeaderItem {

    // All individual javascript files used in the template composer API. The array order determines the include order,
    // which matters. All files listed below should also be present in the aggregation section in frontend-api/pom.xml.
    private static final String[] FILES = {
            "IFrameWindow.js",
            "ComponentPropertiesEditor.js",
            "PlainComponentPropertiesEditor.js",
            "ComponentVariantAdder.js",
            "PlainComponentVariantAdder.js"
    };
    private static final JavaScriptResourceReference[] RESOURCES;

    private static final JavaScriptResourceReference CONCATENATED_RESOURCES =
            new JavaScriptResourceReference(ChannelEditorApiHeaderItem.class, "channel-editor-api-bundle.js");

    static {
        RESOURCES = new JavaScriptResourceReference[FILES.length];
        for (int i = 0; i < FILES.length; i++) {
            RESOURCES[i] = new JavaScriptResourceReference(ChannelEditorApiHeaderItem.class, FILES[i]);
        }
    }

    private static final ChannelEditorApiHeaderItem INSTANCE = new ChannelEditorApiHeaderItem();

    public static ChannelEditorApiHeaderItem get() {
        return INSTANCE;
    }

    private ChannelEditorApiHeaderItem() {}

    @Override
    public List<HeaderItem> getDependencies() {
        return Arrays.asList(ExtResourcesHeaderItem.get(), CmsHeaderItem.get(), ExtWidgetRegistryHeaderItem.get(), ExtUtilsHeaderItem.get());
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singletonList("channel-editor-api-header-item");
    }

    @Override
    public void render(final Response response) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            for (JavaScriptResourceReference resourceReference : RESOURCES) {
                JavaScriptHeaderItem.forReference(resourceReference).render(response);
            }
        } else {
            JavaScriptHeaderItem.forReference(CONCATENATED_RESOURCES).render(response);
        }
    }

}
