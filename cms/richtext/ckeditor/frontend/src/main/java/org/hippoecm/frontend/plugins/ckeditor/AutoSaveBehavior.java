/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor;

import java.util.HashMap;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.StringValue;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.dialog.AbstractAjaxDialogBehavior;
import org.hippoecm.frontend.plugins.richtext.dialog.AbstractRichTextEditorDialog;
import org.hippoecm.frontend.plugins.richtext.dialog.links.DocumentBrowserDialog;
import org.hippoecm.frontend.plugins.richtext.dialog.links.RichTextEditorLinkService;
import org.hippoecm.frontend.plugins.richtext.model.RichTextEditorInternalLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSaveBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AutoSaveBehavior.class);

    private static final String POST_PARAM_DATA = "data";

    private final IModel<String> editorModel;

    public AutoSaveBehavior(IModel<String> editorModel) {
        this.editorModel = editorModel;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        final Request request = RequestCycle.get().getRequest();
        final IRequestParameters requestParameters = request.getPostParameters();
        final StringValue data = requestParameters.getParameterValue(POST_PARAM_DATA);

        if (data.isNull()) {
            log.info("Cannot auto-save CKEditor contents because the request parameter '{}' is missing", POST_PARAM_DATA);
        } else {
            log.debug("Auto-saving CKEditor contents: '{}'", data);
            editorModel.setObject(data.toString());
        }
    }

}
