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

import org.hippoecm.frontend.dialog.DialogBehavior;
import org.hippoecm.frontend.dialog.ConfigProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.ckeditor.CKEditorNodePlugin;
import org.hippoecm.frontend.plugins.richtext.dialog.links.LinkPickerBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.links.RichTextEditorLinkService;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketNodeFactory;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactoryImpl;

/**
 * Manages the picker dialog for internal links in rich text fields. The behavior can be called by the frontend to open
 * the link picker. When done the method 'ChannelEditor#onLinkPicked' is called.
 */
public class LinkPickerManager extends RichTextPickerManager<RichTextEditorDocumentLink> {

    public LinkPickerManager(final IPluginContext context, final String channelEditorId) {
        super(context, CKEditorNodePlugin.DEFAULT_LINK_PICKER_CONFIG, channelEditorId);
    }

    @Override
    protected DialogBehavior<RichTextEditorDocumentLink> createBehavior(final IPluginContext context,
                                                                        final ConfigProvider configProvider) {
        final Model<Node> fieldNodeModel = getFieldNodeModel();
        final RichTextLinkFactory linkFactory = new RichTextLinkFactoryImpl(fieldNodeModel, WicketNodeFactory.INSTANCE);
        final RichTextEditorLinkService linkService = new RichTextEditorLinkService(linkFactory);
        return new LinkPickerBehavior(context, configProvider, linkService);
    }
}
