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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.extjs.ExtWidgetRegistryHeaderItem;

public class ChannelEditorHeaderItem extends HeaderItem {

    // This list should be identical to the one defined in the pom.xml of this module.
    // The order of items matters.
    private static final String[] RESOURCES = {
            "plugins/colorfield/colorfield.js",
            "plugins/floatingwindow/FloatingWindow.js",
            "plugins/vtabs/VerticalTabPanel.js",
            "ComponentVariants.js",
            "ComponentPropertiesForm.js",
            "ComponentPropertiesPanel.js",
            "ComponentPropertiesWindow.js",
            "ChannelEditor.js"
    };

    private static final String CONCATENATED_RESOURCES = "channel-editor-bundle.js";

    private static final List<JavaScriptResourceReference> DEVELOPMENT_REFERENCES;
    private static final List<JavaScriptResourceReference> DEPLOYMENT_REFERENCES;

    static {
        final List<JavaScriptResourceReference> developmentRefs = new ArrayList<>();
        developmentRefs.addAll(javaScriptResources(ChannelEditorHeaderItem.class, RESOURCES));
        DEVELOPMENT_REFERENCES = Collections.unmodifiableList(developmentRefs);

        final JavaScriptResourceReference concatenatedRef = new JavaScriptResourceReference(ChannelEditorHeaderItem.class, CONCATENATED_RESOURCES);
        DEPLOYMENT_REFERENCES = Collections.singletonList(concatenatedRef);
    }

    private static List<JavaScriptResourceReference> javaScriptResources(Class scope, String... sources) {
        final List<JavaScriptResourceReference> resources = new ArrayList<>();
        for (String source : sources) {
            resources.add(new JavaScriptResourceReference(scope, source));
        }
        return resources;
    }

    private static final ChannelEditorHeaderItem INSTANCE = new ChannelEditorHeaderItem();

    public static ChannelEditorHeaderItem get() {
        return INSTANCE;
    }

    private ChannelEditorHeaderItem() {}

    @Override
    public List<HeaderItem> getDependencies() {
        return Collections.singletonList(ExtWidgetRegistryHeaderItem.get());
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singletonList("channel-editor-header-item");
    }

    @Override
    public void render(final Response response) {
        for (JavaScriptResourceReference reference : getRenderedReferences()) {
            JavaScriptHeaderItem.forReference(reference).render(response);
        }
    }

    private static List<JavaScriptResourceReference> getRenderedReferences() {
        return Application.get().getDebugSettings().isAjaxDebugModeEnabled() ? DEVELOPMENT_REFERENCES : DEPLOYMENT_REFERENCES;
    }
}
