/*
 * Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.templatecomposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.extjs.ExtWidgetRegistryHeaderItem;
import org.onehippo.cms7.channelmanager.common.CommonBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.pageeditor.PageEditorBundle;
import org.onehippo.cms7.channelmanager.templatecomposer.plugins.PluginsBundle;

public class TemplateComposerHeaderItem extends HeaderItem {

    private static final List<JavaScriptResourceReference> DEVELOPMENT_REFERENCES;
    private static final List<JavaScriptResourceReference> DEPLOYMENT_REFERENCES;

    static {
        List<JavaScriptResourceReference> developmentRefs = new ArrayList<>();
        developmentRefs.addAll(javaScriptResources(PluginsBundle.class, PluginsBundle.FILES));
        developmentRefs.add(new JavaScriptResourceReference(TemplateComposerGlobalBundle.class, TemplateComposerGlobalBundle.GLOBALS));
        developmentRefs.add(new JavaScriptResourceReference(CommonBundle.class, CommonBundle.MARK_REQUIRED_FIELDS));
        developmentRefs.addAll(javaScriptResources(PageEditorBundle.class, PageEditorBundle.FILES));
        DEVELOPMENT_REFERENCES = Collections.unmodifiableList(developmentRefs);

        List<JavaScriptResourceReference> deploymentRefs = new ArrayList<>();
        deploymentRefs.add(new JavaScriptResourceReference(PluginsBundle.class, PluginsBundle.ALL));
        deploymentRefs.add(new JavaScriptResourceReference(PageEditorBundle.class, PageEditorBundle.ALL));
        DEPLOYMENT_REFERENCES = Collections.unmodifiableList(deploymentRefs);
    }

    private static List<JavaScriptResourceReference> javaScriptResources(Class scope, String... sources) {
        final List<JavaScriptResourceReference> resources = new ArrayList<>();
        for (String source : sources) {
            resources.add(new JavaScriptResourceReference(scope, source));
        }
        return resources;
    }

    private static final TemplateComposerHeaderItem INSTANCE = new TemplateComposerHeaderItem();

    public static TemplateComposerHeaderItem get() {
        return INSTANCE;
    }

    private TemplateComposerHeaderItem() {}

    @Override
    public Iterable<? extends HeaderItem> getDependencies() {
        return Arrays.asList(ExtWidgetRegistryHeaderItem.get());
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Arrays.asList("template-composer-header-item");
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
