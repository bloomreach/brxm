/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialogConfig;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.CKEditorNodePlugin;
import org.hippoecm.frontend.plugins.richtext.dialog.AbstractRichTextEditorDialog;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.links.RichTextEditorLinkService;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;
import org.hippoecm.frontend.plugins.richtext.processor.WicketNodeFactory;
import org.onehippo.cms7.services.processor.html.model.Model;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactory;
import org.onehippo.cms7.services.processor.richtext.link.RichTextLinkFactoryImpl;

/**
 * Manages the picker dialog for internal links in rich text fields. The behavior can be called by the frontend to
 * open the link picker. When done the method 'ChannelEditor#onLinkPicked' is called.
 */
class LinkPickerManager extends PickerManager {

    private final LinkPickerBehavior behavior;

    LinkPickerManager(final IPluginContext context, final String channelEditorId) {
        super(CKEditorNodePlugin.DEFAULT_LINK_PICKER_CONFIG);

        final Model<Node> fieldNodeModel = this.getFieldNodeModel();
        final RichTextLinkFactory linkFactory = new RichTextLinkFactoryImpl(fieldNodeModel, WicketNodeFactory.INSTANCE);
        final RichTextEditorLinkService linkService = new RichTextEditorLinkService(linkFactory);
        behavior = new StatelessLinkPickerBehavior(context, getPickerConfig(), linkService);
        behavior.setCloseAction(new PickedAction<>(channelEditorId, "onLinkPicked", fieldNodeModel));
    }

    LinkPickerBehavior getBehavior() {
        return behavior;
    }

    private class StatelessLinkPickerBehavior extends LinkPickerBehavior {

        StatelessLinkPickerBehavior(final IPluginContext context,
                                    final IPluginConfig dialogConfig,
                                    final RichTextEditorLinkService linkService) {
            super(context, dialogConfig, linkService);
        }

        @Override
        protected AbstractRichTextEditorDialog<RichTextEditorDocumentLink> createDialog() {
            initPicker(getParameters());

            final JavaPluginConfig pickerConfig = getPickerConfig();
            final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(pickerConfig, LinkPickerManager.this::getFieldNode);
            pickerConfig.putAll(dialogConfig);

            return super.createDialog();
        }
    }
}
