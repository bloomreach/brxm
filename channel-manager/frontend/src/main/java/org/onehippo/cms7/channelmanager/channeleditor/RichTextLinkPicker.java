/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channeleditor;

import java.util.Map;

import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.CKEditorNodePlugin;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPickerManager;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;

public class RichTextLinkPicker extends RichTextPicker<RichTextEditorDocumentLink> {

    protected RichTextLinkPicker(final IPluginContext context, final String channelEditorId) {
        this(context, CKEditorNodePlugin.DEFAULT_LINK_PICKER_CONFIG, channelEditorId);
    }

    protected RichTextLinkPicker(final IPluginContext context, final IPluginConfig config, final String channelEditorId) {
        super(context, config, channelEditorId);
    }

    @Override
    protected DialogManager<RichTextEditorDocumentLink> createDialogManager(final IPluginContext context, final IPluginConfig config) {
        return new LinkPickerManager(context, config, getFieldNodeModel()) {
            @Override
            protected void beforeShowDialog(final Map<String, String> parameters) {
                setNodeId(parameters);
            }
        };
    }
}
