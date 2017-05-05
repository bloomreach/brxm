/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.IEditorContext;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.HippoStdNodeType;

public class TestEditorContext implements IEditorContext {

    public static final String EDITORS = "editors";
    public static final String PREVIEWS = "previews";
    public static final String FILTERS = "filters";

    public static final String[] CONTENT_1 = {
            "/test", "nt:unstructured",
            "/test/content", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable"
    };
    public static final String[] CONTENT_2 = {
            "/test/facetsearch", "hippo:facetsearch",
                "hippo:docbase", "/test/content",
                "hippo:queryname", "state",
                "hippo:facets", HippoStdNodeType.HIPPOSTD_STATE,
            "/test/mirror", "hippo:mirror",
                "hippo:docbase", "/test/content",
            "/config/test-app/cms-editor", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:services", "wicket.id",
            "/config/test-app/cms-editor/plugin", "frontend:plugin",
                "plugin.class", Editor.class.getName(),
            "/config/test-app/cms-preview", "frontend:plugincluster",
                "frontend:references", "wicket.model",
                "frontend:references", "model.compareTo",
                "frontend:references", "editor.id",
                "frontend:services", "wicket.id",
            "/config/test-app/cms-preview/plugin", "frontend:plugin",
                "plugin.class", Preview.class.getName(),
            "/config/test-app/cms-preview/filter", "frontend:plugin",
                "plugin.class", CloseFilter.class.getName(),
    };

    public static final String[] CMS_TEST_DOCUMENT = {
            "/${name}", "hippo:handle",
                "jcr:mixinTypes", "mix:referenceable",
            "/${name}/${name}", "cmstest:document",
                "jcr:mixinTypes", "mix:versionable",
                "jcr:mixinTypes", "hippostdpubwf:document",
                "jcr:mixinTypes", "hippostd:publishableSummary",
                HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.UNPUBLISHED,
                HippoStdNodeType.HIPPOSTD_STATESUMMARY, HippoStdNodeType.NEW,
                "hippostdpubwf:createdBy", "admin",
                "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00",
                "hippostdpubwf:lastModifiedBy", "admin",
                "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00"
    };

    public IEditorManager getEditorManager() {
        return null;
    }

    public void onClose() {
    }

    public void onFocus() {
    }

    public static class Editor extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public Editor(final IPluginContext context, final IPluginConfig config) {
            super(context, config);

            context.registerService(this, EDITORS);
        }
    }

    public static class Preview extends RenderPlugin {
        private static final long serialVersionUID = 1L;

        public Preview(final IPluginContext context, final IPluginConfig config) {
            super(context, config);

            context.registerService(this, PREVIEWS);
        }

        public IModel getCompareToModel() {
            return getPluginContext().getService(getPluginConfig().getString("model.compareTo"), IModelReference.class)
                    .getModel();
        }
    }

    public static class CloseFilter extends Plugin implements IEditorFilter {
        private static final long serialVersionUID = 1L;

        boolean closed;

        public CloseFilter(final IPluginContext context, final IPluginConfig config) {
            super(context, config);

            final IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);
            context.registerService(this, context.getReference(editor).getServiceId());

            context.registerService(this, FILTERS);
        }

        public Object preClose() {
            closed = true;
            return new Object();
        }

        public void postClose(final Object object) {
        }
    }

}
