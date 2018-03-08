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
package org.hippoecm.frontend.plugins.richtext.dialog.links;

import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogManager;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.htmlprocessor.WicketNodeFactory;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorDocumentLink;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorInternalLink;
import org.onehippo.cms7.services.htmlprocessor.model.Model;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactory;
import org.onehippo.cms7.services.htmlprocessor.richtext.link.RichTextLinkFactoryImpl;

public class RichTextLinkPicker extends DialogManager<RichTextEditorDocumentLink> {

    private final RichTextEditorLinkService linkService;

    public RichTextLinkPicker(final IPluginContext context, final IPluginConfig config, final Model<Node> nodeModel) {
        super(context, config);

        final RichTextLinkFactory linkFactory = new RichTextLinkFactoryImpl(nodeModel, WicketNodeFactory.INSTANCE);
        linkService = new RichTextEditorLinkService(linkFactory);
    }

    @Override
    protected Dialog<RichTextEditorDocumentLink> createDialog(final IPluginContext context, final IPluginConfig config, final Map<String, String> parameters) {
        final RichTextEditorInternalLink internalLink = linkService.create(parameters);
        final IModel<RichTextEditorDocumentLink> model = org.apache.wicket.model.Model.of(internalLink);
        return new DocumentBrowserDialog<>(context, config, model);
    }

    @Override
    public void detach() {
        linkService.detach();
        super.detach();
    }
}
