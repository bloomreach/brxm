/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.dialog.model.InternalCKEditorLink;
import org.hippoecm.frontend.plugins.richtext.AbstractAjaxDialogBehavior;

public class DocumentPickerBehavior extends AbstractAjaxDialogBehavior {

    private static final long serialVersionUID = 1L;

    private final CKEditorLinkService linkService;
    private final String editorId;

    public DocumentPickerBehavior(final IPluginContext context,
                                  final IPluginConfig config,
                                  final CKEditorLinkService linkService,
                                  final String editorId) {
        super(context, config);
        this.linkService = linkService;
        this.editorId = editorId;
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        final IModel<InternalCKEditorLink> model = new Model<InternalCKEditorLink>(linkService.create(getParameters()));
        final DocumentBrowserDialog dialog = new DocumentBrowserDialog<InternalCKEditorLink>(
                getPluginContext(), getPluginConfig(), model, editorId);
        getDialogService().show(dialog);
    }

}
