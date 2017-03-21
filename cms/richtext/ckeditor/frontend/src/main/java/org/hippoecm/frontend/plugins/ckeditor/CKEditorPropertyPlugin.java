/*
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.LineEndingsModel;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextDiffPanel;
import org.hippoecm.frontend.plugins.richtext.view.RichTextPreviewPanel;
import org.hippoecm.frontend.plugins.standards.diff.DefaultHtmlDiffService;
import org.hippoecm.frontend.plugins.standards.diff.DiffService;
import org.onehippo.ckeditor.CKEditorConfig;

/**
 * Property field plugin for editing HTML in a String property using CKEditor. Internal links and images are
 * not supported.
 */
public class CKEditorPropertyPlugin extends AbstractCKEditorPlugin<String> {

    public CKEditorPropertyPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config, CKEditorConfig.DEFAULT_FORMATTED_TEXT_CONFIG);
    }

    @Override
    protected Panel createViewPanel(final String id) {
        return new RichTextPreviewPanel(id, getHtmlModel());
    }

    @Override
    protected IModel<String> createEditModel() {
        return new LineEndingsModel(new RichTextModel(getHtmlModel(), getHtmlCleaner()));
    }

    @Override
    protected IModel<String> getHtmlModel() {
        return (IModel<String>) getDefaultModel();
    }

    @Override
    protected Panel createComparePanel(final String id, final IModel<String> baseModel, final IModel<String> currentModel) {
        final JcrPropertyValueModel<String> basePropertyModel = (JcrPropertyValueModel<String>) baseModel;
        final JcrPropertyValueModel<String> currentPropertyModel = (JcrPropertyValueModel<String>) currentModel;
        return new RichTextDiffPanel(id, basePropertyModel, currentPropertyModel, getDiffService());
    }

    private DiffService getDiffService() {
        final String serviceId = getPluginConfig().getString(DiffService.SERVICE_ID);
        return getPluginContext().getService(serviceId, DefaultHtmlDiffService.class);
    }

}
